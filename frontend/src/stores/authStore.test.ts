import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as fc from 'fast-check';
import { useAuthStore } from './authStore';
import type { Role } from '@/types';

const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

const userArbitrary = fc.record({
  id: fc.integer({ min: 1, max: 10000 }),
  name: fc.string({ minLength: 1, maxLength: 100 }).filter(s => s.trim().length > 0),
  email: fc.tuple(
    fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-z]+$/.test(s)),
    fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-z]+$/.test(s)),
    fc.constantFrom('com', 'org', 'net')
  ).map(([local, domain, tld]) => `${local}@${domain}.${tld}`),
  role: fc.constantFrom<Role>('USER', 'PROBLEM_SETTER', 'ADMIN'),
  createdAt: fc.constant(new Date().toISOString()),
});

const tokenArbitrary = fc.string({ minLength: 10, maxLength: 500 });

describe('Property 6: Auth Store State Management', () => {
  beforeEach(() => {
    useAuthStore.setState({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    });
    vi.clearAllMocks();
  });

  it('setAuth should set isAuthenticated to true with valid user and token', () => {
    fc.assert(
      fc.property(
        userArbitrary,
        tokenArbitrary,
        (user, token) => {
          const store = useAuthStore.getState();
          store.setAuth(user, token);
          
          const state = useAuthStore.getState();
          expect(state.isAuthenticated).toBe(true);
          expect(state.user).toEqual(user);
          expect(state.token).toBe(token);
          expect(state.isLoading).toBe(false);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('clearAuth should reset all auth state', () => {
    fc.assert(
      fc.property(
        userArbitrary,
        tokenArbitrary,
        (user, token) => {
          const store = useAuthStore.getState();
          
          store.setAuth(user, token);
          expect(useAuthStore.getState().isAuthenticated).toBe(true);
          
          store.clearAuth();
          
          const state = useAuthStore.getState();
          expect(state.isAuthenticated).toBe(false);
          expect(state.user).toBeNull();
          expect(state.token).toBeNull();
          expect(state.isLoading).toBe(false);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('setLoading should update loading state', () => {
    fc.assert(
      fc.property(
        fc.boolean(),
        (loading) => {
          const store = useAuthStore.getState();
          store.setLoading(loading);
          
          expect(useAuthStore.getState().isLoading).toBe(loading);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('multiple setAuth calls should update state correctly', () => {
    fc.assert(
      fc.property(
        fc.array(fc.tuple(userArbitrary, tokenArbitrary), { minLength: 2, maxLength: 5 }),
        (authPairs) => {
          const store = useAuthStore.getState();
          
          for (const [user, token] of authPairs) {
            store.setAuth(user, token);
            
            const state = useAuthStore.getState();
            expect(state.user).toEqual(user);
            expect(state.token).toBe(token);
            expect(state.isAuthenticated).toBe(true);
          }
        }
      ),
      { numRuns: 100 }
    );
  });
});
