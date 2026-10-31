import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { pathToFileURL } from 'node:url';

const VALID_SEVERITIES = new Set([
  'CRITICAL',
  'HIGH',
  'MEDIUM',
  'LOW',
  'INFORMATIONAL',
  'UNKNOWN'
]);

export function calculateSha256(bufferOrString) {
  const hash = createHash('sha256');
  hash.update(bufferOrString);
  return hash.digest('hex').toLowerCase();
}

export function mapSeverity(rawSeverity) {
  if (!rawSeverity || typeof rawSeverity !== 'string') {
    return 'UNKNOWN';
  }
  const normalized = rawSeverity.trim().toUpperCase();
  if (VALID_SEVERITIES.has(normalized)) {
    return normalized;
  }
  return 'UNKNOWN';
}

export function indexSbomComponents(sbom) {
  if (!sbom || typeof sbom !== 'object') {
    throw new Error('Invalid SBOM: expected a CycloneDX JSON object.');
  }

  const byBomRef = new Map();
  const byPurl = new Map();
  const byCoord = new Map();

  function walk(components) {
    if (!Array.isArray(components)) return;
    for (const comp of components) {
      if (!comp || typeof comp !== 'object') continue;

      const bomRef = comp['bom-ref'] || comp.bomRef;
      if (bomRef) {
        byBomRef.set(bomRef, comp);
      }

      if (comp.purl) {
        if (!byPurl.has(comp.purl)) {
          byPurl.set(comp.purl, []);
        }
        byPurl.get(comp.purl).push(comp);
      }

      const group = comp.group || '';
      const name = comp.name || '';
      const version = comp.version || '';
      if (name) {
        const coordKey = `${group}:${name}@${version}`.toLowerCase();
        if (!byCoord.has(coordKey)) {
          byCoord.set(coordKey, []);
        }
        byCoord.get(coordKey).push(comp);
      }

      if (Array.isArray(comp.components)) {
        walk(comp.components);
      }
    }
  }

  walk(sbom.components);

  return {
    byBomRef,
    byPurl,
    byCoord,
    totalComponents: byBomRef.size
  };
}

export function matchComponentBomRef(vuln, sbomIndex) {
  // 1. Direct BOMRef match from Trivy PkgIdentifier
  const directBomRef = vuln.PkgIdentifier?.BOMRef;
  if (directBomRef && sbomIndex.byBomRef.has(directBomRef)) {
    return directBomRef;
  }

  // 2. Direct PURL match
  const purl = vuln.PkgIdentifier?.PURL;
  if (purl) {
    if (sbomIndex.byBomRef.has(purl)) {
      return purl;
    }
    const purlMatches = sbomIndex.byPurl.get(purl) || [];
    if (purlMatches.length === 1) {
      return purlMatches[0]['bom-ref'];
    }
    if (purlMatches.length > 1) {
      throw new Error(
        `Ambiguous component match for PURL "${purl}": matched multiple bom-refs (${purlMatches.map(c => c['bom-ref']).join(', ')}).`
      );
    }
  }

  // 3. Fallback coordinate match by PkgName (group:name) + InstalledVersion
  const pkgName = vuln.PkgName || '';
  const version = vuln.InstalledVersion || '';
  if (pkgName) {
    let group = '';
    let name = pkgName;
    if (pkgName.includes(':')) {
      const parts = pkgName.split(':');
      group = parts[0];
      name = parts.slice(1).join(':');
    }
    const coordKey = `${group}:${name}@${version}`.toLowerCase();
    const coordMatches = sbomIndex.byCoord.get(coordKey) || [];
    if (coordMatches.length === 1) {
      return coordMatches[0]['bom-ref'];
    }
    if (coordMatches.length > 1) {
      throw new Error(
        `Ambiguous component match for coordinate "${coordKey}": matched multiple bom-refs (${coordMatches.map(c => c['bom-ref']).join(', ')}).`
      );
    }
  }

  // If we reach here, finding cannot be matched
  throw new Error(
    `Unmatched vulnerability finding "${vuln.VulnerabilityID || 'UNKNOWN'}" on package "${pkgName}:${version}". Finding cannot be mapped to any SBOM component.`
  );
}

