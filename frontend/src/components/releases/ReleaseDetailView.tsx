import React, { useEffect, useState, useCallback } from 'react';
import {
  Application,
  CreateDecisionRequest,
  DecisionAction,
  PageResponse,
  Release,
  ReleaseDecision,
  SbomComponent,
  SbomDocument,
  VulnerabilityFinding,
  VulnerabilityImport,
} from '../../types';
import { api, ApiError } from '../../api/client';
import { StatusBadge } from '../common/StatusBadge';
import { AlertBanner } from '../common/AlertBanner';
import { Pagination } from '../common/Pagination';
import { Modal } from '../common/Modal';
import {
  ArrowLeft,
  CheckCircle,
  FileCode2,
  RefreshCw,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Upload,
} from 'lucide-react';

interface ReleaseDetailViewProps {
  application: Application;
  releaseId: number;
  onBack: () => void;
}

export const ReleaseDetailView: React.FC<ReleaseDetailViewProps> = ({
  application,
  releaseId,
  onBack,
}) => {
  const [release, setRelease] = useState<Release | null>(null);
  const [loadingRelease, setLoadingRelease] = useState(true);
  const [releaseError, setReleaseError] = useState<string | null>(null);

  // Active sub-tab: 'overview' | 'sbom' | 'vulnerabilities' | 'approvals'
  const [activeTab, setActiveTab] = useState<'approvals' | 'sbom' | 'vulnerabilities'>('approvals');

  // SBOM State
  const [sbomDoc, setSbomDoc] = useState<SbomDocument | null>(null);
  const [loadingSbom, setLoadingSbom] = useState(false);
  const [sbomComponents, setSbomComponents] = useState<PageResponse<SbomComponent>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [sbomPage, setSbomPage] = useState(0);
  const [uploadingSbom, setUploadingSbom] = useState(false);
  const [sbomError, setSbomError] = useState<string | null>(null);
  const [sbomSuccess, setSbomSuccess] = useState<string | null>(null);

  // Vulnerability Imports & Findings State
  const [imports, setImports] = useState<VulnerabilityImport[]>([]);
  const [selectedImportId, setSelectedImportId] = useState<number | null>(null);
  const [findings, setFindings] = useState<PageResponse<VulnerabilityFinding>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [findingsPage, setFindingsPage] = useState(0);
  const [loadingFindings, setLoadingFindings] = useState(false);
  const [vulnError, setVulnError] = useState<string | null>(null);
  const [vulnSuccess, setVulnSuccess] = useState<string | null>(null);
  const [uploadingVuln, setUploadingVuln] = useState(false);

  // Approvals & Decision History State
  const [decisions, setDecisions] = useState<PageResponse<ReleaseDecision>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [decisionsPage, setDecisionsPage] = useState(0);
  const [loadingDecisions, setLoadingDecisions] = useState(false);
  const [decisionError, setDecisionError] = useState<string | null>(null);
  const [decisionSuccess, setDecisionSuccess] = useState<string | null>(null);

  // Decision Modal State
  const [decisionModalOpen, setDecisionModalOpen] = useState(false);
  const [decisionAction, setDecisionAction] = useState<DecisionAction>('SUBMIT');
  const [decisionActor, setDecisionActor] = useState('');
  const [decisionReason, setDecisionReason] = useState('');
  const [decisionImportId, setDecisionImportId] = useState<number | ''>('');
  const [submittingDecision, setSubmittingDecision] = useState(false);

  // 1. Fetch Release
  const fetchRelease = useCallback(async () => {
    try {
      setLoadingRelease(true);
      setReleaseError(null);
      const res = await api.getRelease(application.id, releaseId);
      setRelease(res);
    } catch (err) {
      if (err instanceof ApiError) {
        setReleaseError(`[${err.status}] ${err.message}`);
      } else {
        setReleaseError('Failed to load release details.');
      }
    } finally {
      setLoadingRelease(false);
    }
  }, [application.id, releaseId]);

  // 2. Fetch SBOM & Components
  const fetchSbom = useCallback(
    async (pageToLoad = 0) => {
      try {
        setLoadingSbom(true);
        setSbomError(null);
        const doc = await api.getSbom(application.id, releaseId);
        setSbomDoc(doc);
        if (doc) {
          const compRes = await api.getSbomComponents(application.id, releaseId, pageToLoad, 10);
          setSbomComponents(compRes);
          setSbomPage(compRes.page);
        } else {
          setSbomComponents({
            content: [],
            page: 0,
            size: 10,
            totalElements: 0,
            totalPages: 0,
          });
        }
      } catch (err) {
        if (err instanceof ApiError) {
          setSbomError(`[${err.status}] ${err.message}`);
        } else {
          setSbomError('Failed to load SBOM inventory.');
        }
      } finally {
        setLoadingSbom(false);
      }
    },
    [application.id, releaseId]
  );

  // 3. Fetch Vulnerability Imports
  const fetchImports = useCallback(async () => {
    try {
      const res = await api.getVulnerabilityImports(application.id, releaseId, 0, 50);
      setImports(res.content);
      if (res.content.length > 0 && selectedImportId === null) {
        setSelectedImportId(res.content[0].id);
      }
    } catch {
      // ignore
    }
  }, [application.id, releaseId, selectedImportId]);

  // 4. Fetch Findings for selected import
  const fetchFindings = useCallback(
    async (importId: number, pageToLoad = 0) => {
      try {
        setLoadingFindings(true);
        setVulnError(null);
        const res = await api.getFindings(application.id, releaseId, importId, pageToLoad, 10);
        setFindings(res);
        setFindingsPage(res.page);
      } catch (err) {
        if (err instanceof ApiError) {
          setVulnError(`[${err.status}] ${err.message}`);
        } else {
          setVulnError('Failed to load vulnerability findings.');
        }
      } finally {
        setLoadingFindings(false);
      }
    },
    [application.id, releaseId]
  );

  // 5. Fetch Decision History
  const fetchDecisions = useCallback(
    async (pageToLoad = 0) => {
      try {
        setLoadingDecisions(true);
        setDecisionError(null);
        const res = await api.getDecisions(application.id, releaseId, pageToLoad, 10);
        setDecisions(res);
        setDecisionsPage(res.page);
      } catch (err) {
        if (err instanceof ApiError) {
          setDecisionError(`[${err.status}] ${err.message}`);
        } else {
          setDecisionError('Failed to load decision history.');
        }
      } finally {
        setLoadingDecisions(false);
      }
    },
    [application.id, releaseId]
  );

  useEffect(() => {
    fetchRelease();
    fetchSbom(0);
    fetchImports();
    fetchDecisions(0);
  }, [fetchRelease, fetchSbom, fetchImports, fetchDecisions]);

  useEffect(() => {
    if (selectedImportId !== null) {
      fetchFindings(selectedImportId, 0);
    }
  }, [selectedImportId, fetchFindings]);

  // SBOM File Upload
  const handleSbomFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 10 * 1024 * 1024) {
      setSbomError('File size exceeds the 10 MiB limit.');
      return;
    }

    try {
      setUploadingSbom(true);
      setSbomError(null);
      setSbomSuccess(null);
      const res = await api.uploadSbom(application.id, releaseId, file);
      setSbomDoc(res);
      setSbomSuccess(`Successfully imported SBOM: ${res.originalFilename} (${res.componentCount} components)`);
      fetchSbom(0);
    } catch (err) {
      if (err instanceof ApiError) {
        setSbomError(`[${err.status}] ${err.message}`);
      } else {
        setSbomError('Failed to upload SBOM file.');
      }
    } finally {
      setUploadingSbom(false);
      // Reset input
      e.target.value = '';
    }
  };

  // Vulnerability Report Upload
  const handleVulnFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 10 * 1024 * 1024) {
      setVulnError('File size exceeds the 10 MiB limit.');
      return;
    }

    try {
      setUploadingVuln(true);
      setVulnError(null);
      setVulnSuccess(null);
      const res = await api.uploadVulnerabilityReport(application.id, releaseId, file);
      setVulnSuccess(
        `Imported scan #${res.id} from ${res.scannerName} (${res.findingCount} findings)`
      );
      await fetchImports();
      setSelectedImportId(res.id);
    } catch (err) {
      if (err instanceof ApiError) {
        setVulnError(`[${err.status}] ${err.message}`);
      } else {
        setVulnError('Failed to upload vulnerability report.');
      }
    } finally {
      setUploadingVuln(false);
      e.target.value = '';
    }
  };

  // Open Decision Modal
  const openDecisionModal = (action: DecisionAction) => {
    setDecisionAction(action);
    setDecisionActor(application.ownerTeam);
    setDecisionReason('');
    setDecisionImportId(imports.length > 0 ? imports[0].id : '');
    setDecisionModalOpen(true);
  };

  // Submit Decision
  const handleDecisionSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!decisionActor.trim() || !decisionReason.trim()) {
      setDecisionError('Actor and Reason are required.');
      return;
    }

    if (decisionAction === 'APPROVE' && !decisionImportId) {
      setDecisionError('Approving a release requires selecting a valid vulnerability scan import.');
      return;
    }

    try {
      setSubmittingDecision(true);
      setDecisionError(null);
      setDecisionSuccess(null);

      const payload: CreateDecisionRequest = {
        action: decisionAction,
        actor: decisionActor.trim(),
        reason: decisionReason.trim(),
        vulnerabilityImportId:
          decisionAction === 'APPROVE' && decisionImportId ? Number(decisionImportId) : null,
      };

      const res = await api.createDecision(application.id, releaseId, payload);
      setDecisionSuccess(
        `Action ${res.action} applied. Release status transitioned from ${res.fromStatus} to ${res.toStatus}.`
      );
      setDecisionModalOpen(false);
      await fetchRelease();
      await fetchDecisions(0);
    } catch (err) {
      if (err instanceof ApiError) {
        setDecisionError(`[${err.status}] ${err.message}`);
      } else {
        setDecisionError('Failed to submit release decision.');
      }
    } finally {
      setSubmittingDecision(false);
    }
  };

  if (loadingRelease && !release) {
    return (
      <div className="p-12 text-center bg-white rounded-xl border border-slate-200">
        <RefreshCw className="w-8 h-8 text-emerald-600 animate-spin mx-auto mb-2" />
        <p className="text-sm text-slate-500 font-medium">Loading release details...</p>
      </div>
    );
  }

  if (!release) {
    return (
      <div className="space-y-4">
        <button
          onClick={onBack}
          className="inline-flex items-center text-sm font-medium text-slate-600 hover:text-slate-900"
        >
          <ArrowLeft className="w-4 h-4 mr-1.5" />
          Back to Releases
        </button>
        {releaseError && <AlertBanner type="error" message={releaseError} />}
      </div>
    );
  }

  const selectedImport = imports.find((i) => i.id === selectedImportId);

  return (
    <div className="space-y-6">
      {/* Back button */}
      <div>
        <button
          onClick={onBack}
          className="inline-flex items-center text-sm font-semibold text-slate-600 hover:text-emerald-700 transition"
        >
          <ArrowLeft className="w-4 h-4 mr-1.5" />
          Back to {application.name} Releases
        </button>
      </div>

      {/* Release Overview Header */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center space-x-3">
              <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-600">
                Release #{release.id}
              </span>
              <h1 className="text-2xl font-bold text-slate-900 tracking-tight font-mono">
                {release.version}
              </h1>
              <StatusBadge status={release.status} />
            </div>
            <p className="text-xs text-slate-500">
              Application: <strong className="text-slate-700">{application.name}</strong> • Owner Team:{' '}
              <strong className="text-slate-700">{application.ownerTeam}</strong>
            </p>
          </div>

          {/* Action Buttons depending on status */}
          <div className="flex flex-wrap items-center gap-2">
            {release.status === 'DRAFT' && (
              <button
                onClick={() => openDecisionModal('SUBMIT')}
                className="px-3.5 py-1.5 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
              >
                Submit for Review
              </button>
            )}

            {release.status === 'READY_FOR_REVIEW' && (
              <>
                <button
                  onClick={() => openDecisionModal('APPROVE')}
                  className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition inline-flex items-center"
                >
                  <ShieldCheck className="w-3.5 h-3.5 mr-1" />
                  Approve Release
                </button>
                <button
                  onClick={() => openDecisionModal('REJECT')}
                  className="px-3.5 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
                >
                  Reject to Draft
                </button>
              </>
            )}

            {release.status === 'APPROVED' && (
              <button
                onClick={() => openDecisionModal('REVOKE')}
                className="px-3.5 py-1.5 bg-amber-700 hover:bg-amber-800 text-white rounded-lg text-xs font-semibold shadow-sm transition"
              >
                Revoke Approval
              </button>
            )}

            {release.status !== 'RETIRED' && (
              <button
                onClick={() => openDecisionModal('RETIRE')}
                className="px-3.5 py-1.5 border border-slate-300 text-slate-700 hover:bg-slate-50 rounded-lg text-xs font-semibold transition"
              >
                Retire Release
              </button>
            )}
          </div>
        </div>

        {/* Technical Attributes */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 pt-4 border-t border-slate-100 text-xs">
          <div>
            <span className="text-slate-500 block font-medium">Git Commit SHA</span>
            <span className="font-mono text-slate-800 break-all">{release.gitCommitSha}</span>
          </div>
          <div>
            <span className="text-slate-500 block font-medium">Docker Image</span>
            <span className="font-mono text-slate-800">{release.dockerImage}</span>
          </div>
          <div>
            <span className="text-slate-500 block font-medium">Image Digest</span>
            <span className="font-mono text-slate-800 break-all">
              {release.imageDigest ? release.imageDigest : 'None (Optional)'}
            </span>
          </div>
          <div>
            <span className="text-slate-500 block font-medium">Registered / Version</span>
            <span className="text-slate-800 font-mono">
              {new Date(release.createdAt).toLocaleString()} (v{release.lockVersion})
            </span>
          </div>
        </div>
      </div>

      {/* Sub-tab Navigation */}
      <div className="flex border-b border-slate-200 space-x-6 text-sm font-medium">
        <button
          onClick={() => setActiveTab('approvals')}
          className={`pb-3 border-b-2 transition flex items-center ${
            activeTab === 'approvals'
              ? 'border-emerald-600 text-emerald-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Shield className="w-4 h-4 mr-1.5" />
          Approval Decisions &amp; Governance
          <span className="ml-2 px-1.5 py-0.2 bg-slate-100 text-slate-600 rounded-full text-xs">
            {decisions.totalElements}
          </span>
        </button>
        <button
          onClick={() => setActiveTab('sbom')}
          className={`pb-3 border-b-2 transition flex items-center ${
            activeTab === 'sbom'
              ? 'border-emerald-600 text-emerald-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <FileCode2 className="w-4 h-4 mr-1.5" />
          CycloneDX SBOM Inventory
          <span className="ml-2 px-1.5 py-0.2 bg-slate-100 text-slate-600 rounded-full text-xs">
            {sbomDoc ? sbomDoc.componentCount : '0'}
          </span>
        </button>
        <button
          onClick={() => setActiveTab('vulnerabilities')}
          className={`pb-3 border-b-2 transition flex items-center ${
            activeTab === 'vulnerabilities'
              ? 'border-emerald-600 text-emerald-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <ShieldAlert className="w-4 h-4 mr-1.5" />
          Vulnerability Scan Findings
          <span className="ml-2 px-1.5 py-0.2 bg-slate-100 text-slate-600 rounded-full text-xs">
            {imports.length} scans
          </span>
        </button>
      </div>

      {/* ----------------- TAB: APPROVAL DECISIONS & GOVERNANCE ----------------- */}
      {activeTab === 'approvals' && (
        <div className="space-y-6">
          {decisionError && <AlertBanner type="error" message={decisionError} onClose={() => setDecisionError(null)} />}
          {decisionSuccess && <AlertBanner type="success" message={decisionSuccess} onClose={() => setDecisionSuccess(null)} />}

          {/* Policy Information Box */}
          <div className="p-4 bg-emerald-50/70 border border-emerald-200 rounded-xl text-xs space-y-2">
            <div className="flex items-center space-x-2 text-emerald-900 font-bold">
              <ShieldCheck className="w-4 h-4 text-emerald-700" />
              <span>Release Policy Rules (Active Policy: release-policy-v1)</span>
            </div>
            <ul className="list-disc list-inside text-emerald-800 space-y-1 pl-1">
              <li>
                <strong>Scan Freshness:</strong> Attached vulnerability scan must be ≤ 7 days old at decision time.
              </li>
              <li>
                <strong>Severity Enforcement:</strong> CRITICAL, HIGH, UNKNOWN, or missing severity data blocks release approval.
              </li>
              <li>
                <strong>Allowed Severities:</strong> MEDIUM, LOW, and INFORMATIONAL findings are permitted with review.
              </li>
              <li>
                <strong>Clean Scans:</strong> A verified fresh imported scan with 0 reported findings satisfies approval criteria.
              </li>
              <li>
                <strong>Production Gate:</strong> Production deployments re-evaluate policy freshness and require APPROVED status with current evidence.
              </li>
            </ul>
          </div>

          {/* Decision History Table */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-800">Decision Audit History</h3>
              <button
                onClick={() => fetchDecisions(decisionsPage)}
                disabled={loadingDecisions}
                className="text-xs text-slate-500 hover:text-slate-800 inline-flex items-center"
              >
                <RefreshCw className={`w-3.5 h-3.5 mr-1 ${loadingDecisions ? 'animate-spin' : ''}`} />
                Refresh
              </button>
            </div>

            {decisions.content.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-500">
                No governance decisions recorded yet. Status is {release.status}.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-600">
                  <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">Decision ID</th>
                      <th className="px-4 py-3">Action</th>
                      <th className="px-4 py-3">Transition</th>
                      <th className="px-4 py-3">Actor</th>
                      <th className="px-4 py-3">Reason</th>
                      <th className="px-4 py-3">Scan Import ID</th>
                      <th className="px-4 py-3">Policy Version</th>
                      <th className="px-4 py-3">Decided At</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {decisions.content.map((d) => (
                      <tr key={d.id} className="hover:bg-slate-50/60">
                        <td className="px-4 py-3 font-mono font-semibold text-slate-800">#{d.id}</td>
                        <td className="px-4 py-3">
                          <span className="font-semibold text-slate-900 px-2 py-0.5 rounded bg-slate-100">
                            {d.action}
                          </span>
                        </td>
                        <td className="px-4 py-3 font-mono text-[11px]">
                          {d.fromStatus} → {d.toStatus}
                        </td>
                        <td className="px-4 py-3 font-medium text-slate-900">{d.actor}</td>
                        <td className="px-4 py-3 max-w-xs truncate" title={d.reason}>
                          {d.reason}
                        </td>
                        <td className="px-4 py-3 font-mono">
                          {d.vulnerabilityImportId ? `#${d.vulnerabilityImportId}` : '—'}
                        </td>
                        <td className="px-4 py-3 font-mono text-[11px]">
                          {d.policyVersion || '—'}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-slate-500">
                          {new Date(d.decidedAt).toLocaleString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <Pagination
              page={decisions.page}
              size={decisions.size}
              totalElements={decisions.totalElements}
              totalPages={decisions.totalPages}
              onPageChange={(p) => fetchDecisions(p)}
            />
          </div>
        </div>
      )}

      {/* ----------------- TAB: CYCLONEDX SBOM INVENTORY ----------------- */}
      {activeTab === 'sbom' && (
        <div className="space-y-6">
          {sbomError && <AlertBanner type="error" message={sbomError} onClose={() => setSbomError(null)} />}
          {sbomSuccess && <AlertBanner type="success" message={sbomSuccess} onClose={() => setSbomSuccess(null)} />}

          {!sbomDoc ? (
            <div className="p-8 text-center bg-white rounded-xl border border-dashed border-slate-300 space-y-4">
              <FileCode2 className="w-12 h-12 text-slate-400 mx-auto" />
              <div>
                <h3 className="text-base font-semibold text-slate-800">No SBOM Uploaded Yet</h3>
                <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
                  Upload a CycloneDX JSON 1.6 Software Bill of Materials (max 10 MiB) to register the software inventory.
                </p>
              </div>
              <div>
                <label className="inline-flex items-center px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold cursor-pointer shadow-sm transition">
                  <Upload className="w-4 h-4 mr-1.5" />
                  {uploadingSbom ? 'Uploading & Parsing...' : 'Upload CycloneDX SBOM'}
                  <input
                    type="file"
                    accept=".json"
                    className="hidden"
                    disabled={uploadingSbom}
                    onChange={handleSbomFileUpload}
                  />
                </label>
              </div>
            </div>
          ) : (
            <>
              {/* SBOM Document Metadata Card */}
              <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 border-b border-slate-100 pb-3">
                  <div className="flex items-center space-x-2">
                    <CheckCircle className="w-4 h-4 text-emerald-600" />
                    <h3 className="text-sm font-bold text-slate-800">
                      Imported SBOM Document: {sbomDoc.originalFilename}
                    </h3>
                  </div>
                  <span className="font-mono text-xs text-slate-500">
                    Uploaded {new Date(sbomDoc.uploadedAt).toLocaleString()}
                  </span>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs">
                  <div>
                    <span className="text-slate-500 block">Format &amp; Spec</span>
                    <span className="font-medium text-slate-800">
                      {sbomDoc.bomFormat} v{sbomDoc.specVersion}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Total Components</span>
                    <span className="font-bold text-slate-900">{sbomDoc.componentCount}</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-slate-500 block">Document SHA-256</span>
                    <span className="font-mono text-[11px] text-slate-800 break-all select-all">
                      {sbomDoc.documentSha256}
                    </span>
                  </div>
                </div>
              </div>

              {/* Components Table */}
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="p-4 border-b border-slate-100 flex items-center justify-between">
                  <h4 className="text-sm font-bold text-slate-800">Component Inventory</h4>
                  <button
                    onClick={() => fetchSbom(sbomPage)}
                    disabled={loadingSbom}
                    className="text-xs text-slate-500 hover:text-slate-800 inline-flex items-center"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 mr-1 ${loadingSbom ? 'animate-spin' : ''}`} />
                    Refresh
                  </button>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs text-slate-600">
                    <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
                      <tr>
                        <th className="px-4 py-3">ID</th>
                        <th className="px-4 py-3">Name</th>
                        <th className="px-4 py-3">Version</th>
                        <th className="px-4 py-3">Type</th>
                        <th className="px-4 py-3">Group</th>
                        <th className="px-4 py-3">BOM Ref</th>
                        <th className="px-4 py-3">Package URL (purl)</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {sbomComponents.content.map((c) => (
                        <tr key={c.id} className="hover:bg-slate-50/60">
                          <td className="px-4 py-2.5 font-mono text-slate-500">#{c.id}</td>
                          <td className="px-4 py-2.5 font-bold text-slate-900">{c.name}</td>
                          <td className="px-4 py-2.5 font-mono text-slate-700">
                            {c.version || '—'}
                          </td>
                          <td className="px-4 py-2.5 uppercase text-[10px] tracking-wide font-medium text-slate-500">
                            {c.componentType}
                          </td>
                          <td className="px-4 py-2.5 text-slate-600">{c.groupName || '—'}</td>
                          <td className="px-4 py-2.5 font-mono text-[11px] text-slate-600">
                            {c.bomRef || '—'}
                          </td>
                          <td className="px-4 py-2.5 font-mono text-[11px] text-slate-500 max-w-xs truncate" title={c.purl || ''}>
                            {c.purl || '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <Pagination
                  page={sbomComponents.page}
                  size={sbomComponents.size}
                  totalElements={sbomComponents.totalElements}
                  totalPages={sbomComponents.totalPages}
                  onPageChange={(p) => fetchSbom(p)}
                />
              </div>
            </>
          )}
        </div>
      )}

      {/* ----------------- TAB: VULNERABILITY FINDINGS ----------------- */}
      {activeTab === 'vulnerabilities' && (
        <div className="space-y-6">
          {vulnError && <AlertBanner type="error" message={vulnError} onClose={() => setVulnError(null)} />}
          {vulnSuccess && <AlertBanner type="success" message={vulnSuccess} onClose={() => setVulnSuccess(null)} />}

          {/* Action Bar: Scan Selector & Upload */}
          <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex flex-col sm:flex-row sm:items-center gap-3">
              <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Select Scan Import:
              </label>
              {imports.length === 0 ? (
                <span className="text-xs text-amber-800 bg-amber-50 border border-amber-200 px-3 py-1.5 rounded-lg">
                  No scan reports imported yet
                </span>
              ) : (
                <select
                  value={selectedImportId || ''}
                  onChange={(e) => setSelectedImportId(Number(e.target.value))}
                  className="px-3 py-1.5 border border-slate-300 rounded-lg text-xs font-medium focus:ring-2 focus:ring-emerald-500 outline-none"
                >
                  {imports.map((imp) => (
                    <option key={imp.id} value={imp.id}>
                      Scan #{imp.id} • {imp.scannerName} v{imp.scannerVersion} ({imp.findingCount} findings, {new Date(imp.scannedAt).toLocaleDateString()})
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div>
              <label className="inline-flex items-center px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold cursor-pointer shadow-sm transition">
                <Upload className="w-3.5 h-3.5 mr-1.5" />
                {uploadingVuln ? 'Importing Report...' : 'Import Vulnerability Report'}
                <input
                  type="file"
                  accept=".json"
                  className="hidden"
                  disabled={uploadingVuln}
                  onChange={handleVulnFileUpload}
                />
              </label>
            </div>
          </div>

          {/* Selected Import Metadata */}
          {selectedImport && (
            <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm text-xs grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div>
                <span className="text-slate-500 block">Scanner Tool</span>
                <span className="font-semibold text-slate-800">
                  {selectedImport.scannerName} v{selectedImport.scannerVersion}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Scanned Timestamp</span>
                <span className="font-mono text-slate-800">
                  {new Date(selectedImport.scannedAt).toLocaleString()}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Findings Reported</span>
                <span className="font-bold text-slate-900">{selectedImport.findingCount}</span>
              </div>
              <div>
                <span className="text-slate-500 block">Report Import Date</span>
                <span className="font-mono text-slate-800">
                  {new Date(selectedImport.importedAt).toLocaleString()}
                </span>
              </div>
            </div>
          )}

          {/* Findings Table */}
          {imports.length === 0 ? (
            <div className="p-12 text-center bg-white rounded-xl border border-dashed border-slate-300">
              <ShieldAlert className="w-10 h-10 text-slate-400 mx-auto mb-2" />
              <h4 className="text-sm font-semibold text-slate-800">Missing Scan Evidence</h4>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                No vulnerability reports have been imported for this release. A missing scan is not treated as safe.
              </p>
            </div>
          ) : selectedImport && selectedImport.findingCount === 0 ? (
            <div className="p-12 text-center bg-white rounded-xl border border-emerald-200 shadow-sm">
              <ShieldCheck className="w-10 h-10 text-emerald-600 mx-auto mb-2" />
              <h4 className="text-sm font-bold text-emerald-900">Zero Findings Reported</h4>
              <p className="text-xs text-emerald-800 mt-1 max-w-sm mx-auto">
                Scanner {selectedImport.scannerName} reported 0 vulnerabilities for this release’s SBOM components.
              </p>
            </div>
          ) : (
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
              <div className="p-4 border-b border-slate-100 flex items-center justify-between">
                <h4 className="text-sm font-bold text-slate-800">
                  Reported Vulnerabilities (Import #{selectedImportId})
                </h4>
                <button
                  onClick={() => selectedImportId && fetchFindings(selectedImportId, findingsPage)}
                  disabled={loadingFindings}
                  className="text-xs text-slate-500 hover:text-slate-800 inline-flex items-center"
                >
                  <RefreshCw className={`w-3.5 h-3.5 mr-1 ${loadingFindings ? 'animate-spin' : ''}`} />
                  Refresh
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-600">
                  <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">Severity</th>
                      <th className="px-4 py-3">Vulnerability ID</th>
                      <th className="px-4 py-3">Component</th>
                      <th className="px-4 py-3">Component Version</th>
                      <th className="px-4 py-3">Fixed In</th>
                      <th className="px-4 py-3">Description</th>
                      <th className="px-4 py-3">Owner Team</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {findings.content.map((f) => (
                      <tr key={f.id} className="hover:bg-slate-50/60">
                        <td className="px-4 py-2.5">
                          <StatusBadge severity={f.severity} size="sm" />
                        </td>
                        <td className="px-4 py-2.5 font-mono font-bold text-slate-900">
                          {f.vulnerabilityId}
                        </td>
                        <td className="px-4 py-2.5 font-medium text-slate-800">
                          {f.componentName}
                        </td>
                        <td className="px-4 py-2.5 font-mono text-slate-700">
                          {f.componentVersion || '—'}
                        </td>
                        <td className="px-4 py-2.5 font-mono text-emerald-700 font-semibold">
                          {f.fixedVersion || '—'}
                        </td>
                        <td className="px-4 py-2.5 max-w-sm truncate" title={f.description}>
                          {f.description}
                        </td>
                        <td className="px-4 py-2.5 text-slate-700 font-medium">
                          {f.ownerTeam}
                        </td>
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
        </div>
      )}

      {/* Decision Action Modal */}
      <Modal
        isOpen={decisionModalOpen}
        onClose={() => setDecisionModalOpen(false)}
        title={`Governance Action: ${decisionAction}`}
        description={`Execute ${decisionAction} on ${release.version} of ${application.name}.`}
      >
        <form onSubmit={handleDecisionSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Actor *
            </label>
            <input
              type="text"
              required
              value={decisionActor}
              onChange={(e) => setDecisionActor(e.target.value)}
              placeholder="e.g. Planner Team or Security Reviewer"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Reason *
            </label>
            <textarea
              required
              rows={2}
              value={decisionReason}
              onChange={(e) => setDecisionReason(e.target.value)}
              placeholder="e.g. Security scan reviewed and all high/critical findings remediated."
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
            />
          </div>

          {decisionAction === 'APPROVE' && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                Scan Evidence (Required for Approval) *
              </label>
              {imports.length === 0 ? (
                <p className="text-xs text-rose-600 font-medium">
                  No scan import exists for this release. You cannot approve without attaching scan evidence.
                </p>
              ) : (
                <select
                  required
                  value={decisionImportId}
                  onChange={(e) => setDecisionImportId(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none font-medium"
                >
                  <option value="" disabled>Select scan import...</option>
                  {imports.map((imp) => (
                    <option key={imp.id} value={imp.id}>
                      Import #{imp.id} ({imp.scannerName}, {imp.findingCount} findings, scanned {new Date(imp.scannedAt).toLocaleDateString()})
                    </option>
                  ))}
                </select>
              )}
            </div>
          )}

          <div className="flex justify-end space-x-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={() => setDecisionModalOpen(false)}
              className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submittingDecision || (decisionAction === 'APPROVE' && !decisionImportId)}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition disabled:opacity-50"
            >
              {submittingDecision ? 'Applying Decision...' : `Confirm ${decisionAction}`}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
