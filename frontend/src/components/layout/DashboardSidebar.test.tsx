import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import fc from 'fast-check';
import { DashboardSidebar, getNavItemsForRole } from './DashboardSidebar';
import { useAuthStore } from '@/stores/authStore';
import type { Role, User } from '@/types';

vi.mock('@/stores/authStore', () => ({
  useAuthStore: vi.fn(),
}));

const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

const createMockUser = (role: Role): User => ({
  id: 1,
  name: 'Test User',
  email: 'test@example.com',
  role,
  createdAt: new Date().toISOString(),
});

const roleArbitrary = fc.constantFrom<Role>('USER', 'PROBLEM_SETTER', 'ADMIN');

describe('DashboardSidebar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    cleanup();
  });

  describe('Property 33: Role-Based Navigation Items', () => {
    it('should display correct navigation items for PROBLEM_SETTER role', () => {
      fc.assert(
        fc.property(fc.constant('PROBLEM_SETTER' as Role), (role) => {
          const items = getNavItemsForRole(role);
          const labels = items.map((i) => i.label);

          expect(labels).toContain('My Problems');
          expect(labels).toContain('Create Problem');
          expect(labels).toContain('Dashboard');

          expect(labels).not.toContain('Admin Overview');
          expect(labels).not.toContain('Users');
          expect(labels).not.toContain('All Problems');
          expect(labels).not.toContain('Submissions');

          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('should display correct navigation items for ADMIN role', () => {
      fc.assert(
        fc.property(fc.constant('ADMIN' as Role), (role) => {
          const items = getNavItemsForRole(role);
          const labels = items.map((i) => i.label);

          expect(labels).toContain('Admin Overview');
          expect(labels).toContain('My Problems');
          expect(labels).toContain('Create Problem');
          expect(labels).toContain('Users');
          expect(labels).toContain('All Problems');
          expect(labels).toContain('Submissions');
          expect(labels).toContain('Dashboard');

          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('should display correct navigation items for USER role', () => {
      fc.assert(
        fc.property(fc.constant('USER' as Role), (role) => {
          const items = getNavItemsForRole(role);
          const labels = items.map((i) => i.label);

          expect(labels).toContain('Dashboard');

          expect(labels).not.toContain('My Problems');
          expect(labels).not.toContain('Create Problem');
          expect(labels).not.toContain('Admin Overview');
          expect(labels).not.toContain('Users');
          expect(labels).not.toContain('All Problems');
          expect(labels).not.toContain('Submissions');

          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('should return items only for roles that include the user role', () => {
      fc.assert(
        fc.property(roleArbitrary, (role) => {
          const items = getNavItemsForRole(role);

          return items.every((item) => item.roles.includes(role));
        }),
        { numRuns: 100 }
      );
    });
  });

  describe('Property 34: Sidebar User Info Display', () => {
    it('should display user name and role for any authenticated user', () => {
      fc.assert(
        fc.property(
          fc.record({
            name: fc.stringMatching(/^[a-zA-Z][a-zA-Z0-9]{1,48}$/),
            role: roleArbitrary,
          }),
          ({ name, role }) => {
            vi.clearAllMocks();
            cleanup();

            const user = { ...createMockUser(role), name };
            mockUseAuthStore.mockReturnValue({ user });

            const { unmount } = render(
              <MemoryRouter>
                <DashboardSidebar />
              </MemoryRouter>
            );

            expect(screen.getAllByText(name).length).toBeGreaterThan(0);

            const roleLabels: Record<Role, string> = {
              ADMIN: 'Admin',
              PROBLEM_SETTER: 'Problem Setter',
              USER: 'User',
            };
            expect(screen.getAllByText(roleLabels[role]).length).toBeGreaterThan(0);

            unmount();
            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('should not render when user is not authenticated', () => {
      mockUseAuthStore.mockReturnValue({ user: null });

      const { container } = render(
        <MemoryRouter>
          <DashboardSidebar />
        </MemoryRouter>
      );

      expect(container.firstChild).toBeNull();
    });
  });

  describe('Unit Tests', () => {
    it('renders navigation links for ADMIN user', () => {
      mockUseAuthStore.mockReturnValue({ user: createMockUser('ADMIN') });

      render(
        <MemoryRouter>
          <DashboardSidebar />
        </MemoryRouter>
      );

      expect(screen.getAllByText('Admin').length).toBeGreaterThan(0);
      expect(screen.getByText('Admin Overview')).toBeInTheDocument();
      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('All Problems')).toBeInTheDocument();
    });

    it('renders navigation links for PROBLEM_SETTER user', () => {
      mockUseAuthStore.mockReturnValue({ user: createMockUser('PROBLEM_SETTER') });

      render(
        <MemoryRouter>
          <DashboardSidebar />
        </MemoryRouter>
      );

      expect(screen.getByText('My Problems')).toBeInTheDocument();
      expect(screen.getByText('Create Problem')).toBeInTheDocument();
      expect(screen.queryByText('Users')).not.toBeInTheDocument();
    });

    it('highlights active link based on current path', () => {
      mockUseAuthStore.mockReturnValue({ user: createMockUser('ADMIN') });

      render(
        <MemoryRouter initialEntries={['/my-problems']}>
          <DashboardSidebar />
        </MemoryRouter>
      );

      const myProblemsLink = screen.getByText('My Problems').closest('a');
      expect(myProblemsLink).toHaveClass('bg-primary');
    });
  });
});
