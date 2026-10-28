import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { CreateApplicationRequest } from '../../types';
import { api, ApiError } from '../../api/client';
import { AlertBanner } from '../common/AlertBanner';

interface RegisterApplicationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated: () => void;
}

export const RegisterApplicationModal: React.FC<RegisterApplicationModalProps> = ({
  isOpen,
  onClose,
  onCreated,
}) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [repositoryUrl, setRepositoryUrl] = useState('');
  const [ownerTeam, setOwnerTeam] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !repositoryUrl.trim() || !ownerTeam.trim()) {
      setError('Please provide Name, Repository URL, and Owner Team.');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const payload: CreateApplicationRequest = {
        name: name.trim(),
        description: description.trim() ? description.trim() : undefined,
        repositoryUrl: repositoryUrl.trim(),
        ownerTeam: ownerTeam.trim(),
      };
      await api.createApplication(payload);
      setName('');
      setDescription('');
      setRepositoryUrl('');
      setOwnerTeam('');
      onCreated();
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(`[${err.status}] ${err.message}`);
      } else {
        setError('Failed to create application.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Register Application"
      description="Register a new software application and assign an owner team."
    >
      {error && <AlertBanner type="error" message={error} onClose={() => setError(null)} />}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="app-name" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Application Name *
          </label>
          <input
            id="app-name"
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. College Planner"
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition"
          />
        </div>

        <div>
          <label htmlFor="app-owner" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Owner Team *
          </label>
          <input
            id="app-owner"
            type="text"
            required
            value={ownerTeam}
            onChange={(e) => setOwnerTeam(e.target.value)}
            placeholder="e.g. Planner Team"
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition"
          />
        </div>

        <div>
          <label htmlFor="app-repo" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Repository URL *
          </label>
          <input
            id="app-repo"
            type="url"
            required
            value={repositoryUrl}
            onChange={(e) => setRepositoryUrl(e.target.value)}
            placeholder="https://github.com/example/repo"
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition"
          />
        </div>

        <div>
          <label htmlFor="app-desc" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
            Description (Optional)
          </label>
          <textarea
            id="app-desc"
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Brief overview of application purpose..."
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition"
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
            {loading ? 'Registering...' : 'Register Application'}
          </button>
        </div>
      </form>
    </Modal>
  );
};
