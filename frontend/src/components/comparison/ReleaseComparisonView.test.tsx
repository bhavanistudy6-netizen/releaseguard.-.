import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ReleaseComparisonView } from './ReleaseComparisonView';
import { Application, Release } from '../../types';

const mockApp: Application = {
  id: 1,
  name: 'College Planner',
  description: 'Test application',
  repositoryUrl: 'https://github.com/example/planner',
  ownerTeam: 'Planner Team',
  createdAt: '2026-10-10T10:00:00Z',
  updatedAt: '2026-10-10T10:00:00Z',
};

const mockReleases: Release[] = [
  {
    id: 1,
    applicationId: 1,
    version: 'v1.0.0',
    gitCommitSha: 'a'.repeat(40),
    dockerImage: 'planner:1.0.0',
    imageDigest: null,
    status: 'READY_FOR_REVIEW',
    createdAt: '2026-10-10T10:00:00Z',
    updatedAt: '2026-10-10T10:00:00Z',
    lockVersion: 1,
  },
  {
    id: 2,
    applicationId: 1,
    version: 'v1.1.0',
    gitCommitSha: 'b'.repeat(40),
    dockerImage: 'planner:1.1.0',
    imageDigest: null,
    status: 'DRAFT',
    createdAt: '2026-10-15T10:00:00Z',
    updatedAt: '2026-10-15T10:00:00Z',
    lockVersion: 0,
  },
];

vi.mock('../../api/client', () => ({
  api: {
    getReleases: vi.fn().mockResolvedValue({
      content: [
        {
          id: 1,
          applicationId: 1,
          version: 'v1.0.0',
          gitCommitSha: 'a'.repeat(40),
          dockerImage: 'planner:1.0.0',
          imageDigest: null,
          status: 'READY_FOR_REVIEW',
          createdAt: '2026-10-10T10:00:00Z',
          updatedAt: '2026-10-10T10:00:00Z',
          lockVersion: 1,
        },
        {
          id: 2,
          applicationId: 1,
          version: 'v1.1.0',
          gitCommitSha: 'b'.repeat(40),
          dockerImage: 'planner:1.1.0',
          imageDigest: null,
          status: 'DRAFT',
          createdAt: '2026-10-15T10:00:00Z',
          updatedAt: '2026-10-15T10:00:00Z',
          lockVersion: 0,
        },
      ],
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
    }),
    compareReleases: vi.fn().mockResolvedValue({
      applicationId: 1,
      fromReleaseId: 1,
      toReleaseId: 2,
      changes: [
        {
          identity: {
            type: 'maven',
            namespace: 'org.apache.commons',
            name: 'commons-lang3',
            subpath: null,
            qualifiers: {},
          },
          changeType: 'VERSION_CHANGED',
          before: { bomRef: 'ref-1', version: '3.12.0', purl: 'pkg:maven/org.apache.commons/commons-lang3@3.12.0' },
          after: { bomRef: 'ref-2', version: '3.14.0', purl: 'pkg:maven/org.apache.commons/commons-lang3@3.14.0' },
          reason: 'Version upgraded from 3.12.0 to 3.14.0',
        },
        {
          identity: {
            type: 'maven',
            namespace: null,
            name: 'demo-lib',
            subpath: null,
            qualifiers: {},
          },
          changeType: 'ADDED',
          before: null,
          after: { bomRef: 'ref-3', version: '1.0.0', purl: 'pkg:maven/demo-lib@1.0.0' },
          reason: 'Newly added dependency',
        },
      ],
    }),
  },
}));

describe('ReleaseComparisonView', () => {
  it('renders comparison controls and triggers comparison', async () => {
    render(
      <ReleaseComparisonView
        applications={[mockApp]}
        initialApplication={mockApp}
        initialReleases={mockReleases}
      />
    );

    expect(screen.getByText('Release Dependency Comparison')).toBeInTheDocument();

    await waitFor(() => {
      const selects = screen.getAllByRole('combobox');
      expect(selects.length).toBe(3);
    });

    const compareButton = screen.getByRole('button', { name: /Compare Dependencies/i });
    expect(compareButton).toBeInTheDocument();

    await waitFor(() => {
      expect(compareButton).not.toBeDisabled();
    });

    fireEvent.click(compareButton);

    await waitFor(() => {
      expect(screen.getByText(/commons-lang3/)).toBeInTheDocument();
      expect(screen.getByText(/demo-lib/)).toBeInTheDocument();
      expect(screen.getByText('VERSION CHANGED')).toBeInTheDocument();
      expect(screen.getByText('+ ADDED')).toBeInTheDocument();
    });
  });
});
