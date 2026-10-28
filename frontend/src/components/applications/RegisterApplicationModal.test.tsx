import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { RegisterApplicationModal } from './RegisterApplicationModal';

describe('RegisterApplicationModal', () => {
  it('renders input fields and validates required values', () => {
    const onClose = vi.fn();
    const onCreated = vi.fn();

    render(
      <RegisterApplicationModal
        isOpen={true}
        onClose={onClose}
        onCreated={onCreated}
      />
    );

    expect(screen.getByRole('heading', { name: 'Register Application' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Application Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Owner Team/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Repository URL/i)).toBeInTheDocument();

    const submitBtn = screen.getByRole('button', { name: /Register Application/i });
    expect(submitBtn).toBeInTheDocument();

    const cancelBtn = screen.getByRole('button', { name: /Cancel/i });
    fireEvent.click(cancelBtn);
    expect(onClose).toHaveBeenCalled();
  });
});
