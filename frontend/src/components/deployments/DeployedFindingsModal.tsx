import React, { useEffect, useState, useCallback } from 'react';
import { Modal } from '../common/Modal';
import {
  DeploymentEnvironment,
  PageResponse,
  VulnerabilityFinding,
  VulnerabilityImport,
} from '../../types';
import { api, ApiError } from '../../api/client';
import { AlertBanner } from '../common/AlertBanner';
import { StatusBadge } from '../common/StatusBadge';
import { Pagination } from '../common/Pagination';
import { RefreshCw, ShieldAlert, ShieldCheck } from 'lucide-react';

interface DeployedFindingsModalProps {
  isOpen: boolean;
  applicationId: number;
  environment: DeploymentEnvironment;
  currentReleaseId: number;
  currentReleaseVersion: string;
  onClose: () => void;
}

export const DeployedFindingsModal: React.FC<DeployedFindingsModalProps> = ({
  isOpen,
  applicationId,
  environment,
  currentReleaseId,
  currentReleaseVersion,
  onClose,
}) => {
  const [imports, setImports] = useState<VulnerabilityImport[]>([]);
  const [selectedImportId, setSelectedImportId] = useState<number | null>(null);
  const [findings, setFindings] = useState<PageResponse<VulnerabilityFinding>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchImports = useCallback(async () => {
    try {
      const res = await api.getVulnerabilityImports(applicationId, currentReleaseId, 0, 50);
      setImports(res.content);
      if (res.content.length > 0 && selectedImportId === null) {
        setSelectedImportId(res.content[0].id);
      }
    } catch {
      // ignore
    }
  }, [applicationId, currentReleaseId, selectedImportId]);

  const fetchFindings = useCallback(
    async (importId: number, pageToLoad = 0) => {
      try {
        setLoading(true);
        setError(null);
        const res = await api.getDeployedFindings(
          applicationId,
          environment,
          importId,
          pageToLoad,
          10
        );
        setFindings(res);
        setCurrentPage(res.page);
      } catch (err) {
        if (err instanceof ApiError) {
          setError(`[${err.status}] ${err.message}`);
        } else {
          setError('Failed to load deployed findings.');
        }
      } finally {
        setLoading(false);
      }
    },
    [applicationId, environment]
  );

  useEffect(() => {
    if (isOpen) {
      fetchImports();
    }
  }, [isOpen, fetchImports]);

  useEffect(() => {
    if (selectedImportId !== null && isOpen) {
      fetchFindings(selectedImportId, 0);
    }
  }, [selectedImportId, isOpen, fetchFindings]);

  const selectedImport = imports.find((i) => i.id === selectedImportId);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      maxWidth="2xl"
      title={`Active Vulnerability Findings • ${environment}`}
      description={`Findings for current release ${currentReleaseVersion} from an explicitly selected scan import.`}
    >
      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}

      <div className="space-y-4">
        {/* Scan Selector */}
        <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
          <label className="font-semibold text-slate-700 uppercase tracking-wider">
            Select Scan Import:
          </label>
          {imports.length === 0 ? (
            <span className="text-amber-800 font-medium">
              No scans imported for currently deployed release {currentReleaseVersion}
            </span>
          ) : (
            <select
              value={selectedImportId || ''}
              onChange={(e) => setSelectedImportId(Number(e.target.value))}
              className="px-3 py-1.5 border border-slate-300 rounded-lg text-xs font-semibold focus:ring-2 focus:ring-emerald-500 outline-none"
            >
              {imports.map((imp) => (
                <option key={imp.id} value={imp.id}>
                  Import #{imp.id} • {imp.scannerName} ({imp.findingCount} findings, scanned {new Date(imp.scannedAt).toLocaleDateString()})
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Findings Table */}
        {imports.length === 0 ? (
          <div className="p-8 text-center bg-white rounded-lg border border-dashed border-slate-300 text-xs text-slate-500">
            <ShieldAlert className="w-8 h-8 text-slate-400 mx-auto mb-2" />
            No vulnerability scans available for this release.
          </div>
        ) : selectedImport && selectedImport.findingCount === 0 ? (
          <div className="p-8 text-center bg-emerald-50/50 rounded-lg border border-emerald-200 text-xs text-emerald-800">
            <ShieldCheck className="w-8 h-8 text-emerald-600 mx-auto mb-2" />
            <strong className="block text-emerald-900 text-sm">0 Findings Reported</strong>
            The selected scan reported zero vulnerabilities for this deployed release.
          </div>
        ) : (
          <div className="border border-slate-200 rounded-lg overflow-hidden">
            <div className="p-3 bg-slate-50 border-b border-slate-100 flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-700">
                Findings ({findings.totalElements})
              </span>
              <button
                onClick={() => selectedImportId && fetchFindings(selectedImportId, currentPage)}
                disabled={loading}
                className="text-slate-500 hover:text-slate-800 inline-flex items-center"
              >
                <RefreshCw className={`w-3.5 h-3.5 mr-1 ${loading ? 'animate-spin' : ''}`} />
                Refresh
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-600">
                <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
                  <tr>
                    <th className="px-3 py-2">Severity</th>
                    <th className="px-3 py-2">ID</th>
                    <th className="px-3 py-2">Component</th>
                    <th className="px-3 py-2">Fixed In</th>
                    <th className="px-3 py-2">Description</th>
                    <th className="px-3 py-2">Owner Team</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {findings.content.map((f) => (
                    <tr key={f.id} className="hover:bg-slate-50/60">
                      <td className="px-3 py-2">
                        <StatusBadge severity={f.severity} size="sm" />
                      </td>
                      <td className="px-3 py-2 font-mono font-bold text-slate-900">
                        {f.vulnerabilityId}
                      </td>
                      <td className="px-3 py-2 font-medium text-slate-800">
                        {f.componentName} <span className="font-mono text-slate-500">v{f.componentVersion}</span>
                      </td>
                      <td className="px-3 py-2 font-mono text-emerald-700">
                        {f.fixedVersion || '—'}
                      </td>
                      <td className="px-3 py-2 max-w-xs truncate" title={f.description || undefined}>
                        {f.description}
                      </td>
                      <td className="px-3 py-2 text-slate-700">{f.ownerTeam}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={findings.page}
              size={findings.size}
              totalElements={findings.totalElements}
              totalPages={findings.totalPages}
              onPageChange={(p) => selectedImportId && fetchFindings(selectedImportId, p)}
            />
          </div>
        )}

        <div className="flex justify-end pt-2">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-slate-300 rounded-lg text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
          >
            Close
          </button>
        </div>
      </div>
    </Modal>
  );
};
