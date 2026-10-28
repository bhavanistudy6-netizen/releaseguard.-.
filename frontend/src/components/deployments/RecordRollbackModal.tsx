import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { Deployment, DeploymentEnvironment, RecordRollbackRequest } from '../../types';
import { api, ApiError } from '../../api/client';
import { AlertBanner } from '../common/AlertBanner';
import { AlertTriangle, RotateCcw } from 'lucide-react';

interface RecordRollbackModalProps {
  isOpen: boolean;
  applicationId: number;
  applicationName: string;
  environment: DeploymentEnvironment;
  currentDeployment: Deployment;
  historyDeployments: Deployment[];
  onClose: () => void;
  onRecorded: () => void;
}

export const RecordRollbackModal: React.FC<RecordRollbackModalProps> = ({
  isOpen,
  applicationId,
  applicationName,
  environment,
  currentDeployment,
  historyDeployments,
  onClose,
  onRecorded,
}) => {
  // Candidate targets are previous deployments in the same application & environment
  const candidates = historyDeployments.filter((d) => d.id !== currentDeployment.id);

  const [targetDeploymentId, setTargetDeploymentId] = useState<number | ''>(
    candidates.length > 0 ? candidates[0].id : ''
  );
  const [deployedBy, setDeployedBy] = useState('');
  const [reason, setReason] = useState('');
  const [deployedAt, setDeployedAt] = useState(new Date().toISOString().slice(0, 16));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetDeploymentId || !deployedBy.trim() || !reason.trim()) {
      setError('Target Deployment, Deployed By, and Reason are required.');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const payload: RecordRollbackRequest = {
        environment,
        expectedCurrentDeploymentId: currentDeployment.id,
        targetDeploymentId: Number(targetDeploymentId),
        deployedBy: deployedBy.trim(),
        deployedAt: new Date(deployedAt).toISOString(),
        reason: reason.trim(),
      };

      await api.recordRollback(applicationId, payload);
      onRecorded();
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setError(
            `Conflict (409): The current deployment in ${environment} has changed. Please refresh and review before rolling back.`
          );
        } else {
          setError(`[${err.status}] ${err.message}`);
        }
      } else {
        setError('Failed to record rollback.');
      }
    } finally {
      setLoading(false);
    }
  };

  const selectedTarget = candidates.find((c) => c.id === targetDeploymentId);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Record Rollback in ${environment}`}
      description={`Restore an earlier release for ${applicationName} in ${environment}.`}
    >
      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}

      <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-900 flex items-start space-x-2 mb-4">
        <AlertTriangle className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
        <div>
          <strong>Atomic Rollback Protection:</strong> Uses application-level write lock. Expected current pointer is{' '}
          <span className="font-mono font-bold">#{currentDeployment.id}</span> ({currentDeployment.releaseVersion}).
          If production, the restored release's approval policy and scan evidence will be re-evaluated.
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Current Deployment (Expected Pointer)
          </label>
          <div className="px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-xs font-mono text-slate-700">
            ID #{currentDeployment.id} • Release: {currentDeployment.releaseVersion} (Deployed by{' '}
            {currentDeployment.deployedBy})
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Target Deployment to Restore *
          </label>
          {candidates.length === 0 ? (
            <p className="text-xs text-rose-600 font-medium">
              No earlier deployments exist in {environment} to roll back to.
            </p>
          ) : (
            <select
              required
              value={targetDeploymentId}
              onChange={(e) => setTargetDeploymentId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none font-mono"
            >
              {candidates.map((c) => (
                <option key={c.id} value={c.id}>
                  Restore ID #{c.id} • Release {c.releaseVersion} (from {new Date(c.deployedAt).toLocaleDateString()})
                </option>
              ))}
            </select>
          )}
          {selectedTarget && (
            <p className="text-xs text-slate-500 mt-1">
              Will restore release: <strong className="text-slate-800 font-mono">{selectedTarget.releaseVersion}</strong>
            </p>
          )}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Rollback Reason *
          </label>
          <textarea
            required
            rows={2}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="e.g. Critical issue observed in current release; restoring previous release."
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Deployed By (Actor) *
          </label>
          <input
            type="text"
            required
            value={deployedBy}
            onChange={(e) => setDeployedBy(e.target.value)}
            placeholder="e.g. Operations Team"
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Timestamp *
          </label>
          <input
            type="datetime-local"
            required
            value={deployedAt}
            onChange={(e) => setDeployedAt(e.target.value)}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none font-mono"
          />
        </div>

        <div className="flex justify-end space-x-3 pt-3 border-t border-slate-100">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading || !targetDeploymentId}
            className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-sm font-semibold shadow-sm transition disabled:opacity-50 inline-flex items-center"
          >
            <RotateCcw className="w-3.5 h-3.5 mr-1.5" />
            {loading ? 'Recording Rollback...' : 'Execute Rollback'}
          </button>
        </div>
      </form>
    </Modal>
  );
};
