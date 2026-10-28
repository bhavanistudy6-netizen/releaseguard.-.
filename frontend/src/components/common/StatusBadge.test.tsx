import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { StatusBadge } from './StatusBadge';

describe('StatusBadge', () => {
  it('renders release status badges correctly', () => {
    const { rerender } = render(<StatusBadge status="DRAFT" />);
    expect(screen.getByText('Draft')).toBeInTheDocument();

    rerender(<StatusBadge status="READY_FOR_REVIEW" />);
    expect(screen.getByText('Ready for Review')).toBeInTheDocument();

    rerender(<StatusBadge status="APPROVED" />);
    expect(screen.getByText('Approved')).toBeInTheDocument();

    rerender(<StatusBadge status="RETIRED" />);
    expect(screen.getByText('Retired')).toBeInTheDocument();
  });

  it('renders vulnerability severity correctly', () => {
    const { rerender } = render(<StatusBadge severity="CRITICAL" />);
    expect(screen.getByText('CRITICAL')).toBeInTheDocument();

    rerender(<StatusBadge severity="HIGH" />);
    expect(screen.getByText('HIGH')).toBeInTheDocument();

    rerender(<StatusBadge severity="UNKNOWN" />);
    expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
  });

  it('renders comparison change types correctly', () => {
    const { rerender } = render(<StatusBadge changeType="ADDED" />);
    expect(screen.getByText('+ ADDED')).toBeInTheDocument();

    rerender(<StatusBadge changeType="UNCERTAIN" />);
    expect(screen.getByText('UNCERTAIN')).toBeInTheDocument();
  });

  it('renders rollback deployment type badge', () => {
    render(<StatusBadge deploymentType="ROLLBACK" />);
    expect(screen.getByText('↺ ROLLBACK')).toBeInTheDocument();
  });
});
