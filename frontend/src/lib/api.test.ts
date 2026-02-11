import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as fc from 'fast-check';
import { ApiClient, ApiClientError } from './api';

const mockFetch = vi.fn();
(globalThis as unknown as { fetch: typeof mockFetch }).fetch = mockFetch;

describe('Property 13: JWT Token Inclusion in Authenticated Requests', () => {
  let apiClient: ApiClient;
  let mockGetToken: ReturnType<typeof vi.fn>;
  let mockOnUnauthorized: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockGetToken = vi.fn();
    mockOnUnauthorized = vi.fn();
    apiClient = new ApiClient({
      getToken: mockGetToken as () => string | null,
      onUnauthorized: mockOnUnauthorized as () => void,
    });
  });

  it('should include Bearer token in Authorization header when token exists', async () => {
    const tokenArbitrary = fc.string({ minLength: 10, maxLength: 100 })
      .filter(s => /^[a-zA-Z0-9._-]+$/.test(s));

    await fc.assert(
      fc.asyncProperty(
        tokenArbitrary,
        async (token) => {
          vi.clearAllMocks();
          mockGetToken.mockReturnValue(token);
          mockFetch.mockResolvedValue({
            ok: true,
            status: 200,
            json: () => Promise.resolve({ data: 'test' }),
          });

          await apiClient.getCurrentUser();

          expect(mockFetch).toHaveBeenCalled();
          const [, options] = mockFetch.mock.calls[0];
          expect(options.headers['Authorization']).toBe(`Bearer ${token}`);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should not include Authorization header when token is null', async () => {
    mockGetToken.mockReturnValue(null);
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ data: 'test' }),
    });

    await apiClient.getCurrentUser();

    expect(mockFetch).toHaveBeenCalled();
    const [, options] = mockFetch.mock.calls[0];
    expect(options.headers['Authorization']).toBeUndefined();
  });

  it('should not include Authorization header for auth endpoints', async () => {
    const tokenArbitrary = fc.string({ minLength: 10, maxLength: 100 })
      .filter(s => /^[a-zA-Z0-9._-]+$/.test(s));

    await fc.assert(
      fc.asyncProperty(
        tokenArbitrary,
        async (token) => {
          vi.clearAllMocks();
          mockGetToken.mockReturnValue(token);
          mockFetch.mockResolvedValue({
            ok: true,
            status: 200,
            json: () => Promise.resolve({ token: 'new-token', user: {} }),
          });

          await apiClient.login({ email: 'test@test.com', password: 'password123' });

          expect(mockFetch).toHaveBeenCalled();
          const [, options] = mockFetch.mock.calls[0];
          expect(options.headers['Authorization']).toBeUndefined();
        }
      ),
      { numRuns: 100 }
    );
  });
});

describe('Property 14: API Error Response Handling', () => {
  let apiClient: ApiClient;
  let mockGetToken: ReturnType<typeof vi.fn>;
  let mockOnUnauthorized: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockGetToken = vi.fn().mockReturnValue('test-token');
    mockOnUnauthorized = vi.fn();
    apiClient = new ApiClient({
      getToken: mockGetToken as () => string | null,
      onUnauthorized: mockOnUnauthorized as () => void,
    });
  });

  it('should call onUnauthorized and throw UNAUTHORIZED error on 401', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: () => Promise.resolve({ code: 'UNAUTHORIZED', message: 'Unauthorized' }),
    });

    await expect(apiClient.getCurrentUser()).rejects.toThrow(ApiClientError);
    
    try {
      await apiClient.getCurrentUser();
    } catch (error) {
      expect(error).toBeInstanceOf(ApiClientError);
      expect((error as ApiClientError).code).toBe('UNAUTHORIZED');
    }
    expect(mockOnUnauthorized).toHaveBeenCalled();
  });

  it('should throw FORBIDDEN error on 403 without calling onUnauthorized', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 403,
      json: () => Promise.resolve({ code: 'FORBIDDEN', message: 'Forbidden' }),
    });

    try {
      await apiClient.getCurrentUser();
      expect.fail('Should have thrown');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiClientError);
      expect((error as ApiClientError).code).toBe('FORBIDDEN');
    }
    expect(mockOnUnauthorized).not.toHaveBeenCalled();
  });

  it('should throw RATE_LIMITED error with retryAfter on 429', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.integer({ min: 1, max: 3600 }),
        async (retryAfter) => {
          vi.clearAllMocks();
          mockGetToken.mockReturnValue('test-token');
          mockFetch.mockResolvedValue({
            ok: false,
            status: 429,
            headers: {
              get: (name: string) => name === 'Retry-After' ? String(retryAfter) : null,
            },
            json: () => Promise.resolve({}),
          });

          try {
            await apiClient.getCurrentUser();
            expect.fail('Should have thrown');
          } catch (error) {
            expect(error).toBeInstanceOf(ApiClientError);
            expect((error as ApiClientError).code).toBe('RATE_LIMITED');
            expect((error as ApiClientError).retryAfter).toBe(retryAfter);
          }
        }
      ),
      { numRuns: 100 }
    );
  });
});
