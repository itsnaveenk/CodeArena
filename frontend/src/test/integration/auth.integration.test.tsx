import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';

const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
});

describe('Authentication Flow Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.setState({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Protected Route Access', () => {
    it('should redirect to login when not authenticated', () => {
      const queryClient = createTestQueryClient();
      
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/protected']}>
            <Routes>
              <Route element={<ProtectedRoute />}>
                <Route path="/protected" element={<div>Protected Content</div>} />
              </Route>
              <Route path="/login" element={<div>Login Page</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      );

      expect(screen.getByText('Login Page')).toBeInTheDocument();
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    });

    it('should allow access when authenticated', () => {
      useAuthStore.setState({
        user: {
          id: 1,
          name: 'Test User',
          email: 'test@example.com',
          role: 'USER',
          createdAt: new Date().toISOString(),
        },
        token: 'test-token',
        isAuthenticated: true,
        isLoading: false,
      });

      const queryClient = createTestQueryClient();
      
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/protected']}>
            <Routes>
              <Route element={<ProtectedRoute />}>
                <Route path="/protected" element={<div>Protected Content</div>} />
              </Route>
              <Route path="/login" element={<div>Login Page</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      );

      expect(screen.getByText('Protected Content')).toBeInTheDocument();
      expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
    });
  });

  describe('Auth Store State Management', () => {
    it('should set auth state correctly', () => {
      const user = {
        id: 1,
        name: 'Test User',
        email: 'test@example.com',
        role: 'USER' as const,
        createdAt: new Date().toISOString(),
      };
      const token = 'test-jwt-token';

      useAuthStore.getState().setAuth(user, token);

      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(true);
      expect(state.user).toEqual(user);
      expect(state.token).toBe(token);
    });

    it('should clear auth state on logout', () => {
      useAuthStore.setState({
        user: {
          id: 1,
          name: 'Test User',
          email: 'test@example.com',
          role: 'USER',
          createdAt: new Date().toISOString(),
        },
        token: 'test-token',
        isAuthenticated: true,
        isLoading: false,
      });

      expect(useAuthStore.getState().isAuthenticated).toBe(true);
      expect(useAuthStore.getState().user).not.toBeNull();

      useAuthStore.getState().clearAuth();

      expect(useAuthStore.getState().isAuthenticated).toBe(false);
      expect(useAuthStore.getState().user).toBeNull();
      expect(useAuthStore.getState().token).toBeNull();
    });

    it('should handle loading state', () => {
      useAuthStore.getState().setLoading(true);
      expect(useAuthStore.getState().isLoading).toBe(true);

      useAuthStore.getState().setLoading(false);
      expect(useAuthStore.getState().isLoading).toBe(false);
    });
  });

  describe('Role-Based Access', () => {
    it('should store user role correctly', () => {
      const adminUser = {
        id: 1,
        name: 'Admin User',
        email: 'admin@example.com',
        role: 'ADMIN' as const,
        createdAt: new Date().toISOString(),
      };

      useAuthStore.getState().setAuth(adminUser, 'admin-token');

      expect(useAuthStore.getState().user?.role).toBe('ADMIN');
    });

    it('should differentiate between user roles', () => {
      const roles = ['USER', 'PROBLEM_SETTER', 'ADMIN'] as const;

      for (const role of roles) {
        const user = {
          id: 1,
          name: 'Test User',
          email: 'test@example.com',
          role,
          createdAt: new Date().toISOString(),
        };

        useAuthStore.getState().setAuth(user, 'token');
        expect(useAuthStore.getState().user?.role).toBe(role);
      }
    });
  });
});
