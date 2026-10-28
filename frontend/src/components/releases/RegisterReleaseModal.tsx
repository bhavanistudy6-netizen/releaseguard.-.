import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { CreateReleaseRequest } from '../../types';
import { api, ApiError } from '../../api/client';
import { AlertBanner } from '../common/AlertBanner';

interface RegisterReleaseModalProps {
  isOpen: boolean;
  applicationId: number;
  applicationName: string;
  onClose: () => void;
  onCreated: () => void;
}

export const RegisterReleaseModal: React.FC<RegisterReleaseModalProps> = ({
  isOpen,
  applicationId,
  applicationName,
  onClose,
  onCreated,
}) => {
  const [version, setVersion] = useState('');
  const [gitCommitSha, setGitCommitSha] = useState('');
  const [dockerImage, setDockerImage] = useState('');
  const [imageDigest, setImageDigest] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const trimmedVersion = version.trim();
    const trimmedSha = gitCommitSha.trim();
    const trimmedDocker = dockerImage.trim();
    const trimmedDigest = imageDigest.trim();

    if (!trimmedVersion || !trimmedSha || !trimmedDocker) {
      setError('Please provide Version, Git Commit SHA, and Docker Image.');
      return;
    }

    // Validate SHA (40 or 64 hex chars)
    const shaPattern = /^[0-9a-fA-F]{40}$|^[0-9a-fA-F]{64}$/;
    if (!shaPattern.test(trimmedSha)) {
      setError('Git Commit SHA must be a 40 or 64 character hexadecimal hash.');
      return;
    }

    if (trimmedDigest && !/^sha256:[0-9a-fA-F]{64}$/.test(trimmedDigest)) {
      setError('Image digest must match format: sha256:<64 hex characters>');
      return;
    }

    try {
      setLoading(true);
      const payload: CreateReleaseRequest = {
        version: trimmedVersion,
        gitCommitSha: trimmedSha,
        dockerImage: trimmedDocker,
        imageDigest: trimmedDigest ? trimmedDigest : null,
      };

      await api.createRelease(applicationId, payload);
      setVersion('');
      setGitCommitSha('');
      setDockerImage('');
      setImageDigest('');
      onCreated();
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setError(`Release version '${trimmedVersion}' already exists for this application.`);
        } else {
          setError(`[${err.status}] ${err.message}`);
        }
      } else {
        setError('Failed to register release.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Register Release"
      description={`Register a new versioned release for ${applicationName}.`}
    >
      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Version *
          </label>
          <input
            type="text"
            required
            value={version}
            onChange={(e) => setVersion(e.target.value)}
            placeholder="e.g. v1.0.0"
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition font-mono"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Git Commit SHA *
          </label>
          <input
            type="text"
            required
            value={gitCommitSha}
            onChange={(e) => setGitCommitSha(e.target.value)}
            placeholder="40 or 64 character hexadecimal hash"
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition font-mono text-xs"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Docker Image *
          </label>
          <input
            type="text"
            required
            value={dockerImage}
            onChange={(e) => setDockerImage(e.target.value)}
            placeholder="e.g. college-planner:1.0.0"
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition font-mono"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Image Digest (Optional)
          </label>
          <input
            type="text"
            value={imageDigest}
            onChange={(e) => setImageDigest(e.target.value)}
            placeholder="sha256:<64 hex characters>"
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition font-mono text-xs"
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
            disabled={loading}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition disabled:opacity-50"
          >
            {loading ? 'Registering...' : 'Register Release'}
          </button>
        </div>
      </form>
    </Modal>
  );
};
