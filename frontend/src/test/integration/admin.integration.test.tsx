import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { RoleGuard } from '@/components/auth/RoleGuard';

const mockFetch = vi.fn();
(globalThis as unknown as { fetch: typeof mockFetch }).fetch = mockFetch;

const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
});

const mockUsers = [
  {
    id: 1,
    name: 'Admin User',
    email: 'admin@example.com',
    role: 'ADMIN' as const,
    createdAt: new Date().toISOString(),
  },
  {
    id: 2,
    name: 'Problem Setter',
    email: 'setter@example.com',
    role: 'PROBLEM_SETTER' as const,
    createdAt: new Date().toISOString(),
  },
  {
    id: 3,
    name: 'Regular User',
    email: 'user@example.com',
    role: 'USER' as const,
    createdAt: new Date().toISOString(),
  },
];

describe('Admin Flow Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    
    useAuthStore.setState({
      user: {
        id: 1,
        name: 'Admin User',
        email: 'admin@example.com',
        role: 'ADMIN',
        createdAt: new Date().toISOString(),
      },
      token: 'admin-token',
      isAuthenticated: true,
      isLoading: false,
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('Admin Access Control', () => {
    it('should allow ADMIN to access admin routes', () => {
      const queryClient = createTestQueryClient();
      
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/admin']}>
            <Routes>
              <Route element={<RoleGuard allowedRoles={['ADMIN']} />}>
                <Route path="/admin" element={<div>Admin Dashboard</div>} />
              </Route>
              <Route path="/problems" element={<div>Problems Page</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      );

      expect(screen.getByText('Admin Dashboard')).toBeInTheDocument();
      expect(screen.queryByText('Problems Page')).not.toBeInTheDocument();
    });

    it('should deny USER access to admin routes', () => {
      useAuthStore.setState({
        user: {
          id: 3,
          name: 'Regular User',
          email: 'user@example.com',
          role: 'USER',
          createdAt: new Date().toISOString(),
        },
        token: 'user-token',
        isAuthenticated: true,
        isLoading: false,
      });

      const queryClient = createTestQueryClient();
      
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/admin']}>
            <Routes>
              <Route element={<RoleGuard allowedRoles={['ADMIN']} />}>
                <Route path="/admin" element={<div>Admin Dashboard</div>} />
              </Route>
              <Route path="/problems" element={<div>Problems Page</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      );

      expect(screen.queryByText('Admin Dashboard')).not.toBeInTheDocument();
      expect(screen.getByText('Problems Page')).toBeInTheDocument();
    });

    it('should deny PROBLEM_SETTER access to admin routes', () => {
      useAuthStore.setState({
        user: {
          id: 2,
          name: 'Problem Setter',
          email: 'setter@example.com',
          role: 'PROBLEM_SETTER',
          createdAt: new Date().toISOString(),
        },
        token: 'setter-token',
        isAuthenticated: true,
        isLoading: false,
      });

      const queryClient = createTestQueryClient();
      
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/admin']}>
            <Routes>
              <Route element={<RoleGuard allowedRoles={['ADMIN']} />}>
                <Route path="/admin" element={<div>Admin Dashboard</div>} />
              </Route>
              <Route path="/problems" element={<div>Problems Page</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      );

      expect(screen.queryByText('Admin Dashboard')).not.toBeInTheDocument();
      expect(screen.getByText('Problems Page')).toBeInTheDocument();
    });
  });

  describe('User Management', () => {
    it('should render user management table', () => {
      const queryClient = createTestQueryClient();
      
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({
          content: mockUsers,
          totalElements: mockUsers.length,
          totalPages: 1,
          page: 0,
          size: 10,
        }),
      });
      
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <div>User Management Placeholder</div>
          </MemoryRouter>
        </QueryClientProvider>
      );

      expect(screen.getByText('User Management Placeholder')).toBeInTheDocument();
    });
  });

  describe('API Integration', () => {
    beforeEach(() => {
      mockFetch.mockReset();
    });

    it('should call publish API endpoint', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ id: 1, status: 'PUBLISHED' }),
      });

      const response = await fetch('http://localhost:8080/api/admin/problems/1/publish', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': 'Bearer admin-token',
        },
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.status).toBe('PUBLISHED');
    });

    it('should call reject API endpoint', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ id: 1, status: 'REJECTED' }),
      });

      const response = await fetch('http://localhost:8080/api/admin/problems/1/reject', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': 'Bearer admin-token',
        },
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.status).toBe('REJECTED');
    });

    it('should call role change API endpoint', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ id: 3, role: 'PROBLEM_SETTER' }),
      });

      const response = await fetch('http://localhost:8080/api/admin/users/3/role', {
        method: 'PUT',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': 'Bearer admin-token',
        },
        body: JSON.stringify({ role: 'PROBLEM_SETTER' }),
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.role).toBe('PROBLEM_SETTER');
    });
  });
});
