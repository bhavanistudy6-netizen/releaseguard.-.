import { useState, useEffect, useCallback } from 'react';
import { Application } from './types';
import { api } from './api/client';
import { GovernanceNoticeBanner } from './components/common/NoticeBanner';
import { ActiveTab, Header } from './components/common/Header';
import { ApplicationList } from './components/applications/ApplicationList';
import { ApplicationDetailView } from './components/applications/ApplicationDetailView';
import { EnvironmentView } from './components/deployments/EnvironmentView';
import { ReleaseComparisonView } from './components/comparison/ReleaseComparisonView';

export default function App() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('applications');
  const [applications, setApplications] = useState<Application[]>([]);
  const [selectedApp, setSelectedApp] = useState<Application | null>(null);

  const fetchApps = useCallback(async () => {
    try {
      const res = await api.getApplications(0, 50);
      setApplications(res.content);
      if (res.content.length > 0 && !selectedApp) {
        setSelectedApp(res.content[0]);
      }
    } catch {
      // ignore
    }
  }, [selectedApp]);

  useEffect(() => {
    fetchApps();
  }, [fetchApps]);

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
      <GovernanceNoticeBanner />
      <Header
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
        }}
        selectedAppName={selectedApp ? selectedApp.name : undefined}
        onBackToApps={() => {
          setSelectedApp(null);
          setActiveTab('applications');
        }}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeTab === 'applications' && (
          <>
            {selectedApp ? (
              <ApplicationDetailView
                application={selectedApp}
                applications={applications}
                onBack={() => {
                  setSelectedApp(null);
                  fetchApps();
                }}
              />
            ) : (
              <ApplicationList
                onSelectApplication={(app) => {
                  setSelectedApp(app);
                  setActiveTab('applications');
                }}
              />
            )}
          </>
        )}

        {activeTab === 'environments' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
              <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
                Select Active Application:
              </label>
              <select
                value={selectedApp?.id || ''}
                onChange={(e) => {
                  const found = applications.find((a) => a.id === Number(e.target.value));
                  if (found) setSelectedApp(found);
                }}
                className="px-3 py-1.5 border border-slate-300 rounded-lg text-sm font-semibold focus:ring-2 focus:ring-emerald-500 outline-none"
              >
                {applications.map((app) => (
                  <option key={app.id} value={app.id}>
                    {app.name} (#{app.id} • {app.ownerTeam})
                  </option>
                ))}
              </select>
            </div>

            {selectedApp ? (
              <EnvironmentView application={selectedApp} releases={[]} />
            ) : (
              <div className="p-8 text-center text-xs text-slate-500">
                Please register an application first to track environments.
              </div>
            )}
          </div>
        )}

        {activeTab === 'comparison' && (
          <ReleaseComparisonView
            applications={applications}
            initialApplication={selectedApp || undefined}
          />
        )}
      </main>

      <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500">
        <p>ReleaseGuard Governance Dashboard • Enterprise Software Supply Chain Security</p>
      </footer>
    </div>
  );
}
