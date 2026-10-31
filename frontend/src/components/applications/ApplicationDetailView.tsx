import React, { useState, useEffect, useCallback } from 'react';
import { Application, PageResponse, Release } from '../../types';
import { api, ApiError } from '../../api/client';
import { StatusBadge } from '../common/StatusBadge';
import { AlertBanner } from '../common/AlertBanner';
import { Pagination } from '../common/Pagination';
import { RegisterReleaseModal } from '../releases/RegisterReleaseModal';
import { ReleaseDetailView } from '../releases/ReleaseDetailView';
import { EnvironmentView } from '../deployments/EnvironmentView';
import { ReleaseComparisonView } from '../comparison/ReleaseComparisonView';
import { useAuth } from '../../context/AuthContext';
import {
  ArrowLeft,
  ArrowRight,
  ExternalLink,
  FolderGit2,
  GitCompare,
  Lock,
  Package,
  Plus,
  RefreshCw,
  Server,
  Users,
} from 'lucide-react';

interface ApplicationDetailViewProps {
  application: Application;
  onBack: () => void;
  applications: Application[];
}

export const ApplicationDetailView: React.FC<ApplicationDetailViewProps> = ({
  application,
  onBack,
  applications,
}) => {
  const { can } = useAuth();
  const canCreateRelease = can('create-release');

  const [activeTab, setActiveTab] = useState<'releases' | 'deployments' | 'compare'>('releases');
  const [releasesData, setReleasesData] = useState<PageResponse<Release>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [currentPage, setCurrentPage] = useState(0);
  const [loadingReleases, setLoadingReleases] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Selected release to inspect details
  const [selectedReleaseId, setSelectedReleaseId] = useState<number | null>(null);

  // Register release modal
  const [registerReleaseModalOpen, setRegisterReleaseModalOpen] = useState(false);

  const fetchReleases = useCallback(
    async (pageToLoad = 0) => {
      try {
        setLoadingReleases(true);
        setError(null);
        const res = await api.getReleases(application.id, pageToLoad, 10);
        setReleasesData(res);
        setCurrentPage(res.page);
      } catch (err) {
        if (err instanceof ApiError) {
          setError(`[${err.status}] ${err.message}`);
        } else {
          setError('Failed to load application releases.');
        }
      } finally {
        setLoadingReleases(false);
      }
    },
    [application.id]
  );

  useEffect(() => {
    fetchReleases(0);
  }, [fetchReleases]);

  // If a release is selected, show the ReleaseDetailView workspace
  if (selectedReleaseId !== null) {
    return (
      <ReleaseDetailView
        application={application}
        releaseId={selectedReleaseId}
        onBack={() => {
          setSelectedReleaseId(null);
          fetchReleases(currentPage);
        }}
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* Back button */}
      <div>
        <button
          onClick={onBack}
          className="inline-flex items-center text-sm font-semibold text-slate-600 hover:text-emerald-700 transition"
        >
          <ArrowLeft className="w-4 h-4 mr-1.5" />
          Back to Applications
        </button>
      </div>

      {/* Application Overview Card */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center space-x-3">
              <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-600">
                Application #{application.id}
              </span>
              <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
                {application.name}
              </h1>
            </div>
            {application.description && (
              <p className="text-sm text-slate-600 max-w-2xl">{application.description}</p>
            )}
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => canCreateRelease && setRegisterReleaseModalOpen(true)}
              disabled={!canCreateRelease}
              title={canCreateRelease ? 'Register Release' : 'Action restricted: Requires DEVELOPER or ADMIN role'}
              className={`inline-flex items-center px-4 py-2 rounded-lg text-sm font-semibold shadow-sm transition ${
                canCreateRelease
                  ? 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-600/20'
                  : 'bg-slate-200 text-slate-400 cursor-not-allowed'
              }`}
            >
              {canCreateRelease ? <Plus className="w-4 h-4 mr-1.5" /> : <Lock className="w-4 h-4 mr-1.5" />}
              Register Release
            </button>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-6 text-xs text-slate-500 pt-3 border-t border-slate-100">
          <div className="flex items-center text-slate-700 font-medium">
            <Users className="w-4 h-4 mr-1.5 text-slate-400" />
            Owner Team: <strong className="ml-1 text-slate-900">{application.ownerTeam}</strong>
          </div>
          <div className="flex items-center text-emerald-700">
            <FolderGit2 className="w-4 h-4 mr-1.5 text-slate-400" />
            <a
              href={application.repositoryUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="font-mono hover:underline inline-flex items-center"
            >
              {application.repositoryUrl}
              <ExternalLink className="w-3 h-3 ml-1" />
            </a>
          </div>
          <div className="text-slate-400">
            Registered: {new Date(application.createdAt).toLocaleString()}
          </div>
        </div>
      </div>

      {/* Sub Tabs */}
      <div className="flex border-b border-slate-200 space-x-6 text-sm font-medium">
        <button
          onClick={() => setActiveTab('releases')}
          className={`pb-3 border-b-2 transition flex items-center ${
            activeTab === 'releases'
              ? 'border-emerald-600 text-emerald-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Package className="w-4 h-4 mr-1.5" />
          Releases
          <span className="ml-2 px-1.5 py-0.2 bg-slate-100 text-slate-600 rounded-full text-xs">
            {releasesData.totalElements}
          </span>
        </button>
        <button
          onClick={() => setActiveTab('deployments')}
          className={`pb-3 border-b-2 transition flex items-center ${
            activeTab === 'deployments'
              ? 'border-emerald-600 text-emerald-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Server className="w-4 h-4 mr-1.5" />
          Deployments &amp; Environments
        </button>
        <button
          onClick={() => setActiveTab('compare')}
          className={`pb-3 border-b-2 transition flex items-center ${
            activeTab === 'compare'
              ? 'border-emerald-600 text-emerald-700 font-semibold'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <GitCompare className="w-4 h-4 mr-1.5" />
          Compare Dependencies
        </button>
      </div>

      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}

      {/* Tab Content */}
      {activeTab === 'releases' && (
        <div className="space-y-4">
          {releasesData.content.length === 0 ? (
            <div className="p-12 text-center bg-white rounded-xl border border-dashed border-slate-300">
              <Package className="w-12 h-12 text-slate-400 mx-auto mb-3" />
              <h3 className="text-base font-semibold text-slate-800">No releases registered</h3>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                Register a release version (e.g. v1.0.0) with git commit SHA and docker image.
              </p>
              <button
                onClick={() => canCreateRelease && setRegisterReleaseModalOpen(true)}
                disabled={!canCreateRelease}
                title={canCreateRelease ? 'Register First Release' : 'Action restricted: Requires DEVELOPER or ADMIN role'}
                className={`mt-4 inline-flex items-center px-4 py-2 text-xs font-semibold rounded-lg transition ${
                  canCreateRelease
                    ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                    : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                }`}
              >
                {canCreateRelease ? <Plus className="w-4 h-4 mr-1.5" /> : <Lock className="w-4 h-4 mr-1.5" />}
                Register First Release
              </button>
            </div>
          ) : (
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
              <div className="p-4 border-b border-slate-100 flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                  Releases List ({releasesData.totalElements})
                </span>
                <button
                  onClick={() => fetchReleases(currentPage)}
                  disabled={loadingReleases}
                  className="text-xs text-slate-500 hover:text-slate-800 inline-flex items-center"
                >
                  <RefreshCw className={`w-3.5 h-3.5 mr-1 ${loadingReleases ? 'animate-spin' : ''}`} />
                  Refresh
                </button>
              </div>

              <div className="divide-y divide-slate-100">
                {releasesData.content.map((r) => (
                  <div
                    key={r.id}
                    onClick={() => setSelectedReleaseId(r.id)}
                    className="p-5 hover:bg-slate-50/80 transition cursor-pointer flex flex-col md:flex-row md:items-center justify-between gap-4 group"
                  >
                    <div className="space-y-1 flex-1">
                      <div className="flex items-center space-x-3">
                        <span className="font-mono text-sm font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">
                          {r.version}
                        </span>
                        <StatusBadge status={r.status} />
                        <span className="text-xs text-slate-400 font-mono">ID #{r.id}</span>
                      </div>
                      <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 pt-1 font-mono">
                        <span className="truncate max-w-xs text-slate-700">
                          Image: <strong className="font-semibold text-slate-900">{r.dockerImage}</strong>
                        </span>
                        <span className="text-slate-500 truncate max-w-xs">
                          Commit: {r.gitCommitSha.slice(0, 8)}...
                        </span>
                        <span className="text-slate-400 font-sans">
                          Registered {new Date(r.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center space-x-2">
                      <span className="text-xs font-semibold text-emerald-600 group-hover:translate-x-0.5 transition-transform inline-flex items-center">
                        Inspect Workspace
                        <ArrowRight className="w-4 h-4 ml-1" />
                      </span>
                    </div>
                  </div>
                ))}
              </div>

              <Pagination
                page={releasesData.page}
                size={releasesData.size}
                totalElements={releasesData.totalElements}
                totalPages={releasesData.totalPages}
                onPageChange={(p) => fetchReleases(p)}
              />
            </div>
          )}
        </div>
      )}

      {activeTab === 'deployments' && (
        <EnvironmentView
          application={application}
          releases={releasesData.content}
        />
      )}

      {activeTab === 'compare' && (
        <ReleaseComparisonView
          applications={applications}
          initialApplication={application}
          initialReleases={releasesData.content}
        />
      )}

      <RegisterReleaseModal
        isOpen={registerReleaseModalOpen}
        applicationId={application.id}
        applicationName={application.name}
        onClose={() => setRegisterReleaseModalOpen(false)}
        onCreated={() => fetchReleases(currentPage)}
      />
    </div>
  );
};
