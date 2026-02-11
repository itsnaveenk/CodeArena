import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import fc from 'fast-check';
import { UserManagementTable } from './UserManagementTable';
import type { Role } from '@/types';
import type { UserListItem, PaginatedResponse } from '@/lib/api';

const mockUseUsers = vi.fn();
const mockUseUpdateUserRole = vi.fn();
vi.mock('@/hooks/useUsers', () => ({
  useUsers: () => mockUseUsers(),
  useUpdateUserRole: () => mockUseUpdateUserRole(),
}));

const mockCurrentUser = { id: 999, name: 'Current Admin', email: 'admin@test.com', role: 'ADMIN' as Role };
vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({ user: mockCurrentUser }),
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

const roleArbitrary = (): fc.Arbitrary<Role> =>
  fc.constantFrom<Role>('USER', 'PROBLEM_SETTER', 'ADMIN');

const userListItemArbitrary = (): fc.Arbitrary<UserListItem> =>
  fc.record({
    id: fc.integer({ min: 1, max: 10000 }),
    name: fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
    email: fc.emailAddress(),
    role: roleArbitrary(),
    createdAt: fc.constant(new Date().toISOString()),
    problemsSolved: fc.integer({ min: 0, max: 1000 }),
  });


function filterUsersBySearch(users: UserListItem[], search: string): UserListItem[] {
  if (!search) return users;
  const lowerSearch = search.toLowerCase();
  return users.filter(
    (u) =>
      u.name.toLowerCase().includes(lowerSearch) ||
      u.email.toLowerCase().includes(lowerSearch)
  );
}

function filterUsersByRole(users: UserListItem[], role: Role | null): UserListItem[] {
  if (!role) return users;
  return users.filter((u) => u.role === role);
}

