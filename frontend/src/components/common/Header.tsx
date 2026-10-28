import React from 'react';
import { Shield, Layers, GitCompare, Server } from 'lucide-react';

export type ActiveTab = 'applications' | 'environments' | 'comparison';

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
          </nav>
        </div>
      </div>
    </header>
  );
};
