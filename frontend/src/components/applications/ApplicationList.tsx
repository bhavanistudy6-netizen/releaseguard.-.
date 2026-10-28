import React, { useEffect, useState, useCallback } from 'react';
import { Application, PageResponse } from '../../types';
import { api, ApiError } from '../../api/client';
import { Plus, FolderGit2, Users, ExternalLink, ArrowRight, RefreshCw } from 'lucide-react';
import { Pagination } from '../common/Pagination';
import { AlertBanner } from '../common/AlertBanner';
import { RegisterApplicationModal } from './RegisterApplicationModal';

interface ApplicationListProps {
  onSelectApplication: (app: Application) => void;
}

export const ApplicationList: React.FC<ApplicationListProps> = ({ onSelectApplication }) => {
  const [data, setData] = useState<PageResponse<Application>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);

  const fetchApplications = useCallback(async (pageToLoad = 0) => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.getApplications(pageToLoad, 10);
      setData(res);
      setCurrentPage(res.page);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(`[${err.status}] ${err.message}`);
      } else {
        setError('Failed to load applications. Is the backend running on port 8080?');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchApplications(0);
  }, [fetchApplications]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Applications Registry</h1>
          <p className="text-sm text-slate-500 mt-1">
            Registered software services, repositories, and designated owner teams.
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={() => fetchApplications(currentPage)}
            disabled={loading}
            className="p-2 text-slate-600 hover:text-slate-900 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 transition shadow-sm"
            title="Refresh list"
            aria-label="Refresh application list"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-emerald-600' : ''}`} />
          </button>
          <button
            onClick={() => setIsModalOpen(true)}
            className="inline-flex items-center px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition shadow-emerald-600/20"
          >
            <Plus className="w-4 h-4 mr-1.5" />
            Register Application
          </button>
        </div>
      </div>

      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}

      {loading && data.content.length === 0 ? (
        <div className="p-12 text-center bg-white rounded-xl border border-slate-200 shadow-sm">
          <RefreshCw className="w-8 h-8 text-emerald-600 animate-spin mx-auto mb-3" />
          <p className="text-sm text-slate-500 font-medium">Loading applications...</p>
        </div>
      ) : data.content.length === 0 ? (
        <div className="p-12 text-center bg-white rounded-xl border border-dashed border-slate-300">
          <FolderGit2 className="w-12 h-12 text-slate-400 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-800">No applications registered yet</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto">
            Get started by registering an application with its repository and owner team.
          </p>
          <button
            onClick={() => setIsModalOpen(true)}
            className="mt-4 inline-flex items-center px-4 py-2 bg-emerald-600 text-white text-sm font-medium rounded-lg hover:bg-emerald-700 transition"
          >
            <Plus className="w-4 h-4 mr-1.5" />
            Register First Application
          </button>
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="divide-y divide-slate-100">
            {data.content.map((app) => (
              <div
                key={app.id}
                onClick={() => onSelectApplication(app)}
                className="p-5 hover:bg-slate-50/80 transition cursor-pointer flex flex-col md:flex-row md:items-center justify-between gap-4 group"
              >
                <div className="space-y-1.5 flex-1">
                  <div className="flex items-center space-x-3">
                    <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-600">
                      ID #{app.id}
                    </span>
                    <h3 className="text-base font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">
                      {app.name}
                    </h3>
                  </div>
                  {app.description && (
                    <p className="text-sm text-slate-600 line-clamp-2 max-w-2xl">{app.description}</p>
                  )}
                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 pt-1">
                    <span className="inline-flex items-center text-slate-700 font-medium">
                      <Users className="w-3.5 h-3.5 mr-1.5 text-slate-400" />
                      {app.ownerTeam}
                    </span>
                    <a
                      href={app.repositoryUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={(e) => e.stopPropagation()}
                      className="inline-flex items-center text-emerald-600 hover:text-emerald-800 transition"
                    >
                      <FolderGit2 className="w-3.5 h-3.5 mr-1" />
                      <span className="font-mono">{app.repositoryUrl}</span>
                      <ExternalLink className="w-3 h-3 ml-1" />
                    </a>
                    <span className="text-slate-400">
                      Created {new Date(app.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                <div className="flex items-center space-x-3 self-end md:self-center">
                  <span className="text-xs font-semibold text-emerald-600 group-hover:translate-x-0.5 transition-transform inline-flex items-center">
                    Manage Releases &amp; Deployments
                    <ArrowRight className="w-4 h-4 ml-1" />
                  </span>
                </div>
              </div>
            ))}
          </div>
          <Pagination
            page={data.page}
            size={data.size}
            totalElements={data.totalElements}
            totalPages={data.totalPages}
            onPageChange={(p) => fetchApplications(p)}
          />
        </div>
      )}

      <RegisterApplicationModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onCreated={() => fetchApplications(currentPage)}
      />
    </div>
  );
};
