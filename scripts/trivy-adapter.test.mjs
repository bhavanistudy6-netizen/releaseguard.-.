import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateSha256,
  mapSeverity,
  indexSbomComponents,
  matchComponentBomRef,
  convertTrivyToReleaseGuard
} from './trivy-adapter.mjs';

test('calculateSha256 produces lowercase 64-char hex string', () => {
  const content = Buffer.from('test SBOM content');
  const hash = calculateSha256(content);
  assert.equal(hash.length, 64);
  assert.match(hash, /^[0-9a-f]{64}$/);
});

test('mapSeverity correctly maps standard and unknown severities', () => {
  assert.equal(mapSeverity('CRITICAL'), 'CRITICAL');
  assert.equal(mapSeverity('critical'), 'CRITICAL');
  assert.equal(mapSeverity('HIGH'), 'HIGH');
  assert.equal(mapSeverity('MEDIUM'), 'MEDIUM');
  assert.equal(mapSeverity('LOW'), 'LOW');
  assert.equal(mapSeverity('INFORMATIONAL'), 'INFORMATIONAL');
  assert.equal(mapSeverity('UNKNOWN'), 'UNKNOWN');
  assert.equal(mapSeverity(null), 'UNKNOWN');
  assert.equal(mapSeverity(''), 'UNKNOWN');
  assert.equal(mapSeverity('UNRECOGNIZED_CUSTOM'), 'UNKNOWN');
});

test('indexSbomComponents indexes flat and nested components', () => {
  const sampleSbom = {
    components: [
      {
        'bom-ref': 'ref-1',
        name: 'commons-lang3',
        group: 'org.apache.commons',
        version: '3.12.0',
        purl: 'pkg:maven/org.apache.commons/commons-lang3@3.12.0'
      },
      {
        'bom-ref': 'ref-2',
        name: 'jackson-core',
        group: 'com.fasterxml.jackson.core',
        version: '2.15.0',
        components: [
          {
            'bom-ref': 'ref-3',
            name: 'nested-comp',
            group: 'org.nested',
            version: '1.0.0'
          }
        ]
      }
    ]
  };

  const index = indexSbomComponents(sampleSbom);
  assert.equal(index.totalComponents, 3);
  assert.ok(index.byBomRef.has('ref-1'));
  assert.ok(index.byBomRef.has('ref-2'));
  assert.ok(index.byBomRef.has('ref-3'));
});

test('matchComponentBomRef matches by direct BOMRef, PURL, and coordinate', () => {
  const sbom = {
    components: [
      {
        'bom-ref': 'pkg:maven/ch.qos.logback/logback-core@1.4.14?type=jar',
        name: 'logback-core',
        group: 'ch.qos.logback',
        version: '1.4.14',
        purl: 'pkg:maven/ch.qos.logback/logback-core@1.4.14?type=jar'
      }
    ]
  };
  const index = indexSbomComponents(sbom);

  // 1. Direct BOMRef
  const vuln1 = {
    VulnerabilityID: 'CVE-2024-12798',
    PkgIdentifier: { BOMRef: 'pkg:maven/ch.qos.logback/logback-core@1.4.14?type=jar' }
  };
  assert.equal(matchComponentBomRef(vuln1, index), 'pkg:maven/ch.qos.logback/logback-core@1.4.14?type=jar');

  // 2. Direct PURL
  const vuln2 = {
    VulnerabilityID: 'CVE-2024-12798',
    PkgIdentifier: { PURL: 'pkg:maven/ch.qos.logback/logback-core@1.4.14?type=jar' }
  };
  assert.equal(matchComponentBomRef(vuln2, index), 'pkg:maven/ch.qos.logback/logback-core@1.4.14?type=jar');

  // 3. Coordinate fallback
  const vuln3 = {
    VulnerabilityID: 'CVE-2024-12798',
    PkgName: 'ch.qos.logback:logback-core',
    InstalledVersion: '1.4.14'
  };
  assert.equal(matchComponentBomRef(vuln3, index), 'pkg:maven/ch.qos.logback/logback-core@1.4.14?type=jar');
});

test('matchComponentBomRef rejects unmatched finding with clear error', () => {
  const sbom = { components: [] };
  const index = indexSbomComponents(sbom);

  const vuln = {
    VulnerabilityID: 'CVE-2024-9999',
    PkgName: 'unknown:package',
    InstalledVersion: '1.0.0'
  };

  assert.throws(
    () => matchComponentBomRef(vuln, index),
    /Unmatched vulnerability finding "CVE-2024-9999"/
  );
});

