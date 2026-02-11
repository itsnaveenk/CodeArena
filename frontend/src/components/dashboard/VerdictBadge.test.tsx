import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { render, screen } from '@testing-library/react';
import { VerdictBadge } from './VerdictBadge';
import { VERDICT_COLORS, type Verdict } from '@/types';

const verdictArbitrary = fc.constantFrom<Verdict>(
  'ACCEPTED',
  'WRONG_ANSWER',
  'TLE',
  'RUNTIME_ERROR',
  'COMPILATION_ERROR'
);

const VERDICT_LABELS: Record<Verdict, string> = {
  ACCEPTED: 'Accepted',
  WRONG_ANSWER: 'Wrong Answer',
  TLE: 'Time Limit Exceeded',
  RUNTIME_ERROR: 'Runtime Error',
  COMPILATION_ERROR: 'Compilation Error',
};

describe('Property 9: Verdict Color Mapping Consistency', () => {
  it('should render correct label for any verdict', () => {
    fc.assert(
      fc.property(
        verdictArbitrary,
        (verdict) => {
          const { unmount } = render(<VerdictBadge verdict={verdict} />);
          
          expect(screen.getByText(VERDICT_LABELS[verdict])).toBeInTheDocument();
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should apply correct color classes for any verdict', () => {
    fc.assert(
      fc.property(
        verdictArbitrary,
        (verdict) => {
          const { unmount, container } = render(<VerdictBadge verdict={verdict} />);
          
          const badge = container.firstChild as HTMLElement;
          const expectedColors = VERDICT_COLORS[verdict];
          
          expect(badge.className).toContain(expectedColors.bg.split(' ')[0]);
          expect(badge.className).toContain(expectedColors.text.split(' ')[0]);
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map ACCEPTED to green colors', () => {
    fc.assert(
      fc.property(
        fc.constant('ACCEPTED' as Verdict),
        (verdict) => {
          const { unmount, container } = render(<VerdictBadge verdict={verdict} />);
          
          const badge = container.firstChild as HTMLElement;
          expect(badge.className).toContain('bg-green');
          expect(badge.className).toContain('text-green');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map WRONG_ANSWER to red colors', () => {
    fc.assert(
      fc.property(
        fc.constant('WRONG_ANSWER' as Verdict),
        (verdict) => {
          const { unmount, container } = render(<VerdictBadge verdict={verdict} />);
          
          const badge = container.firstChild as HTMLElement;
          expect(badge.className).toContain('bg-red');
          expect(badge.className).toContain('text-red');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map TLE to orange colors', () => {
    fc.assert(
      fc.property(
        fc.constant('TLE' as Verdict),
        (verdict) => {
          const { unmount, container } = render(<VerdictBadge verdict={verdict} />);
          
          const badge = container.firstChild as HTMLElement;
          expect(badge.className).toContain('bg-orange');
          expect(badge.className).toContain('text-orange');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map RUNTIME_ERROR to purple colors', () => {
    fc.assert(
      fc.property(
        fc.constant('RUNTIME_ERROR' as Verdict),
        (verdict) => {
          const { unmount, container } = render(<VerdictBadge verdict={verdict} />);
          
          const badge = container.firstChild as HTMLElement;
          expect(badge.className).toContain('bg-purple');
          expect(badge.className).toContain('text-purple');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map COMPILATION_ERROR to gray colors', () => {
    fc.assert(
      fc.property(
        fc.constant('COMPILATION_ERROR' as Verdict),
        (verdict) => {
          const { unmount, container } = render(<VerdictBadge verdict={verdict} />);
          
          const badge = container.firstChild as HTMLElement;
          expect(badge.className).toContain('bg-gray');
          expect(badge.className).toContain('text-gray');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should accept optional className prop', () => {
    fc.assert(
      fc.property(
        verdictArbitrary,
        fc.string({ minLength: 1, maxLength: 20 }).filter(s => /^[a-z-]+$/.test(s)),
        (verdict, customClass) => {
          const { unmount, container } = render(
            <VerdictBadge verdict={verdict} className={customClass} />
          );
          
          const badge = container.firstChild as HTMLElement;
          expect(badge.className).toContain(customClass);
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });
});
