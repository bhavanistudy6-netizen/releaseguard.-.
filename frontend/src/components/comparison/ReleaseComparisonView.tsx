import React, { useState, useEffect, useCallback } from 'react';
import {
  Application,
  ChangeType,
  Release,
  ReleaseComparisonResponse,
} from '../../types';
import { api, ApiError } from '../../api/client';
import { StatusBadge } from '../common/StatusBadge';
import { AlertBanner } from '../common/AlertBanner';
import {
  GitCompare,
  Search,
} from 'lucide-react';

interface ReleaseComparisonViewProps {
  applications: Application[];
  initialApplication?: Application;
  initialReleases?: Release[];
}

export const ReleaseComparisonView: React.FC<ReleaseComparisonViewProps> = ({
  applications,
  initialApplication,
  initialReleases,
}) => {
  const [selectedAppId, setSelectedAppId] = useState<number>(
    initialApplication ? initialApplication.id : applications.length > 0 ? applications[0].id : 0
  );
  const [releases, setReleases] = useState<Release[]>(initialReleases || []);
  const [loadingReleases, setLoadingReleases] = useState(false);

  const [fromReleaseId, setFromReleaseId] = useState<number | ''>('');
  const [toReleaseId, setToReleaseId] = useState<number | ''>('');
  const [comparison, setComparison] = useState<ReleaseComparisonResponse | null>(null);
  const [loadingComparison, setLoadingComparison] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Filter
  const [filterType, setFilterType] = useState<ChangeType | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Fetch releases when app changes
  const fetchReleases = useCallback(async (appId: number) => {
    if (!appId) return;
    try {
      setLoadingReleases(true);
      setError(null);
      const res = await api.getReleases(appId, 0, 100);
      setReleases(res.content);
      if (res.content.length >= 2) {
        setFromReleaseId(res.content[0].id);
        setToReleaseId(res.content[1].id);
      } else if (res.content.length === 1) {
        setFromReleaseId(res.content[0].id);
        setToReleaseId('');
      } else {
        setFromReleaseId('');
        setToReleaseId('');
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(`[${err.status}] ${err.message}`);
      } else {
        setError('Failed to load releases for comparison.');
      }
    } finally {
      setLoadingReleases(false);
    }
  }, []);

  useEffect(() => {
    if (selectedAppId) {
      fetchReleases(selectedAppId);
    }
  }, [selectedAppId, fetchReleases]);

  // Execute Comparison
  const handleCompare = async () => {
    if (!selectedAppId || !fromReleaseId || !toReleaseId) {
      setError('Please select an Application, a From Release, and a To Release.');
      return;
    }

    if (fromReleaseId === toReleaseId) {
      setError('From Release and To Release must be different for a meaningful comparison.');
      return;
    }

    try {
      setLoadingComparison(true);
      setError(null);
      const res = await api.compareReleases(selectedAppId, Number(fromReleaseId), Number(toReleaseId));
      setComparison(res);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 404) {
          setError(
            'Both releases must have an uploaded CycloneDX SBOM to compare dependencies. Check that both releases have SBOMs.'
          );
        } else {
          setError(`[${err.status}] ${err.message}`);
        }
      } else {
        setError('Failed to compare releases.');
      }
    } finally {
      setLoadingComparison(false);
    }
  };

  const formatIdentity = (identity: unknown): string => {
    if (!identity) return '—';
    if (typeof identity === 'string') return identity;
    if (typeof identity === 'object' && identity !== null) {
      const obj = identity as { namespace?: string; name?: string };
      const ns = obj.namespace ? `${obj.namespace}/` : '';
      const name = obj.name || '';
      return `${ns}${name}`;
    }
    return String(identity);
  };

  const formatVersions = (target: unknown): string => {
    if (!target) return '—';
    if (Array.isArray(target)) {
      if (target.length === 0) return '—';
      return target.map((x) => (typeof x === 'object' && x ? x.version || 'unknown' : String(x))).join(', ');
    }
    if (typeof target === 'object' && target !== null && 'version' in target) {
      return (target as { version?: string }).version || '—';
    }
    return String(target);
  };

  const formatComponentType = (c: { identity?: unknown; before?: unknown; after?: unknown }): string => {
    if (typeof c.identity === 'object' && c.identity !== null && 'type' in c.identity) {
      return (c.identity as { type: string }).type;
    }
    const extractType = (t: unknown): string | undefined => {
      if (Array.isArray(t) && t[0] && typeof t[0] === 'object' && 'componentType' in t[0]) {
        return (t[0] as { componentType?: string }).componentType;
      }
      if (typeof t === 'object' && t !== null && 'componentType' in t) {
        return (t as { componentType?: string }).componentType;
      }
      return undefined;
    };
    return extractType(c.after) || extractType(c.before) || 'LIBRARY';
  };

  const filteredChanges = comparison
    ? comparison.changes.filter((c) => {
        if (filterType !== 'ALL' && c.changeType !== filterType) return false;
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase();
          const matchIdentity = formatIdentity(c.identity).toLowerCase().includes(q);
          const matchReason = c.reason?.toLowerCase().includes(q) || false;
          return matchIdentity || matchReason;
        }
        return true;
      })
    : [];

  const counts = comparison
    ? {
        total: comparison.changes.length,
        added: comparison.changes.filter((c) => c.changeType === 'ADDED').length,
        removed: comparison.changes.filter((c) => c.changeType === 'REMOVED').length,
        changed: comparison.changes.filter((c) => c.changeType === 'VERSION_CHANGED').length,
        uncertain: comparison.changes.filter((c) => c.changeType === 'UNCERTAIN').length,
        unchanged: comparison.changes.filter((c) => c.changeType === 'UNCHANGED').length,
      }
    : null;

  return (
    <div className="space-y-6">
      {/* Title */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
          Release Dependency Comparison
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Side-by-side SBOM component diff matching on package URL identities (added, removed, version changed, unchanged, or uncertain).
        </p>
      </div>

      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}

      {/* Selectors Bar */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Select Application
            </label>
            <select
              value={selectedAppId}
              onChange={(e) => setSelectedAppId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-semibold focus:ring-2 focus:ring-emerald-500 outline-none"
            >
              {applications.map((app) => (
                <option key={app.id} value={app.id}>
                  {app.name} (#{app.id})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Base Release (From)
            </label>
            <select
              value={fromReleaseId}
              disabled={loadingReleases || releases.length === 0}
              onChange={(e) => setFromReleaseId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-emerald-500 outline-none"
            >
              <option value="" disabled>Select base release...</option>
              {releases.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.version} ({r.status})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              Target Release (To)
            </label>
            <select
              value={toReleaseId}
              disabled={loadingReleases || releases.length === 0}
              onChange={(e) => setToReleaseId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-emerald-500 outline-none"
            >
              <option value="" disabled>Select target release...</option>
              {releases.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.version} ({r.status})
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="flex justify-end pt-2 border-t border-slate-100">
          <button
            onClick={handleCompare}
            disabled={loadingComparison || !fromReleaseId || !toReleaseId}
            className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition disabled:opacity-50 inline-flex items-center"
          >
            <GitCompare className="w-4 h-4 mr-2" />
            {loadingComparison ? 'Comparing Dependencies...' : 'Compare Dependencies'}
          </button>
        </div>
      </div>

      {/* Comparison Results */}
      {counts && comparison && (
        <div className="space-y-6">
          {/* Summary Badges */}
          <div className="grid grid-cols-2 sm:grid-cols-6 gap-3">
            <div className="bg-white p-3.5 rounded-xl border border-slate-200 text-center shadow-sm">
              <span className="text-xs text-slate-500 block font-medium">Total Tracked</span>
              <span className="text-lg font-bold text-slate-900">{counts.total}</span>
            </div>
            <div className="bg-emerald-50/50 p-3.5 rounded-xl border border-emerald-200 text-center shadow-sm">
              <span className="text-xs text-emerald-800 block font-medium">Added</span>
              <span className="text-lg font-bold text-emerald-900">+{counts.added}</span>
            </div>
            <div className="bg-rose-50/50 p-3.5 rounded-xl border border-rose-200 text-center shadow-sm">
              <span className="text-xs text-rose-800 block font-medium">Removed</span>
              <span className="text-lg font-bold text-rose-900">-{counts.removed}</span>
            </div>
            <div className="bg-blue-50/50 p-3.5 rounded-xl border border-blue-200 text-center shadow-sm">
              <span className="text-xs text-blue-800 block font-medium">Version Changed</span>
              <span className="text-lg font-bold text-blue-900">{counts.changed}</span>
            </div>
            <div className="bg-amber-50/50 p-3.5 rounded-xl border border-amber-200 text-center shadow-sm">
              <span className="text-xs text-amber-800 block font-medium">Uncertain</span>
              <span className="text-lg font-bold text-amber-900">{counts.uncertain}</span>
            </div>
            <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-200 text-center shadow-sm">
              <span className="text-xs text-slate-500 block font-medium">Unchanged</span>
              <span className="text-lg font-bold text-slate-700">{counts.unchanged}</span>
            </div>
          </div>

          {/* Filter and Search */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-3">
              <div className="flex flex-wrap items-center gap-1.5 text-xs font-semibold">
                <span className="text-slate-500 mr-1 uppercase tracking-wider text-[11px]">Filter:</span>
                {(['ALL', 'ADDED', 'REMOVED', 'VERSION_CHANGED', 'UNCERTAIN', 'UNCHANGED'] as const).map(
                  (type) => (
                    <button
                      key={type}
                      onClick={() => setFilterType(type)}
                      className={`px-2.5 py-1 rounded-md transition ${
                        filterType === type
                          ? 'bg-slate-900 text-white'
                          : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                      }`}
                    >
                      {type}
                    </button>
                  )
                )}
              </div>

              <div className="relative">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search package name..."
                  className="pl-8 pr-3 py-1.5 border border-slate-300 rounded-lg text-xs w-48 sm:w-64 focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            {/* Changes Table */}
            {filteredChanges.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-500">
                No component changes match the selected filter.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-600">
                  <thead className="bg-slate-50 text-slate-700 uppercase font-semibold border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">Change Type</th>
                      <th className="px-4 py-3">Package Identity</th>
                      <th className="px-4 py-3">Type</th>
                      <th className="px-4 py-3">Before (Base)</th>
                      <th className="px-4 py-3">After (Target)</th>
                      <th className="px-4 py-3">Reason / Details</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-mono">
                    {filteredChanges.map((c, idx) => (
                      <tr key={idx} className="hover:bg-slate-50/60 font-sans">
                        <td className="px-4 py-2.5">
                          <StatusBadge changeType={c.changeType} size="sm" />
                        </td>
                        <td className="px-4 py-2.5 font-bold font-mono text-slate-900">
                          {formatIdentity(c.identity)}
                        </td>
                        <td className="px-4 py-2.5 uppercase text-[10px] text-slate-500 font-mono">
                          {formatComponentType(c)}
                        </td>
                        <td className="px-4 py-2.5 font-mono text-slate-700">
                          {formatVersions(c.before)}
                        </td>
                        <td className="px-4 py-2.5 font-mono text-slate-900 font-semibold">
                          {formatVersions(c.after)}
                        </td>
                        <td className="px-4 py-2.5 text-xs font-sans text-slate-500 max-w-sm truncate" title={c.reason || ''}>
                          {c.reason ? (
                            <span className="text-amber-800 font-medium">{c.reason}</span>
                          ) : (
                            '—'
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
