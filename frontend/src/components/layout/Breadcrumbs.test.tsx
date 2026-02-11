import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import fc from 'fast-check';
import { Breadcrumbs, generateBreadcrumbs, type BreadcrumbItem } from './Breadcrumbs';

const renderWithRouter = (ui: React.ReactElement) => {
  return render(<BrowserRouter>{ui}</BrowserRouter>);
};

const breadcrumbItemArbitrary = (): fc.Arbitrary<BreadcrumbItem> =>
  fc.record({
    label: fc.string({ minLength: 1, maxLength: 30 }).filter(s => s.trim().length > 0),
    href: fc.option(fc.string({ minLength: 1, maxLength: 50 }).map(s => `/${s}`), { nil: undefined }),
  });

const pathSegmentArbitrary = (): fc.Arbitrary<string> =>
  fc.constantFrom(
    'dashboard',
    'my-problems',
    'create-problem',
    'edit-problem',
    'admin',
    'users',
    'problems',
    'submissions'
  );

describe('Breadcrumbs', () => {
  describe('Unit Tests', () => {
    it('renders nothing when items array is empty', () => {
      const { container } = renderWithRouter(<Breadcrumbs items={[]} />);
      expect(container.querySelector('nav')).toBeNull();
    });

    it('renders home link and single breadcrumb item', () => {
      const items: BreadcrumbItem[] = [{ label: 'Dashboard' }];
      renderWithRouter(<Breadcrumbs items={items} />);

      expect(screen.getByLabelText('Home')).toBeInTheDocument();
      expect(screen.getByText('Dashboard')).toBeInTheDocument();
    });

    it('renders multiple breadcrumb items with links', () => {
      const items: BreadcrumbItem[] = [
        { label: 'Admin', href: '/admin' },
        { label: 'Users' },
      ];
      renderWithRouter(<Breadcrumbs items={items} />);

      expect(screen.getByText('Admin')).toBeInTheDocument();
      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Admin' })).toHaveAttribute('href', '/admin');
    });

    it('last item is not a link', () => {
      const items: BreadcrumbItem[] = [
        { label: 'Admin', href: '/admin' },
        { label: 'Users', href: '/admin/users' },
      ];
      renderWithRouter(<Breadcrumbs items={items} />);

      const usersElement = screen.getByText('Users');
      expect(usersElement.tagName).toBe('SPAN');
    });

    it('applies custom className', () => {
      const items: BreadcrumbItem[] = [{ label: 'Test' }];
      renderWithRouter(<Breadcrumbs items={items} className="custom-class" />);

      const nav = screen.getByRole('navigation', { name: 'Breadcrumb' });
      expect(nav).toHaveClass('custom-class');
    });
  });

  describe('generateBreadcrumbs', () => {
    it('generates breadcrumbs for dashboard path', () => {
      const breadcrumbs = generateBreadcrumbs('/dashboard');
      expect(breadcrumbs).toHaveLength(1);
      expect(breadcrumbs[0].label).toBe('Dashboard');
    });

    it('generates breadcrumbs for nested admin path', () => {
      const breadcrumbs = generateBreadcrumbs('/admin/users');
      expect(breadcrumbs).toHaveLength(2);
      expect(breadcrumbs[0].label).toBe('Admin');
      expect(breadcrumbs[0].href).toBe('/admin');
      expect(breadcrumbs[1].label).toBe('Users');
      expect(breadcrumbs[1].href).toBeUndefined();
    });

    it('skips numeric IDs in breadcrumbs', () => {
      const breadcrumbs = generateBreadcrumbs('/edit-problem/123');
      expect(breadcrumbs).toHaveLength(1);
      expect(breadcrumbs[0].label).toBe('Edit Problem');
    });

    it('handles empty path', () => {
      const breadcrumbs = generateBreadcrumbs('/');
      expect(breadcrumbs).toHaveLength(0);
    });
  });

  describe('Property Tests', () => {
    it('Property 35: breadcrumb items are correctly generated from path', () => {
      fc.assert(
        fc.property(
          fc.array(pathSegmentArbitrary(), { minLength: 1, maxLength: 4 }),
          (segments) => {
            const path = '/' + segments.join('/');
            const breadcrumbs = generateBreadcrumbs(path);

            expect(breadcrumbs.length).toBe(segments.length);

            breadcrumbs.forEach((item, index) => {
              if (index < breadcrumbs.length - 1) {
                expect(item.href).toBeDefined();
              } else {
                expect(item.href).toBeUndefined();
              }
            });

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 35b: breadcrumb labels are human-readable', () => {
      const labelMap: Record<string, string> = {
        dashboard: 'Dashboard',
        'my-problems': 'My Problems',
        'create-problem': 'Create Problem',
        'edit-problem': 'Edit Problem',
        admin: 'Admin',
        users: 'Users',
        problems: 'Problems',
        submissions: 'Submissions',
      };

      fc.assert(
        fc.property(pathSegmentArbitrary(), (segment) => {
          const breadcrumbs = generateBreadcrumbs(`/${segment}`);
          expect(breadcrumbs.length).toBe(1);
          expect(breadcrumbs[0].label).toBe(labelMap[segment]);
          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('Property 35c: breadcrumb hrefs build correct paths', () => {
      fc.assert(
        fc.property(
          fc.array(pathSegmentArbitrary(), { minLength: 2, maxLength: 4 }),
          (segments) => {
            const path = '/' + segments.join('/');
            const breadcrumbs = generateBreadcrumbs(path);

            let expectedPath = '';
            breadcrumbs.forEach((item, index) => {
              expectedPath += `/${segments[index]}`;
              if (index < breadcrumbs.length - 1) {
                expect(item.href).toBe(expectedPath);
              }
            });

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 35d: rendered breadcrumbs match items array', () => {
      fc.assert(
        fc.property(
          fc.array(breadcrumbItemArbitrary(), { minLength: 1, maxLength: 5 }),
          (items) => {
            const { container } = renderWithRouter(<Breadcrumbs items={items} />);

            const homeLink = container.querySelector('a[aria-label="Home"]');
            expect(homeLink).toBeTruthy();

            const listItems = container.querySelectorAll('li');
            expect(listItems.length).toBe(items.length + 1); // +1 for home

            return true;
          }
        ),
        { numRuns: 50 }
      );
    });

    it('Property 35e: numeric IDs are always skipped', () => {
      fc.assert(
        fc.property(
          pathSegmentArbitrary(),
          fc.integer({ min: 1, max: 99999 }),
          (segment, id) => {
            const path = `/${segment}/${id}`;
            const breadcrumbs = generateBreadcrumbs(path);

            expect(breadcrumbs.length).toBe(1);

            breadcrumbs.forEach((item) => {
              expect(item.label).not.toBe(String(id));
            });

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });
  });

  describe('Edge Cases', () => {
    it('handles very long labels with truncation', () => {
      const items: BreadcrumbItem[] = [
        { label: 'This is a very long breadcrumb label that should be truncated' },
      ];
      renderWithRouter(<Breadcrumbs items={items} />);

      const label = screen.getByText('This is a very long breadcrumb label that should be truncated');
      expect(label).toHaveClass('truncate');
    });

    it('handles special characters in labels', () => {
      const items: BreadcrumbItem[] = [{ label: 'Test & Demo <script>' }];
      renderWithRouter(<Breadcrumbs items={items} />);

      expect(screen.getByText('Test & Demo <script>')).toBeInTheDocument();
    });

    it('handles path with trailing slash', () => {
      const breadcrumbs = generateBreadcrumbs('/dashboard/');
      expect(breadcrumbs).toHaveLength(1);
      expect(breadcrumbs[0].label).toBe('Dashboard');
    });
  });
});
