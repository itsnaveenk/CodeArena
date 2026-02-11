import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('@/lib/api', () => {
  const mockGetTestcases = vi.fn();
  const mockAddTestcase = vi.fn();
  const mockUpdateTestcase = vi.fn();
  const mockDeleteTestcase = vi.fn();
  
  return {
    getApiClient: () => ({
      getTestcases: mockGetTestcases,
      addTestcase: mockAddTestcase,
      updateTestcase: mockUpdateTestcase,
      deleteTestcase: mockDeleteTestcase,
    }),
    __mocks: {
      mockGetTestcases,
      mockAddTestcase,
      mockUpdateTestcase,
      mockDeleteTestcase,
    },
  };
});

vi.mock('sonner', () => {
  const mockToastSuccess = vi.fn();
  const mockToastError = vi.fn();
  return {
    toast: {
      success: mockToastSuccess,
      error: mockToastError,
    },
    __mocks: {
      mockToastSuccess,
      mockToastError,
    },
  };
});

import { TestcaseManager } from './TestcaseManager';
import * as apiModule from '@/lib/api';
import * as sonnerModule from 'sonner';

const { mockGetTestcases, mockAddTestcase, mockUpdateTestcase, mockDeleteTestcase } = 
  (apiModule as unknown as { __mocks: { mockGetTestcases: ReturnType<typeof vi.fn>; mockAddTestcase: ReturnType<typeof vi.fn>; mockUpdateTestcase: ReturnType<typeof vi.fn>; mockDeleteTestcase: ReturnType<typeof vi.fn> } }).__mocks;
const { mockToastSuccess, mockToastError } = 
  (sonnerModule as unknown as { __mocks: { mockToastSuccess: ReturnType<typeof vi.fn>; mockToastError: ReturnType<typeof vi.fn> } }).__mocks;

const createQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

const renderWithProviders = (ui: React.ReactElement) => {
  const queryClient = createQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
  );
};