export function convertTrivyToReleaseGuard(rawReport, sbom, options = {}) {
  if (!rawReport || typeof rawReport !== 'object') {
    throw new Error('Invalid raw scanner report: expected a JSON object.');
  }

  if (rawReport.SchemaVersion == null) {
    throw new Error('Invalid raw scanner report: missing SchemaVersion.');
  }

  const sbomSha256 = options.sbomSha256 || (options.sbomRawBytes ? calculateSha256(options.sbomRawBytes) : null);
  if (!sbomSha256 || !/^[0-9a-f]{64}$/i.test(sbomSha256)) {
    throw new Error('Invalid sbomSha256: 64-character lowercase hexadecimal hash required.');
  }

  const sbomIndex = indexSbomComponents(sbom);

  const scannerName = options.scannerName || 'Trivy';
  const scannerVersion = options.scannerVersion || '0.57.1';
  const scannedAt = options.scannedAt || new Date().toISOString();

  const results = Array.isArray(rawReport.Results) ? rawReport.Results : [];
  const findingsMap = new Map();

  for (const result of results) {
    const vulns = Array.isArray(result.Vulnerabilities) ? result.Vulnerabilities : [];
    for (const vuln of vulns) {
      const componentBomRef = matchComponentBomRef(vuln, sbomIndex);
      const vulnerabilityId = (vuln.VulnerabilityID || '').trim();
      if (!vulnerabilityId) {
        throw new Error('Finding missing VulnerabilityID.');
      }

      const source = (vuln.SeveritySource || vuln.DataSource?.ID || 'trivy').trim();
      const severity = mapSeverity(vuln.Severity);

      let description = (vuln.Description || vuln.Title || '').trim();
      if (description.length > 10000) {
        description = description.slice(0, 9997) + '...';
      }

      let fixedVersion = (vuln.FixedVersion || null);
      if (fixedVersion && fixedVersion.length > 255) {
        fixedVersion = fixedVersion.slice(0, 255);
      }

      // Persistence key: (componentBomRef, source, vulnerabilityId)
      const key = `${componentBomRef}|${source}|${vulnerabilityId}`;

      if (!findingsMap.has(key)) {
        findingsMap.set(key, {
          componentBomRef,
          vulnerabilityId,
          source,
          severity,
          description: description || null,
          fixedVersion: fixedVersion || null
        });
      } else {
        // If duplicate key exists, preserve fixedVersion if previously missing
        const existing = findingsMap.get(key);
        if (!existing.fixedVersion && fixedVersion) {
          existing.fixedVersion = fixedVersion;
        }
      }
    }
  }

  return {
    formatVersion: '1',
    sbomSha256: sbomSha256.toLowerCase(),
    scannerName,
    scannerVersion,
    scannedAt,
    findings: Array.from(findingsMap.values())
  };
}

// CLI Execution support
const isMain = process.argv[1] && (
  import.meta.url === pathToFileURL(process.argv[1]).href ||
  process.argv[1].endsWith('trivy-adapter.mjs')
);

if (isMain) {
  const args = process.argv.slice(2);
  let rawPath = null;
  let sbomPath = null;
  let outPath = null;
  let scannerVersion = '0.57.1';

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--raw' && args[i + 1]) rawPath = args[++i];
    else if (args[i] === '--sbom' && args[i + 1]) sbomPath = args[++i];
    else if (args[i] === '--out' && args[i + 1]) outPath = args[++i];
    else if (args[i] === '--scanner-version' && args[i + 1]) scannerVersion = args[++i];
  }

  if (!rawPath || !sbomPath || !outPath) {
    console.error('Usage: node scripts/trivy-adapter.mjs --raw <raw-scan.json> --sbom <sbom.json> --out <output.json> [--scanner-version <ver>]');
    process.exit(1);
  }

  try {
    const rawContent = JSON.parse(readFileSync(rawPath, 'utf8'));
    const sbomBytes = readFileSync(sbomPath);
    const sbomContent = JSON.parse(sbomBytes.toString('utf8'));
    const sbomSha256 = calculateSha256(sbomBytes);

    const converted = convertTrivyToReleaseGuard(rawContent, sbomContent, {
      sbomRawBytes: sbomBytes,
      sbomSha256,
      scannerName: 'Trivy',
      scannerVersion
    });

    writeFileSync(outPath, JSON.stringify(converted, null, 2), 'utf8');
    console.log(`Successfully converted ${converted.findings.length} findings to ${outPath}`);
    console.log(`  SBOM SHA-256: ${converted.sbomSha256}`);
    console.log(`  Scanner: ${converted.scannerName} ${converted.scannerVersion}`);
  } catch (err) {
    console.error('Error during Trivy adapter conversion:', err.message);
    process.exit(1);
  }
}
