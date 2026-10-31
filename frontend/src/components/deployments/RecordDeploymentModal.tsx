import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { DeploymentEnvironment, RecordDeploymentRequest, Release } from '../../types';
import { api, ApiError } from '../../api/client';
import { AlertBanner } from '../common/AlertBanner';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface RecordDeploymentModalProps {
  isOpen: boolean;
  applicationId: number;
  applicationName: string;
  defaultEnvironment?: DeploymentEnvironment;
  releases: Release[];
  onClose: () => void;
  onRecorded: () => void;
}

export const RecordDeploymentModal: React.FC<RecordDeploymentModalProps> = ({
  isOpen,
  applicationId,
  applicationName,
  defaultEnvironment = 'STAGING',
  releases,
  onClose,
  onRecorded,
}) => {
  const { user } = useAuth();
  const [environment, setEnvironment] = useState<DeploymentEnvironment>(defaultEnvironment);
  const [releaseId, setReleaseId] = useState<number | ''>(
    releases.length > 0 ? releases[0].id : ''
  );
  const [deployedAt, setDeployedAt] = useState(new Date().toISOString().slice(0, 16));
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!releaseId) {
      setError('Release is required.');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const payload: RecordDeploymentRequest = {
        releaseId: Number(releaseId),
        environment,
        deployedBy: user?.username || undefined,
        deployedAt: new Date(deployedAt).toISOString(),
        notes: notes.trim() ? notes.trim() : undefined,
      };

      await api.recordDeployment(applicationId, payload);
      onRecorded();
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(`[${err.status}] ${err.message}`);
      } else {
        setError('Failed to record deployment.');
      }
    } finally {
      setLoading(false);
    }
  };

  const selectedRelease = releases.find((r) => r.id === releaseId);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Record Deployment"
      description={`Record deployment event for ${applicationName} to ${environment}.`}
    >
      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}

      <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-900 flex items-start space-x-2 mb-4">
        <ShieldAlert className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
        <div>
          <strong>Deployment Boundary:</strong> This records deployment governance history and updates the current pointer; it does not execute pipeline operations. Production deployments enforce APPROVED policy status.
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Environment *
          </label>
          <select
            value={environment}
            onChange={(e) => setEnvironment(e.target.value as DeploymentEnvironment)}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none font-semibold"
          >
            <option value="DEVELOPMENT">Development</option>
            <option value="STAGING">Staging</option>
            <option value="PRODUCTION">Production (Requires Approved Policy)</option>
          </select>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Select Release *
          </label>
          {releases.length === 0 ? (
            <p className="text-xs text-rose-600 font-medium">No registered releases available for this application.</p>
          ) : (
            <select
              value={releaseId}
              onChange={(e) => setReleaseId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none font-mono"
            >
              {releases.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.version} • Status: {r.status} (ID #{r.id})
                </option>
              ))}
            </select>
          )}
          {environment === 'PRODUCTION' && selectedRelease && selectedRelease.status !== 'APPROVED' && (
            <p className="text-xs text-rose-600 font-medium mt-1">
              Warning: Production requires APPROVED status. Currently {selectedRelease.status}.
            </p>
          )}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Authenticated Deployer
          </label>
          <div className="px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium">
            {user?.displayName || user?.username} ({user?.username})
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Deployment Event Timestamp *
          </label>
          <input
            type="datetime-local"
            required
            value={deployedAt}
            onChange={(e) => setDeployedAt(e.target.value)}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none font-mono"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Notes (Optional)
          </label>
          <textarea
            rows={2}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Deployment context or ticket reference..."
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
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
            disabled={loading || !releaseId}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition disabled:opacity-50"
          >
            {loading ? 'Recording...' : 'Record Deployment'}
          </button>
        </div>
      </form>
    </Modal>
  );
};