describe('TestcaseManager', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetTestcases.mockReset();
    mockAddTestcase.mockReset();
    mockUpdateTestcase.mockReset();
    mockDeleteTestcase.mockReset();
    mockToastSuccess.mockReset();
    mockToastError.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  describe('Unit Tests', () => {
    it('renders loading state initially', () => {
      mockGetTestcases.mockReturnValue(new Promise(() => {}));
      
      renderWithProviders(<TestcaseManager problemId={1} />);
      
      expect(document.querySelector('.animate-pulse')).toBeTruthy();
    });

    it('renders empty state when no testcases', async () => {
      mockGetTestcases.mockResolvedValue([]);
      
      renderWithProviders(<TestcaseManager problemId={1} />);
      
      await waitFor(() => {
        expect(screen.getByText(/no test cases yet/i)).toBeInTheDocument();
      });
    });

    it('renders Add Testcase button', async () => {
      mockGetTestcases.mockResolvedValue([]);
      
      renderWithProviders(<TestcaseManager problemId={1} />);
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add testcase/i })).toBeInTheDocument();
      });
    });
  });

  describe('Property Tests', () => {
    it('Property 6: displays all required fields for each testcase', async () => {
      const testcases = [
        { id: 1, input: 'input1', expectedOutput: 'output1', isHidden: false, problemId: 1 },
        { id: 2, input: 'input2', expectedOutput: 'output2', isHidden: true, problemId: 1 },
      ];
      mockGetTestcases.mockResolvedValue(testcases);
      
      const { container } = renderWithProviders(<TestcaseManager problemId={1} />);
      
      await waitFor(() => {
        const rows = container.querySelectorAll('tbody tr');
        expect(rows.length).toBe(2);
      });
      
      const rows = container.querySelectorAll('tbody tr');
      
      testcases.forEach((tc, index) => {
        const row = rows[index];
        
        expect(row.textContent).toContain(tc.input);
        
        expect(row.textContent).toContain(tc.expectedOutput);
        
        if (tc.isHidden) {
          expect(row.textContent).toContain('Hidden');
        } else {
          expect(row.textContent).toContain('Visible');
        }
        
        expect(row.querySelector(`[data-testid="edit-testcase-${tc.id}"]`)).toBeTruthy();
        expect(row.querySelector(`[data-testid="delete-testcase-${tc.id}"]`)).toBeTruthy();
      });
    });

    it('Property 7: validates input and output are required for adding', async () => {
      mockGetTestcases.mockResolvedValue([]);
      renderWithProviders(<TestcaseManager problemId={1} />);
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add testcase/i })).toBeInTheDocument();
      });
      
      fireEvent.click(screen.getByRole('button', { name: /add testcase/i }));
      
      await waitFor(() => {
        expect(screen.getByText('Add Test Case')).toBeInTheDocument();
      });
      
      const submitButton = screen.getByRole('button', { name: /^add testcase$/i });
      fireEvent.click(submitButton);
      
      await waitFor(() => {
        expect(mockToastError).toHaveBeenCalled();
        const calls = mockToastError.mock.calls;
        const hasExpectedMessage = calls.some((call: unknown[]) => call[0] === 'Input and expected output are required');
        expect(hasExpectedMessage).toBe(true);
      });
      expect(mockAddTestcase).not.toHaveBeenCalled();
    });

    it('Property 8: edit form is pre-populated with existing values', async () => {
      const testcase = { id: 1, input: 'test input', expectedOutput: 'test output', isHidden: true, problemId: 1 };
      mockGetTestcases.mockResolvedValue([testcase]);
      
      renderWithProviders(<TestcaseManager problemId={1} />);
      
      await waitFor(() => {
        expect(screen.getByTestId(`edit-testcase-${testcase.id}`)).toBeInTheDocument();
      });
      
      const editButton = screen.getByTestId(`edit-testcase-${testcase.id}`);
      fireEvent.click(editButton);
      
      await waitFor(() => {
        expect(screen.getByText('Edit Test Case')).toBeInTheDocument();
      });
      
      const inputField = screen.getByPlaceholderText(/enter test input/i) as HTMLTextAreaElement;
      const outputField = screen.getByPlaceholderText(/enter expected output/i) as HTMLTextAreaElement;
      const hiddenCheckbox = screen.getByLabelText(/hidden/i) as HTMLInputElement;
      
      expect(inputField.value).toBe(testcase.input);
      expect(outputField.value).toBe(testcase.expectedOutput);
      expect(hiddenCheckbox.checked).toBe(testcase.isHidden);
    });

    it('Property 9: delete shows confirmation dialog', async () => {
      const testcase = { id: 1, input: 'test', expectedOutput: 'output', isHidden: false, problemId: 1 };
      mockGetTestcases.mockResolvedValue([testcase]);
      
      renderWithProviders(<TestcaseManager problemId={1} />);
      
      await waitFor(() => {
        expect(screen.getByTestId(`delete-testcase-${testcase.id}`)).toBeInTheDocument();
      });
      
      const deleteButton = screen.getByTestId(`delete-testcase-${testcase.id}`);
      fireEvent.click(deleteButton);
      
      await waitFor(() => {
        expect(screen.getByText('Delete Test Case')).toBeInTheDocument();
        expect(screen.getByText(/are you sure you want to delete/i)).toBeInTheDocument();
      });
    });

    it('Property 10: hidden testcases display visual indicator', async () => {
      const testcases = [
        { id: 1, input: 'test1', expectedOutput: 'output1', isHidden: true, problemId: 1 },
        { id: 2, input: 'test2', expectedOutput: 'output2', isHidden: false, problemId: 1 },
      ];
      mockGetTestcases.mockResolvedValue(testcases);
      
      const { container } = renderWithProviders(<TestcaseManager problemId={1} />);
      
      await waitFor(() => {
        const rows = container.querySelectorAll('tbody tr');
        expect(rows.length).toBe(2);
      });
      
      const rows = container.querySelectorAll('tbody tr');
      
      const hiddenIndicator = rows[0].querySelector('[data-testid="hidden-indicator"]');
      expect(hiddenIndicator).toBeTruthy();
      expect(rows[0].textContent).toContain('Hidden');
      
      const visibleIndicator = rows[1].querySelector('[data-testid="hidden-indicator"]');
      expect(visibleIndicator).toBeFalsy();
      expect(rows[1].textContent).toContain('Visible');
    });

    it('Property 11: correctly tracks visible vs hidden testcase counts', async () => {
      const testcases = [
        { id: 1, input: 'test1', expectedOutput: 'output1', isHidden: true, problemId: 1 },
        { id: 2, input: 'test2', expectedOutput: 'output2', isHidden: false, problemId: 1 },
        { id: 3, input: 'test3', expectedOutput: 'output3', isHidden: true, problemId: 1 },
      ];
      mockGetTestcases.mockResolvedValue(testcases);
      
      const { container } = renderWithProviders(<TestcaseManager problemId={1} />);
      
      await waitFor(() => {
        const rows = container.querySelectorAll('tbody tr');
        expect(rows.length).toBe(3);
      });
      
      const hiddenBadges = container.querySelectorAll('[data-testid="hidden-indicator"]');
      expect(hiddenBadges.length).toBe(2); // 2 hidden testcases
      
      expect(screen.getByText(/Test Cases/)).toBeInTheDocument();
    });
  });
});
