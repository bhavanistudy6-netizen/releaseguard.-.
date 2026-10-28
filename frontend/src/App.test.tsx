import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import App from './App';

// Mock API client
vi.mock('./api/client', () => ({
  api: {
    getApplications: vi.fn().mockResolvedValue({
      content: [
        {
          id: 1,
          name: 'College Planner',
          description: 'University planner tool',
          repositoryUrl: 'https://github.com/example/planner',
          ownerTeam: 'Planner Team',
          createdAt: '2026-10-10T10:00:00Z',
          updatedAt: '2026-10-10T10:00:00Z',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    }),
    getReleases: vi.fn().mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    }),
    getCurrentDeployment: vi.fn().mockResolvedValue(null),
    getDeploymentHistory: vi.fn().mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    }),
  },
}));

describe('App', () => {
  it('renders header, navigation and applications view', async () => {
    render(<App />);

    expect(screen.getByText('ReleaseGuard')).toBeInTheDocument();
    expect(screen.getByText('Governance Boundary:')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Applications/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Environments/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Compare/i })).toBeInTheDocument();

    await waitFor(() => {
      const elements = screen.getAllByText('College Planner');
      expect(elements.length).toBeGreaterThanOrEqual(1);
    });
  });
});
