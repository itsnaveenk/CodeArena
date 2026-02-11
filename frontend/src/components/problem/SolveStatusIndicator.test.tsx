import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { render } from '@testing-library/react';
import { SolveStatusIndicator } from './SolveStatusIndicator';
import { SOLVE_STATUS_ICONS, type SolveStatus } from '@/types';

const solveStatusArbitrary = fc.constantFrom<SolveStatus>('SOLVED', 'ATTEMPTED', 'NOT_TRIED');

describe('Property 8: Solve Status Indicator Consistency', () => {
  it('should render an SVG for any status', () => {
    fc.assert(
      fc.property(
        solveStatusArbitrary,
        (status) => {
          const { unmount, container } = render(<SolveStatusIndicator status={status} />);
          
          const svg = container.querySelector('svg');
          expect(svg).toBeInTheDocument();
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should apply correct color class for any status', () => {
    fc.assert(
      fc.property(
        solveStatusArbitrary,
        (status) => {
          const { unmount, container } = render(<SolveStatusIndicator status={status} />);
          
          const svg = container.querySelector('svg');
          const expectedColor = SOLVE_STATUS_ICONS[status].color;
          
          const classAttr = svg?.getAttribute('class') || '';
          expect(classAttr).toContain(expectedColor);
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map SOLVED to green color', () => {
    fc.assert(
      fc.property(
        fc.constant('SOLVED' as SolveStatus),
        (status) => {
          const { unmount, container } = render(<SolveStatusIndicator status={status} />);
          
          const svg = container.querySelector('svg');
          const classAttr = svg?.getAttribute('class') || '';
          expect(classAttr).toContain('text-green-500');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map ATTEMPTED to yellow color', () => {
    fc.assert(
      fc.property(
        fc.constant('ATTEMPTED' as SolveStatus),
        (status) => {
          const { unmount, container } = render(<SolveStatusIndicator status={status} />);
          
          const svg = container.querySelector('svg');
          const classAttr = svg?.getAttribute('class') || '';
          expect(classAttr).toContain('text-yellow-500');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map NOT_TRIED to gray color', () => {
    fc.assert(
      fc.property(
        fc.constant('NOT_TRIED' as SolveStatus),
        (status) => {
          const { unmount, container } = render(<SolveStatusIndicator status={status} />);
          
          const svg = container.querySelector('svg');
          const classAttr = svg?.getAttribute('class') || '';
          expect(classAttr).toContain('text-gray-300');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should accept optional className prop', () => {
    fc.assert(
      fc.property(
        solveStatusArbitrary,
        fc.string({ minLength: 1, maxLength: 20 }).filter(s => /^[a-z-]+$/.test(s)),
        (status, customClass) => {
          const { unmount, container } = render(
            <SolveStatusIndicator status={status} className={customClass} />
          );
          
          const svg = container.querySelector('svg');
          const classAttr = svg?.getAttribute('class') || '';
          expect(classAttr).toContain(customClass);
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should have consistent size (h-5 w-5) for all statuses', () => {
    fc.assert(
      fc.property(
        solveStatusArbitrary,
        (status) => {
          const { unmount, container } = render(<SolveStatusIndicator status={status} />);
          
          const svg = container.querySelector('svg');
          const classAttr = svg?.getAttribute('class') || '';
          expect(classAttr).toContain('h-5');
          expect(classAttr).toContain('w-5');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });
});