test('matchComponentBomRef rejects ambiguous match across multiple components', () => {
  const sbom = {
    components: [
      {
        'bom-ref': 'ref-a',
        name: 'dup-pkg',
        group: 'org.dup',
        version: '1.0.0'
      },
      {
        'bom-ref': 'ref-b',
        name: 'dup-pkg',
        group: 'org.dup',
        version: '1.0.0'
      }
    ]
  };
  const index = indexSbomComponents(sbom);

  const vuln = {
    VulnerabilityID: 'CVE-2024-1111',
    PkgName: 'org.dup:dup-pkg',
    InstalledVersion: '1.0.0'
  };

  assert.throws(
    () => matchComponentBomRef(vuln, index),
    /Ambiguous component match/
  );
});

test('convertTrivyToReleaseGuard handles successful scan and deduplicates findings', () => {
  const rawReport = {
    SchemaVersion: 2,
    ArtifactName: 'test-artifact',
    Results: [
      {
        Target: 'Java',
        Vulnerabilities: [
          {
            VulnerabilityID: 'CVE-2024-1001',
            PkgIdentifier: { BOMRef: 'ref-comp-1' },
            SeveritySource: 'ghsa',
            Severity: 'HIGH',
            Title: 'First finding',
            FixedVersion: '1.2.0'
          },
          // Duplicate finding with same component, source, and vulnerabilityId
          {
            VulnerabilityID: 'CVE-2024-1001',
            PkgIdentifier: { BOMRef: 'ref-comp-1' },
            SeveritySource: 'ghsa',
            Severity: 'HIGH',
            Title: 'Duplicate finding entry',
            FixedVersion: '1.2.0'
          },
          {
            VulnerabilityID: 'CVE-2024-1002',
            PkgIdentifier: { BOMRef: 'ref-comp-1' },
            SeveritySource: 'nvd',
            Severity: 'LOW',
            Description: 'Second finding'
          }
        ]
      }
    ]
  };

  const sbom = {
    components: [
      {
        'bom-ref': 'ref-comp-1',
        name: 'my-library',
        version: '1.0.0'
      }
    ]
  };

  const validSha256 = 'a'.repeat(64);
  const result = convertTrivyToReleaseGuard(rawReport, sbom, {
    sbomSha256: validSha256,
    scannerName: 'Trivy',
    scannerVersion: '0.57.1',
    scannedAt: '2026-10-30T12:00:00Z'
  });

  assert.equal(result.formatVersion, '1');
  assert.equal(result.sbomSha256, validSha256);
  assert.equal(result.scannerName, 'Trivy');
  assert.equal(result.scannerVersion, '0.57.1');
  assert.equal(result.scannedAt, '2026-10-30T12:00:00Z');
  // 3 raw entries with 1 duplicate -> exactly 2 unique findings
  assert.equal(result.findings.length, 2);
  assert.equal(result.findings[0].vulnerabilityId, 'CVE-2024-1001');
  assert.equal(result.findings[0].severity, 'HIGH');
  assert.equal(result.findings[1].vulnerabilityId, 'CVE-2024-1002');
  assert.equal(result.findings[1].severity, 'LOW');
});

test('convertTrivyToReleaseGuard handles clean scan with zero findings', () => {
  const rawReport = {
    SchemaVersion: 2,
    ArtifactName: 'clean-artifact',
    Results: [
      {
        Target: 'Java',
        Vulnerabilities: []
      }
    ]
  };

  const sbom = { components: [] };
  const validSha256 = 'b'.repeat(64);

  const result = convertTrivyToReleaseGuard(rawReport, sbom, {
    sbomSha256: validSha256
  });

  assert.equal(result.formatVersion, '1');
  assert.equal(result.findings.length, 0);
  assert.equal(result.sbomSha256, validSha256);
});

test('convertTrivyToReleaseGuard rejects invalid input schemas', () => {
  const validSha256 = 'c'.repeat(64);
  assert.throws(() => convertTrivyToReleaseGuard(null, {}), /Invalid raw scanner report/);
  assert.throws(() => convertTrivyToReleaseGuard({}, {}), /missing SchemaVersion/);
  assert.throws(
    () => convertTrivyToReleaseGuard({ SchemaVersion: 2 }, {}, { sbomSha256: 'short' }),
    /Invalid sbomSha256/
  );
});

