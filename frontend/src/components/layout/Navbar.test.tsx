import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as fc from 'fast-check';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Navbar } from './Navbar';
import { useAuthStore } from '@/stores/authStore';
import type { Role } from '@/types';

vi.mock('@/stores/authStore');
vi.mock('@/stores/themeStore', () => ({
  useThemeStore: () => ({
    theme: 'dark',
    toggleTheme: vi.fn(),
  }),
}));

const mockUseAuthStore = useAuthStore as unknown as ReturnType<typeof vi.fn>;

const userArbitrary = (role: Role) => fc.record({
  id: fc.integer({ min: 1, max: 10000 }),
  name: fc.string({ minLength: 2, maxLength: 50 }).filter(s => /^[a-zA-Z][a-zA-Z0-9]*$/.test(s)),
  email: fc.tuple(
    fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-z]+$/.test(s)),
    fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-z]+$/.test(s)),
    fc.constantFrom('com', 'org', 'net')
  ).map(([local, domain, tld]) => `${local}@${domain}.${tld}`),
  role: fc.constant(role),
  createdAt: fc.constant(new Date().toISOString()),
});

describe('Property 4: Role-Based Navigation Rendering', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should show Login/Signup buttons when not authenticated', () => {
    mockUseAuthStore.mockReturnValue({
      user: null,
      isAuthenticated: false,
      clearAuth: vi.fn(),
    });

    render(
      <BrowserRouter>
        <Navbar />
      </BrowserRouter>
    );

    expect(screen.getByText('Login')).toBeInTheDocument();
    expect(screen.getByText('Sign Up')).toBeInTheDocument();
  });

  it('should show user name when authenticated as USER', () => {
    fc.assert(
      fc.property(
        userArbitrary('USER'),
        (user) => {
          mockUseAuthStore.mockReturnValue({
            user,
            isAuthenticated: true,
            clearAuth: vi.fn(),
          });

          const { unmount } = render(
            <BrowserRouter>
              <Navbar />
            </BrowserRouter>
          );

          expect(screen.getByText(user.name)).toBeInTheDocument();
          
          expect(screen.queryByText('Create Problem')).not.toBeInTheDocument();
          expect(screen.queryByText('Admin')).not.toBeInTheDocument();

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should show Create Problem link for PROBLEM_SETTER', () => {
    fc.assert(
      fc.property(
        userArbitrary('PROBLEM_SETTER'),
        (user) => {
          mockUseAuthStore.mockReturnValue({
            user,
            isAuthenticated: true,
            clearAuth: vi.fn(),
          });

          const { unmount } = render(
            <BrowserRouter>
              <Navbar />
            </BrowserRouter>
          );

          expect(screen.getByText('Create Problem')).toBeInTheDocument();
          expect(screen.queryByText('Admin')).not.toBeInTheDocument();

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should show Admin link for ADMIN users', () => {
    fc.assert(
      fc.property(
        userArbitrary('ADMIN'),
        (user) => {
          mockUseAuthStore.mockReturnValue({
            user,
            isAuthenticated: true,
            clearAuth: vi.fn(),
          });

          const { unmount } = render(
            <BrowserRouter>
              <Navbar />
            </BrowserRouter>
          );

          expect(screen.getByText('Admin')).toBeInTheDocument();
          expect(screen.queryByText('Create Problem')).not.toBeInTheDocument();

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });
});
