import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as fc from 'fast-check';
import { useThemeStore } from './themeStore';

const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

type Theme = 'light' | 'dark';

describe('Property 5: Theme Persistence Round-Trip', () => {
  beforeEach(() => {
    useThemeStore.setState({ theme: 'dark' });
    vi.clearAllMocks();
  });

  it('setTheme should store and retrieve the same theme value', () => {
    fc.assert(
      fc.property(
        fc.constantFrom<Theme>('light', 'dark'),
        (theme) => {
          const store = useThemeStore.getState();
          store.setTheme(theme);
          
          const state = useThemeStore.getState();
          expect(state.theme).toBe(theme);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('toggleTheme should alternate between light and dark', () => {
    fc.assert(
      fc.property(
        fc.constantFrom<Theme>('light', 'dark'),
        fc.integer({ min: 1, max: 10 }),
        (initialTheme, toggleCount) => {
          useThemeStore.setState({ theme: initialTheme });
          const store = useThemeStore.getState();
          
          let expectedTheme = initialTheme;
          for (let i = 0; i < toggleCount; i++) {
            store.toggleTheme();
            expectedTheme = expectedTheme === 'light' ? 'dark' : 'light';
          }
          
          expect(useThemeStore.getState().theme).toBe(expectedTheme);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('double toggle should return to original theme', () => {
    fc.assert(
      fc.property(
        fc.constantFrom<Theme>('light', 'dark'),
        (initialTheme) => {
          useThemeStore.setState({ theme: initialTheme });
          const store = useThemeStore.getState();
          
          store.toggleTheme();
          store.toggleTheme();
          
          expect(useThemeStore.getState().theme).toBe(initialTheme);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('setTheme should override any previous theme', () => {
    fc.assert(
      fc.property(
        fc.array(fc.constantFrom<Theme>('light', 'dark'), { minLength: 2, maxLength: 10 }),
        (themes) => {
          const store = useThemeStore.getState();
          
          for (const theme of themes) {
            store.setTheme(theme);
          }
          
          const lastTheme = themes[themes.length - 1];
          expect(useThemeStore.getState().theme).toBe(lastTheme);
        }
      ),
      { numRuns: 100 }
    );
  });
});
