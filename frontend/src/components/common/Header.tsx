import React from 'react';
import { Shield, Layers, GitCompare, Server, Users, LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Role } from '../../types';

export type ActiveTab = 'applications' | 'environments' | 'comparison' | 'admin';

interface HeaderProps {
  activeTab: ActiveTab;
  onTabChange: (tab: ActiveTab) => void;
  selectedAppName?: string;
  onBackToApps?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  activeTab,
  onTabChange,
  selectedAppName,
  onBackToApps,
}) => {
  const { user, logout, can } = useAuth();

  const getRoleBadgeStyle = (role: Role) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'REVIEWER':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'DEVELOPER':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      case 'DEPLOYER':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'VIEWER':
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center space-x-6">
            <div
              className="flex items-center space-x-3 cursor-pointer group"
              onClick={onBackToApps}
            >
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-500 flex items-center justify-center text-white shadow-md shadow-emerald-500/20 group-hover:scale-105 transition-transform">
                <Shield className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xl font-bold tracking-tight text-slate-900 group-hover:text-emerald-700 transition-colors">
                  ReleaseGuard
                </span>
                <span className="block text-[10px] text-slate-500 tracking-wider font-semibold uppercase">
                  Supply Chain &amp; Governance
                </span>
              </div>
            </div>

            {selectedAppName && (
              <div className="hidden md:flex items-center text-sm">
                <span className="text-slate-400 mx-2">/</span>
                <button
                  onClick={onBackToApps}
                  className="font-medium text-emerald-700 hover:text-emerald-800 transition"
                >
                  {selectedAppName}
                </button>
              </div>
            )}
          </div>

          <div className="flex items-center space-x-4">
            <nav className="flex items-center space-x-1 sm:space-x-2">
              <button
                onClick={() => onTabChange('applications')}
                className={`flex items-center px-3.5 py-2 rounded-lg text-sm font-medium transition ${
                  activeTab === 'applications'
                    ? 'bg-emerald-50 text-emerald-700 font-semibold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Layers className="w-4 h-4 mr-1.5" />
                Applications
              </button>
              <button
                onClick={() => onTabChange('environments')}
                className={`flex items-center px-3.5 py-2 rounded-lg text-sm font-medium transition ${
                  activeTab === 'environments'
                    ? 'bg-emerald-50 text-emerald-700 font-semibold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Server className="w-4 h-4 mr-1.5" />
                Environments
              </button>
              <button
                onClick={() => onTabChange('comparison')}
                className={`flex items-center px-3.5 py-2 rounded-lg text-sm font-medium transition ${
                  activeTab === 'comparison'
                    ? 'bg-emerald-50 text-emerald-700 font-semibold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <GitCompare className="w-4 h-4 mr-1.5" />
                Compare
              </button>
              {can('admin') && (
                <button
                  onClick={() => onTabChange('admin')}
                  className={`flex items-center px-3.5 py-2 rounded-lg text-sm font-medium transition ${
                    activeTab === 'admin'
                      ? 'bg-emerald-50 text-emerald-700 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Users className="w-4 h-4 mr-1.5" />
                  Users
                </button>
              )}
            </nav>

            {user && (
              <div className="flex items-center space-x-3 pl-3 border-l border-slate-200">
                <div className="hidden lg:flex flex-col items-end">
                  <div className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                    <UserIcon className="w-3 h-3 text-slate-500" />
                    {user.displayName} ({user.username})
                  </div>
                  <div className="flex items-center gap-1 mt-0.5">
                    {user.roles.map((r) => (
                      <span
                        key={r}
                        className={`text-[9px] font-bold px-1.5 py-0.2 rounded border ${getRoleBadgeStyle(r)}`}
                      >
                        {r}
                      </span>
                    ))}
                  </div>
                </div>
                <button
                  onClick={() => void logout()}
                  title="Sign Out"
                  className="p-2 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
