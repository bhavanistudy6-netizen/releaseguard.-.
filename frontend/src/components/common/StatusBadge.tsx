import React from 'react';
import {
  ChangeType,
  DeploymentEnvironment,
  DeploymentType,
  ReleaseStatus,
  VulnerabilitySeverity,
} from '../../types';

interface StatusBadgeProps {
  status?: ReleaseStatus;
  severity?: VulnerabilitySeverity;
  environment?: DeploymentEnvironment;
  changeType?: ChangeType;
  deploymentType?: DeploymentType;
  text?: string;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  severity,
  environment,
  changeType,
  deploymentType,
  text,
  size = 'md',
}) => {
  const sizeClass = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs font-semibold';

  if (status) {
    const config: Record<ReleaseStatus, { bg: string; text: string; label: string }> = {
      DRAFT: { bg: 'bg-slate-100 text-slate-700 border-slate-200', text: 'text-slate-700', label: 'Draft' },
      READY_FOR_REVIEW: { bg: 'bg-amber-50 text-amber-800 border-amber-300', text: 'text-amber-800', label: 'Ready for Review' },
      APPROVED: { bg: 'bg-emerald-50 text-emerald-800 border-emerald-300', text: 'text-emerald-800', label: 'Approved' },
      RETIRED: { bg: 'bg-rose-50 text-rose-800 border-rose-300', text: 'text-rose-800', label: 'Retired' },
    };
    const c = config[status] || { bg: 'bg-slate-100 text-slate-700 border-slate-200', label: status };
    return (
      <span className={`inline-flex items-center rounded-full border ${c.bg} ${sizeClass}`}>
        <span className="w-1.5 h-1.5 rounded-full mr-1.5 bg-current opacity-70"></span>
        {c.label}
      </span>
    );
  }

  if (severity) {
    const config: Record<VulnerabilitySeverity, { bg: string; label: string }> = {
      CRITICAL: { bg: 'bg-purple-100 text-purple-900 border-purple-300 font-bold', label: 'CRITICAL' },
      HIGH: { bg: 'bg-rose-100 text-rose-900 border-rose-300 font-semibold', label: 'HIGH' },
      MEDIUM: { bg: 'bg-amber-100 text-amber-900 border-amber-300', label: 'MEDIUM' },
      LOW: { bg: 'bg-blue-100 text-blue-900 border-blue-300', label: 'LOW' },
      INFORMATIONAL: { bg: 'bg-slate-100 text-slate-800 border-slate-200', label: 'INFO' },
      UNKNOWN: { bg: 'bg-zinc-200 text-zinc-900 border-zinc-400', label: 'UNKNOWN' },
    };
    const c = config[severity] || { bg: 'bg-slate-100 text-slate-800 border-slate-200', label: severity };
    return (
      <span className={`inline-flex items-center rounded-md border ${c.bg} ${sizeClass} tracking-wide`}>
        {c.label}
      </span>
    );
  }

  if (environment) {
    const config: Record<DeploymentEnvironment, { bg: string; label: string }> = {
      DEVELOPMENT: { bg: 'bg-sky-50 text-sky-800 border-sky-300', label: 'Development' },
      STAGING: { bg: 'bg-indigo-50 text-indigo-800 border-indigo-300', label: 'Staging' },
      PRODUCTION: { bg: 'bg-purple-50 text-purple-900 border-purple-300 font-bold', label: 'Production' },
    };
    const c = config[environment];
    return (
      <span className={`inline-flex items-center rounded-md border ${c.bg} ${sizeClass}`}>
        {c.label}
      </span>
    );
  }

  if (changeType) {
    const config: Record<ChangeType, { bg: string; label: string }> = {
      ADDED: { bg: 'bg-emerald-100 text-emerald-900 border-emerald-300 font-medium', label: '+ ADDED' },
      REMOVED: { bg: 'bg-rose-100 text-rose-900 border-rose-300 font-medium', label: '- REMOVED' },
      VERSION_CHANGED: { bg: 'bg-blue-100 text-blue-900 border-blue-300 font-medium', label: 'VERSION CHANGED' },
      UNCHANGED: { bg: 'bg-slate-100 text-slate-700 border-slate-200', label: 'UNCHANGED' },
      UNCERTAIN: { bg: 'bg-amber-100 text-amber-900 border-amber-400 font-semibold', label: 'UNCERTAIN' },
    };
    const c = config[changeType];
    return (
      <span className={`inline-flex items-center rounded-md border ${c.bg} ${sizeClass}`}>
        {c.label}
      </span>
    );
  }

  if (deploymentType) {
    const isRollback = deploymentType === 'ROLLBACK';
    return (
      <span
        className={`inline-flex items-center rounded-md border ${
          isRollback
            ? 'bg-amber-100 text-amber-900 border-amber-300'
            : 'bg-emerald-50 text-emerald-800 border-emerald-300'
        } ${sizeClass}`}
      >
        {isRollback ? '↺ ROLLBACK' : 'DEPLOY'}
      </span>
    );
  }

  return (
    <span className={`inline-flex items-center rounded-md border bg-slate-100 text-slate-800 border-slate-200 ${sizeClass}`}>
      {text}
    </span>
  );
};
