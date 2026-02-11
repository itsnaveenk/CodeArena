import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as fc from 'fast-check';
import { render } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { RoleGuard } from './RoleGuard';
import { useAuthStore } from '@/stores/authStore';
import type { User, Role } from '@/types';

vi.mock('@/stores/authStore');
vi.mock('sonner', () => ({
  toast: {
    error: vi.fn(),
  },
}));

const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

const userArbitrary = (role: Role) => fc.record({
  id: fc.integer({ min: 1, max: 10000 }),
  name: fc.string({ minLength: 1, maxLength: 50 }),
  email: fc.emailAddress(),
  role: fc.constant(role),
  createdAt: fc.constant(new Date().toISOString()),
});

const roleArbitrary = fc.constantFrom<Role>('USER', 'PROBLEM_SETTER', 'ADMIN');

describe('Property 12: Role-Based Route Access Control', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderWithRouter = (allowedRoles: Role[], initialPath = '/protected') => {
    return render(
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route element={<RoleGuard allowedRoles={allowedRoles} />}>
            <Route path="/protected" element={<div data-testid="protected-content">Protected</div>} />
          </Route>
          <Route path="/problems" element={<div data-testid="redirect-target">Redirected</div>} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('should allow access when user role is in allowedRoles', () => {
    fc.assert(
      fc.property(
        roleArbitrary,
        (role) => {
          const user: User = {
            id: 1,
            name: 'Test User',
            email: 'test@test.com',
            role,
            createdAt: new Date().toISOString(),
          };

          mockUseAuthStore.mockReturnValue({ user });

          const { unmount, getByTestId, queryByTestId } = renderWithRouter([role]);

          expect(getByTestId('protected-content')).toBeInTheDocument();
          expect(queryByTestId('redirect-target')).not.toBeInTheDocument();

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should deny access when user role is not in allowedRoles', () => {
    fc.assert(
      fc.property(
        userArbitrary('USER'),
        (user) => {
          mockUseAuthStore.mockReturnValue({ user });

          const { unmount, queryByTestId, getByTestId } = renderWithRouter(['ADMIN']);

          expect(queryByTestId('protected-content')).not.toBeInTheDocument();
          expect(getByTestId('redirect-target')).toBeInTheDocument();

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should deny access when user is null', () => {
    mockUseAuthStore.mockReturnValue({ user: null });

    const { queryByTestId, getByTestId } = renderWithRouter(['USER', 'ADMIN']);

    expect(queryByTestId('protected-content')).not.toBeInTheDocument();
    expect(getByTestId('redirect-target')).toBeInTheDocument();
  });

  it('should allow ADMIN access to any role-restricted route', () => {
    fc.assert(
      fc.property(
        userArbitrary('ADMIN'),
        fc.array(roleArbitrary, { minLength: 1, maxLength: 3 }),
        (user, allowedRoles) => {
          const rolesWithAdmin = [...new Set([...allowedRoles, 'ADMIN' as Role])];
          
          mockUseAuthStore.mockReturnValue({ user });

          const { unmount, getByTestId, queryByTestId } = renderWithRouter(rolesWithAdmin);

          expect(getByTestId('protected-content')).toBeInTheDocument();
          expect(queryByTestId('redirect-target')).not.toBeInTheDocument();

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should correctly handle PROBLEM_SETTER access to problem creation routes', () => {
    fc.assert(
      fc.property(
        userArbitrary('PROBLEM_SETTER'),
        (user) => {
          mockUseAuthStore.mockReturnValue({ user });

          const { unmount, getByTestId, queryByTestId } = renderWithRouter(['PROBLEM_SETTER', 'ADMIN']);

          expect(getByTestId('protected-content')).toBeInTheDocument();
          expect(queryByTestId('redirect-target')).not.toBeInTheDocument();

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });
});
