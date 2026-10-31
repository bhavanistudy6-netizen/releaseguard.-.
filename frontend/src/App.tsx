import { useState, useEffect, useCallback } from 'react';
import { Application } from './types';
import { api } from './api/client';
import { GovernanceNoticeBanner } from './components/common/NoticeBanner';
import { ActiveTab, Header } from './components/common/Header';
import { ApplicationList } from './components/applications/ApplicationList';
import { ApplicationDetailView } from './components/applications/ApplicationDetailView';
import { EnvironmentView } from './components/deployments/EnvironmentView';
import { ReleaseComparisonView } from './components/comparison/ReleaseComparisonView';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LoginPage } from './components/auth/LoginPage';
import { UserManagementView } from './components/admin/UserManagementView';
import { RefreshCw } from 'lucide-react';

function Dashboard() {
  const { user, loading, hasRole } = useAuth();
  const [activeTab, setActiveTab] = useState<ActiveTab>('applications');
  const [applications, setApplications] = useState<Application[]>([]);
  const [selectedApp, setSelectedApp] = useState<Application | null>(null);
  const [applicationError, setApplicationError] = useState<string | null>(null);

  const fetchApps = useCallback(async () => {
    try {
      setApplicationError(null);
      const response = await api.getApplications(0, 50);
      setApplications(response.content);
    } catch {
      setApplicationError('Unable to refresh the application selector.');
    }
  }, []);

  useEffect(() => {
    if (user) {
      void fetchApps();
    }
  }, [user, fetchApps]);

  const backToApplications = () => {
    setSelectedApp(null);
    setActiveTab('applications');
    void fetchApps();
  };

  const handleTabChange = (tab: ActiveTab) => {
    setActiveTab(tab);
    if (tab === 'applications') {
      setSelectedApp(null);
    }
    void fetchApps();
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="text-center space-y-3">
          <RefreshCw className="w-8 h-8 text-emerald-600 animate-spin mx-auto" />
          <p className="text-sm font-semibold text-slate-600">Verifying ReleaseGuard session...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <LoginPage />;
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
      <GovernanceNoticeBanner />

      <Header
        activeTab={activeTab}
        onTabChange={handleTabChange}
        selectedAppName={selectedApp ? selectedApp.name : undefined}
        onBackToApps={backToApplications}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {applicationError && (
          <div
            role="alert"
            className="mb-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700"
          >
            {applicationError}
            <button
              type="button"
              onClick={() => void fetchApps()}
              className="ml-3 font-semibold underline"
            >
              Retry
            </button>
          </div>
        )}

        {activeTab === 'applications' && (
          selectedApp ? (
            <ApplicationDetailView
              key={selectedApp.id}
              application={selectedApp}
              applications={applications}
              onBack={backToApplications}
            />
          ) : (
            <ApplicationList
              onSelectApplication={(application) => {
                setSelectedApp(application);
                setActiveTab('applications');
                void fetchApps();
              }}
            />
          )
        )}

        {activeTab === 'environments' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
              <label
                htmlFor="environment-application"
                className="text-xs font-semibold text-slate-700 uppercase tracking-wider"
              >
                Select Active Application:
              </label>

              <select
                id="environment-application"
                value={selectedApp?.id ?? ''}
                onChange={(event) => {
                  const application = applications.find(
                    (item) => item.id === Number(event.target.value)
                  );
                  setSelectedApp(application ?? null);
                }}
                className="px-3 py-1.5 border border-slate-300 rounded-lg text-sm font-semibold focus:ring-2 focus:ring-emerald-500 outline-none"
              >
                <option value="">Select an application</option>
                {applications.map((application) => (
                  <option key={application.id} value={application.id}>
                    {application.name} (#{application.id} — {application.ownerTeam})
                  </option>
                ))}
              </select>
            </div>

            {selectedApp ? (
              <EnvironmentView
                key={selectedApp.id}
                application={selectedApp}
                releases={[]}
              />
            ) : (
              <div className="p-8 text-center text-sm text-slate-500">
                Select an application to view its environments.
              </div>
            )}
          </div>
        )}

        {activeTab === 'comparison' && (
          <ReleaseComparisonView
            applications={applications}
            initialApplication={selectedApp ?? undefined}
          />
        )}

        {activeTab === 'admin' && hasRole('ADMIN') && (
          <UserManagementView />
        )}
      </main>

      <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500">
        <p>
          ReleaseGuard Governance Dashboard — Enterprise Software Supply Chain Security
        </p>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <Dashboard />
    </AuthProvider>
  );
}
