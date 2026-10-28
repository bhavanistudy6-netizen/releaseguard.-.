import React, { useEffect, useState, useCallback } from 'react';
import {
  Application,
  Deployment,
  DeploymentEnvironment,
  PageResponse,
  Release,
} from '../../types';
import { api, ApiError } from '../../api/client';
import { StatusBadge } from '../common/StatusBadge';
import { AlertBanner } from '../common/AlertBanner';
import { Pagination } from '../common/Pagination';
import { RecordDeploymentModal } from './RecordDeploymentModal';
import { RecordRollbackModal } from './RecordRollbackModal';
import { DeployedFindingsModal } from './DeployedFindingsModal';
import {
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Server,
} from 'lucide-react';

interface EnvironmentViewProps {
  application: Application;
  releases: Release[];
}

export const EnvironmentView: React.FC<EnvironmentViewProps> = ({
  application,
  releases,
}) => {
  const [selectedEnv, setSelectedEnv] = useState<DeploymentEnvironment>('STAGING');

  // Current deployment per environment
  const [currentDeployments, setCurrentDeployments] = useState<
    Record<DeploymentEnvironment, Deployment | null>
  >({
    DEVELOPMENT: null,
    STAGING: null,
    PRODUCTION: null,
  });

  // History for selected environment
  const [history, setHistory] = useState<PageResponse<Deployment>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [historyPage, setHistoryPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modals
  const [deployModalOpen, setDeployModalOpen] = useState(false);
  const [rollbackModalOpen, setRollbackModalOpen] = useState(false);
  const [findingsModalOpen, setFindingsModalOpen] = useState(false);

  // Fetch current deployments for all 3 environments
  const fetchCurrentDeployments = useCallback(async () => {
    try {
      const [dev, stg, prd] = await Promise.all([
        api.getCurrentDeployment(application.id, 'DEVELOPMENT'),
        api.getCurrentDeployment(application.id, 'STAGING'),
        api.getCurrentDeployment(application.id, 'PRODUCTION'),
      ]);
      setCurrentDeployments({
        DEVELOPMENT: dev,
        STAGING: stg,
        PRODUCTION: prd,
      });
    } catch {
      // ignore
    }
  }, [application.id]);

  // Fetch history for selected environment
  const fetchHistory = useCallback(
    async (env: DeploymentEnvironment, pageToLoad = 0) => {
      try {
        setLoading(true);
        setError(null);
        const res = await api.getDeploymentHistory(application.id, env, pageToLoad, 10);
        setHistory(res);
        setHistoryPage(res.page);
      } catch (err) {
        if (err instanceof ApiError) {
          setError(`[${err.status}] ${err.message}`);
        } else {
          setError('Failed to load deployment history.');
        }
      } finally {
        setLoading(false);
      }
    },
    [application.id]
  );

  useEffect(() => {
    fetchCurrentDeployments();
  }, [fetchCurrentDeployments]);

  useEffect(() => {
    fetchHistory(selectedEnv, 0);
  }, [selectedEnv, fetchHistory]);

  const current = currentDeployments[selectedEnv];

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight">
            Environment Deployment Traceability
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Deployment status, current release pointers, and audit history for {application.name}.
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => {
              fetchCurrentDeployments();
              fetchHistory(selectedEnv, historyPage);
            }}
            disabled={loading}
            className="p-2 text-slate-600 hover:text-slate-900 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 transition shadow-sm"
            title="Refresh environments"
            aria-label="Refresh environment status"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-emerald-600' : ''}`} />
          </button>
          <button
            onClick={() => setDeployModalOpen(true)}
            className="inline-flex items-center px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
          >
            <Plus className="w-4 h-4 mr-1.5" />
            Record Deployment
          </button>
        </div>
      </div>

      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}

      {/* 3 Environment Status Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {(['DEVELOPMENT', 'STAGING', 'PRODUCTION'] as DeploymentEnvironment[]).map((env) => {
          const dep = currentDeployments[env];
          const isSelected = selectedEnv === env;

          return (
            <div
              key={env}
              onClick={() => setSelectedEnv(env)}
              className={`p-5 rounded-xl border transition cursor-pointer relative shadow-sm ${
                isSelected
                  ? 'bg-white border-emerald-500 ring-2 ring-emerald-500/20'
                  : 'bg-white border-slate-200 hover:border-slate-300'
              }`}
            >
              <div className="flex items-center justify-between mb-3">
                <StatusBadge environment={env} />
                {dep?.deploymentType === 'ROLLBACK' && (
                  <span className="text-[10px] font-bold text-amber-800 bg-amber-100 border border-amber-300 px-2 py-0.5 rounded-full flex items-center">
                    <RotateCcw className="w-2.5 h-2.5 mr-1" />
                    Restored
                  </span>
                )}
              </div>

              {dep ? (
                <div className="space-y-2">
                  <div className="flex items-baseline space-x-2">
                    <span className="text-lg font-bold font-mono text-slate-900">
                      {dep.releaseVersion}
                    </span>
                    <span className="text-xs text-slate-400 font-mono">
                      (ID #{dep.id})
                    </span>
                  </div>
                  <div className="text-xs text-slate-600 space-y-0.5">
                    <p>
                      Deployed by <strong className="text-slate-800">{dep.deployedBy}</strong>
                    </p>
                    <p className="text-slate-400">
                      {new Date(dep.deployedAt).toLocaleString()}
                    </p>
                    {dep.notes && (
                      <p className="italic text-slate-500 truncate" title={dep.notes}>
                        "{dep.notes}"
                      </p>
                    )}
                  </div>
                </div>
              ) : (
                <div className="py-3 text-xs text-slate-400 italic">
                  No active deployment recorded in {env}.
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Active Environment Action Bar & History */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center space-x-3">
            <Server className="w-5 h-5 text-slate-500" />
            <div>
              <h3 className="text-sm font-bold text-slate-900">
                {selectedEnv} Deployment History &amp; Actions
              </h3>
              <p className="text-xs text-slate-500">
                Current active pointer:{' '}
                {current ? (
                  <strong className="text-slate-800 font-mono">
                    #{current.id} ({current.releaseVersion})
                  </strong>
                ) : (
                  <span className="italic">None</span>
                )}
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            {current && (
              <>
                <button
                  onClick={() => setFindingsModalOpen(true)}
                  className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold transition inline-flex items-center"
                >
                  <Search className="w-3.5 h-3.5 mr-1 text-slate-500" />
                  Deployed Findings
                </button>
                <button
                  onClick={() => setRollbackModalOpen(true)}
                  className="px-3 py-1.5 bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-300 rounded-lg text-xs font-semibold transition inline-flex items-center"
                >
                  <RotateCcw className="w-3.5 h-3.5 mr-1" />
                  Record Rollback
                </button>
              </>
            )}
          </div>
        </div>

        {/* History Table */}
        {history.content.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500">
            No deployment events recorded for {selectedEnv}.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-600">
              <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Release</th>
                  <th className="px-4 py-3">Deployed By</th>
                  <th className="px-4 py-3">Event Date</th>
                  <th className="px-4 py-3">Rollback Details</th>
                  <th className="px-4 py-3">Notes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {history.content.map((d) => (
                  <tr
                    key={d.id}
                    className={`hover:bg-slate-50/60 ${
                      current && current.id === d.id ? 'bg-emerald-50/30' : ''
                    }`}
                  >
                    <td className="px-4 py-2.5 font-mono font-semibold text-slate-800">
                      #{d.id}
                      {current && current.id === d.id && (
                        <span className="ml-1.5 text-[10px] font-bold text-emerald-700 bg-emerald-100 px-1.5 py-0.2 rounded">
                          CURRENT
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-2.5">
                      <StatusBadge deploymentType={d.deploymentType} size="sm" />
                    </td>
                    <td className="px-4 py-2.5 font-mono font-bold text-slate-900">
                      {d.releaseVersion}
                    </td>
                    <td className="px-4 py-2.5 text-slate-800 font-medium">
                      {d.deployedBy}
                    </td>
                    <td className="px-4 py-2.5 whitespace-nowrap text-slate-500 font-mono">
                      {new Date(d.deployedAt).toLocaleString()}
                    </td>
                    <td className="px-4 py-2.5 text-[11px] max-w-xs">
                      {d.deploymentType === 'ROLLBACK' ? (
                        <span className="text-amber-900 font-mono">
                          Restored #{d.rollbackTargetDeploymentId} from #{d.rollbackFromDeploymentId}
                          {d.rollbackReason && (
                            <span className="block font-sans text-slate-500 italic truncate" title={d.rollbackReason}>
                              "{d.rollbackReason}"
                            </span>
                          )}
                        </span>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td className="px-4 py-2.5 max-w-xs truncate text-slate-500" title={d.notes || ''}>
                      {d.notes || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <Pagination
          page={history.page}
          size={history.size}
          totalElements={history.totalElements}
          totalPages={history.totalPages}
          onPageChange={(p) => fetchHistory(selectedEnv, p)}
        />
      </div>

      {/* Record Deployment Modal */}
      <RecordDeploymentModal
        isOpen={deployModalOpen}
        applicationId={application.id}
        applicationName={application.name}
        defaultEnvironment={selectedEnv}
        releases={releases}
        onClose={() => setDeployModalOpen(false)}
        onRecorded={() => {
          fetchCurrentDeployments();
          fetchHistory(selectedEnv, 0);
        }}
      />

      {/* Record Rollback Modal */}
      {current && (
        <RecordRollbackModal
          isOpen={rollbackModalOpen}
          applicationId={application.id}
          applicationName={application.name}
          environment={selectedEnv}
          currentDeployment={current}
          historyDeployments={history.content}
          onClose={() => setRollbackModalOpen(false)}
          onRecorded={() => {
            fetchCurrentDeployments();
            fetchHistory(selectedEnv, 0);
          }}
        />
      )}

      {/* Deployed Findings Modal */}
      {current && (
        <DeployedFindingsModal
          isOpen={findingsModalOpen}
          applicationId={application.id}
          environment={selectedEnv}
          currentReleaseId={current.releaseId}
          currentReleaseVersion={current.releaseVersion}
          onClose={() => setFindingsModalOpen(false)}
        />
      )}
    </div>
  );
};
