import React from 'react';
import { ShieldAlert } from 'lucide-react';

export const GovernanceNoticeBanner: React.FC = () => {
  return (
    <div className="bg-slate-900 text-slate-300 text-xs py-2 px-4 border-b border-slate-800 flex items-center justify-between">
      <div className="flex items-center space-x-2">
        <ShieldAlert className="w-4 h-4 text-emerald-400 flex-shrink-0" />
        <span>
          <strong className="text-white">Governance Boundary:</strong> ReleaseGuard records audit evidence, approvals, and deployment events; it does not execute infrastructure changes or perform native scanner sweeps.
        </span>
      </div>
      <span className="hidden md:inline-block text-slate-400 font-mono text-[11px]">
        Policy: release-policy-v1
      </span>
    </div>
  );
};