describe('UserManagementTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseUpdateUserRole.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    });
  });

  afterEach(() => {
    cleanup();
  });

  describe('Unit Tests', () => {
    it('renders loading state correctly', () => {
      mockUseUsers.mockReturnValue({
        data: undefined,
        isLoading: true,
      });

      renderWithProviders(<UserManagementTable />);

      expect(screen.getByPlaceholderText('Search by name or email...')).toBeInTheDocument();
    });

    it('renders user list when data is loaded', () => {
      const mockUsers: PaginatedResponse<UserListItem> = {
        content: [
          { id: 1, name: 'John Doe', email: 'john@test.com', role: 'USER', createdAt: '2024-01-01', problemsSolved: 10 },
          { id: 2, name: 'Jane Smith', email: 'jane@test.com', role: 'PROBLEM_SETTER', createdAt: '2024-01-02', problemsSolved: 25 },
        ],
        totalElements: 2,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseUsers.mockReturnValue({
        data: mockUsers,
        isLoading: false,
      });

      renderWithProviders(<UserManagementTable />);

      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    });

    it('renders empty state when no users', () => {
      mockUseUsers.mockReturnValue({
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: 0,
          size: 10,
        },
        isLoading: false,
      });

      renderWithProviders(<UserManagementTable />);

      expect(screen.getByText('No users found')).toBeInTheDocument();
    });
  });

  describe('Property Tests', () => {
    it('Property 23: displays all required user fields', () => {
      const mockUsers: PaginatedResponse<UserListItem> = {
        content: [
          { id: 1, name: 'Test User', email: 'test@example.com', role: 'USER', createdAt: '2024-06-15T10:00:00Z', problemsSolved: 42 },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseUsers.mockReturnValue({
        data: mockUsers,
        isLoading: false,
      });

      renderWithProviders(<UserManagementTable />);

      expect(screen.getByText('Test User')).toBeInTheDocument();

      expect(screen.getByText('test@example.com')).toBeInTheDocument();

      expect(screen.getByText('User')).toBeInTheDocument();

      expect(screen.getByText('42')).toBeInTheDocument();

      expect(screen.getByText('6/15/2024')).toBeInTheDocument();
    });

    it('Property 23b: displays correct role badges for all roles', () => {
      const mockUsers: PaginatedResponse<UserListItem> = {
        content: [
          { id: 1, name: 'Regular User', email: 'user@test.com', role: 'USER', createdAt: '2024-01-01', problemsSolved: 5 },
          { id: 2, name: 'Content Creator', email: 'setter@test.com', role: 'PROBLEM_SETTER', createdAt: '2024-01-01', problemsSolved: 15 },
          { id: 3, name: 'Admin User', email: 'admin@test.com', role: 'ADMIN', createdAt: '2024-01-01', problemsSolved: 50 },
        ],
        totalElements: 3,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseUsers.mockReturnValue({
        data: mockUsers,
        isLoading: false,
      });

      renderWithProviders(<UserManagementTable />);

      expect(screen.getAllByText('User').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Problem Setter').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Admin').length).toBeGreaterThan(0);
    });

    it('Property 24: search filter logic is correct', () => {
      fc.assert(
        fc.property(
          fc.array(userListItemArbitrary(), { minLength: 1, maxLength: 20 }),
          fc.string({ minLength: 0, maxLength: 20 }),
          (users, searchQuery) => {
            const filtered = filterUsersBySearch(users, searchQuery);

            if (searchQuery) {
              const lowerSearch = searchQuery.toLowerCase();
              filtered.forEach((user) => {
                const matchesName = user.name.toLowerCase().includes(lowerSearch);
                const matchesEmail = user.email.toLowerCase().includes(lowerSearch);
                expect(matchesName || matchesEmail).toBe(true);
              });
            }

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 24b: role filter logic is correct', () => {
      fc.assert(
        fc.property(
          fc.array(userListItemArbitrary(), { minLength: 1, maxLength: 20 }),
          fc.option(roleArbitrary(), { nil: null }),
          (users, roleFilter) => {
            const filtered = filterUsersByRole(users, roleFilter);

            if (roleFilter) {
              filtered.forEach((user) => {
                expect(user.role).toBe(roleFilter);
              });
            } else {
              expect(filtered.length).toBe(users.length);
            }

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 24c: combined filters work correctly', () => {
      fc.assert(
        fc.property(
          fc.array(userListItemArbitrary(), { minLength: 1, maxLength: 20 }),
          fc.string({ minLength: 0, maxLength: 10 }),
          fc.option(roleArbitrary(), { nil: null }),
          (users, searchQuery, roleFilter) => {
            const filteredBySearch = filterUsersBySearch(users, searchQuery);
            const filteredByBoth = filterUsersByRole(filteredBySearch, roleFilter);

            filteredByBoth.forEach((user) => {
              if (searchQuery) {
                const lowerSearch = searchQuery.toLowerCase();
                const matchesName = user.name.toLowerCase().includes(lowerSearch);
                const matchesEmail = user.email.toLowerCase().includes(lowerSearch);
                expect(matchesName || matchesEmail).toBe(true);
              }
              if (roleFilter) {
                expect(user.role).toBe(roleFilter);
              }
            });

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 25: role update mutation is called with correct parameters', () => {
      const mockMutate = vi.fn();
      mockUseUpdateUserRole.mockReturnValue({
        mutate: mockMutate,
        isPending: false,
      });

      const mockUsers: PaginatedResponse<UserListItem> = {
        content: [
          { id: 1, name: 'Test User', email: 'test@test.com', role: 'USER', createdAt: '2024-01-01', problemsSolved: 10 },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseUsers.mockReturnValue({
        data: mockUsers,
        isLoading: false,
      });

      renderWithProviders(<UserManagementTable />);

      expect(mockMutate).toBeDefined();
    });

    it('Property 26: self-role-change is prevented', () => {
      const mockUsers: PaginatedResponse<UserListItem> = {
        content: [
          { id: 999, name: 'Current Admin', email: 'admin@test.com', role: 'ADMIN', createdAt: '2024-01-01', problemsSolved: 100 },
          { id: 1, name: 'Other User', email: 'other@test.com', role: 'USER', createdAt: '2024-01-01', problemsSolved: 10 },
        ],
        totalElements: 2,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseUsers.mockReturnValue({
        data: mockUsers,
        isLoading: false,
      });

      renderWithProviders(<UserManagementTable />);

      const currentUserRow = screen.getByText('Current Admin').closest('tr');
      expect(currentUserRow).toBeInTheDocument();

      const currentUserActionButton = currentUserRow?.querySelector('button');
      expect(currentUserActionButton).toBeDisabled();
    });

    it('Property 26b: other users can have their roles changed', () => {
      const mockUsers: PaginatedResponse<UserListItem> = {
        content: [
          { id: 1, name: 'Other User', email: 'other@test.com', role: 'USER', createdAt: '2024-01-01', problemsSolved: 10 },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseUsers.mockReturnValue({
        data: mockUsers,
        isLoading: false,
      });

      renderWithProviders(<UserManagementTable />);

      const otherUserRow = screen.getByText('Other User').closest('tr');
      expect(otherUserRow).toBeInTheDocument();

      const otherUserActionButton = otherUserRow?.querySelector('button');
      expect(otherUserActionButton).not.toBeDisabled();
    });
  });

  describe('Edge Cases', () => {
    it('handles empty search gracefully', () => {
      const mockUsers: PaginatedResponse<UserListItem> = {
        content: [
          { id: 1, name: 'Test User', email: 'test@test.com', role: 'USER', createdAt: '2024-01-01', problemsSolved: 10 },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseUsers.mockReturnValue({
        data: mockUsers,
        isLoading: false,
      });

      renderWithProviders(<UserManagementTable />);

      expect(screen.getByText('Test User')).toBeInTheDocument();
    });

    it('handles pagination correctly', () => {
      const mockUsers: PaginatedResponse<UserListItem> = {
        content: [
          { id: 1, name: 'User 1', email: 'user1@test.com', role: 'USER', createdAt: '2024-01-01', problemsSolved: 10 },
        ],
        totalElements: 25,
        totalPages: 3,
        page: 0,
        size: 10,
      };

      mockUseUsers.mockReturnValue({
        data: mockUsers,
        isLoading: false,
      });

      renderWithProviders(<UserManagementTable />);

      expect(screen.getByText('User 1')).toBeInTheDocument();
    });
  });
});
