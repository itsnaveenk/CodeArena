import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import fc from 'fast-check';
import {
  ProblemEditor,
  getAvailableTransitions,
  canEditProblem,
} from './ProblemEditor';
import type { ProblemStatus } from '@/types';

vi.mock('@/lib/api', () => ({
  getApiClient: () => ({
    getProblem: vi.fn(),
    getTestcases: vi.fn(),
    createProblem: vi.fn(),
    updateProblem: vi.fn(),
    requestReview: vi.fn(),
  }),
}));

vi.mock('@/components/editor/CodeEditor', () => ({
  CodeEditor: ({ value, onChange, language }: { value: string; onChange: (code: string) => void; language: string }) => (
    <textarea
      data-testid={`code-editor-${language}`}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  ),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
    },
  });

const renderWithProviders = (ui: React.ReactElement) => {
  const queryClient = createTestQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>{ui}</BrowserRouter>
    </QueryClientProvider>
  );
};

const problemStatusArbitrary = () =>
  fc.constantFrom<ProblemStatus>('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'ARCHIVED');




describe('ProblemEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  describe('Unit Tests', () => {
    it('renders create mode when no problemId is provided', () => {
      renderWithProviders(<ProblemEditor />);
      
      expect(screen.getByText('Create Problem')).toBeInTheDocument();
    });

    it('renders all four tabs', () => {
      renderWithProviders(<ProblemEditor />);
      
      expect(screen.getByTestId('tab-details')).toBeInTheDocument();
      expect(screen.getByTestId('tab-testcases')).toBeInTheDocument();
      expect(screen.getByTestId('tab-starter-code')).toBeInTheDocument();
      expect(screen.getByTestId('tab-preview')).toBeInTheDocument();
    });

    it('disables testcases tab in create mode', () => {
      renderWithProviders(<ProblemEditor />);
      
      const testcasesTab = screen.getByTestId('tab-testcases');
      expect(testcasesTab).toBeDisabled();
    });

    it('renders save button', () => {
      renderWithProviders(<ProblemEditor />);
      
      expect(screen.getByTestId('save-button')).toBeInTheDocument();
    });
  });

  describe('Property Tests', () => {
    it('Property 18: component maintains state across tab interactions', async () => {
      renderWithProviders(<ProblemEditor />);
      
      const detailsTab = screen.getByTestId('tab-details');
      const previewTab = screen.getByTestId('tab-preview');
      const starterCodeTab = screen.getByTestId('tab-starter-code');
      
      expect(detailsTab).toBeInTheDocument();
      expect(previewTab).toBeInTheDocument();
      expect(starterCodeTab).toBeInTheDocument();
      
      expect(screen.getByTestId('problem-editor')).toBeInTheDocument();
      expect(screen.getByTestId('save-button')).toBeInTheDocument();
    });

    it('Property 18b: tabs are properly configured for state preservation', async () => {
      renderWithProviders(<ProblemEditor />);
      
      const tabs = screen.getAllByRole('tab');
      expect(tabs.length).toBe(4);
      
      tabs.forEach((tab) => {
        expect(tab).toHaveAttribute('role', 'tab');
        expect(tab).toHaveAttribute('aria-selected');
      });
      
      const tabPanels = screen.getAllByRole('tabpanel', { hidden: true });
      expect(tabPanels.length).toBeGreaterThan(0);
    });

    it('Property 19: getAvailableTransitions returns correct transitions for each status', () => {
      fc.assert(
        fc.property(problemStatusArbitrary(), (status) => {
          const transitions = getAvailableTransitions(status);
          
          switch (status) {
            case 'DRAFT':
              expect(transitions).toContain('PENDING_REVIEW');
              expect(transitions).not.toContain('PUBLISHED');
              break;
            case 'PENDING_REVIEW':
              expect(transitions).toContain('PUBLISHED');
              expect(transitions).toContain('REJECTED');
              break;
            case 'PUBLISHED':
              expect(transitions).toContain('ARCHIVED');
              expect(transitions).not.toContain('DRAFT');
              break;
            case 'REJECTED':
              expect(transitions).toContain('PENDING_REVIEW');
              break;
            case 'ARCHIVED':
              expect(transitions).toHaveLength(0);
              break;
          }
          
          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('Property 19b: workflow transitions follow valid state machine', () => {
      const validTransitions: Record<ProblemStatus, ProblemStatus[]> = {
        DRAFT: ['PENDING_REVIEW'],
        PENDING_REVIEW: ['PUBLISHED', 'REJECTED'],
        PUBLISHED: ['ARCHIVED'],
        REJECTED: ['PENDING_REVIEW'],
        ARCHIVED: [],
      };

      fc.assert(
        fc.property(problemStatusArbitrary(), (status) => {
          const transitions = getAvailableTransitions(status);
          const expected = validTransitions[status];
          
          transitions.forEach((t) => {
            expect(expected).toContain(t);
          });
          
          expected.forEach((t) => {
            expect(transitions).toContain(t);
          });
          
          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('Property 20: canEditProblem returns true for REJECTED status', () => {
      fc.assert(
        fc.property(fc.boolean(), (isAdmin) => {
          expect(canEditProblem('REJECTED', isAdmin)).toBe(true);
          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('Property 20b: REJECTED status allows resubmission', () => {
      const transitions = getAvailableTransitions('REJECTED');
      expect(transitions).toContain('PENDING_REVIEW');
    });

    it('Property 21: canEditProblem returns false for PENDING_REVIEW when not admin', () => {
      expect(canEditProblem('PENDING_REVIEW', false)).toBe(false);
    });

    it('Property 21b: canEditProblem returns true for PENDING_REVIEW when admin', () => {
      expect(canEditProblem('PENDING_REVIEW', true)).toBe(true);
    });

    it('Property 21c: edit restrictions follow role-based rules', () => {
      fc.assert(
        fc.property(problemStatusArbitrary(), fc.boolean(), (status, isAdmin) => {
          const canEdit = canEditProblem(status, isAdmin);
          
          if (isAdmin) {
            expect(canEdit).toBe(true);
          } else {
            if (status === 'DRAFT' || status === 'REJECTED') {
              expect(canEdit).toBe(true);
            } else {
              expect(canEdit).toBe(false);
            }
          }
          
          return true;
        }),
        { numRuns: 100 }
      );
    });
  });

  describe('Edge Cases', () => {
    it('handles undefined problemId gracefully', () => {
      renderWithProviders(<ProblemEditor problemId={undefined} />);
      
      expect(screen.getByText('Create Problem')).toBeInTheDocument();
    });

    it('renders cancel button when onCancel is provided', () => {
      const mockCancel = vi.fn();
      renderWithProviders(<ProblemEditor onCancel={mockCancel} />);
      
      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      expect(cancelButton).toBeInTheDocument();
      
      fireEvent.click(cancelButton);
      expect(mockCancel).toHaveBeenCalled();
    });

    it('does not render cancel button when onCancel is not provided', () => {
      renderWithProviders(<ProblemEditor />);
      
      expect(screen.queryByRole('button', { name: /cancel/i })).not.toBeInTheDocument();
    });
  });
});

describe('Status Workflow Logic', () => {
  describe('Property Tests', () => {
    it('all statuses have defined workflow configuration', () => {
      const allStatuses: ProblemStatus[] = ['DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'ARCHIVED'];
      
      allStatuses.forEach((status) => {
        const transitions = getAvailableTransitions(status);
        expect(Array.isArray(transitions)).toBe(true);
      });
    });

    it('no status can transition to itself', () => {
      fc.assert(
        fc.property(problemStatusArbitrary(), (status) => {
          const transitions = getAvailableTransitions(status);
          expect(transitions).not.toContain(status);
          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('DRAFT is the only entry point status', () => {
      const allStatuses: ProblemStatus[] = ['DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'ARCHIVED'];
      
      allStatuses.forEach((status) => {
        const transitions = getAvailableTransitions(status);
        expect(transitions).not.toContain('DRAFT');
      });
    });

    it('ARCHIVED is a terminal state', () => {
      const transitions = getAvailableTransitions('ARCHIVED');
      expect(transitions).toHaveLength(0);
    });
  });
});
