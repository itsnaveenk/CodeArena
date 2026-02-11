import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { render, screen } from '@testing-library/react';
import { DifficultyBadge } from './DifficultyBadge';
import { DIFFICULTY_COLORS, type Difficulty } from '@/types';

const difficultyArbitrary = fc.constantFrom<Difficulty>('EASY', 'MEDIUM', 'HARD');

describe('Property 7: Difficulty Color Mapping Consistency', () => {
  it('should render correct text for any difficulty', () => {
    fc.assert(
      fc.property(
        difficultyArbitrary,
        (difficulty) => {
          const { unmount } = render(<DifficultyBadge difficulty={difficulty} />);
          
          expect(screen.getByText(difficulty)).toBeInTheDocument();
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should apply correct color classes for any difficulty', () => {
    fc.assert(
      fc.property(
        difficultyArbitrary,
        (difficulty) => {
          const { unmount, container } = render(<DifficultyBadge difficulty={difficulty} />);
          
          const badge = container.firstElementChild as HTMLElement;
          const expectedColors = DIFFICULTY_COLORS[difficulty];
          
          expect(badge.className).toContain(expectedColors.bg.split(' ')[0]);
          expect(badge.className).toContain(expectedColors.text.split(' ')[0]);
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map EASY to green colors', () => {
    fc.assert(
      fc.property(
        fc.constant('EASY' as Difficulty),
        (difficulty) => {
          const { unmount, container } = render(<DifficultyBadge difficulty={difficulty} />);
          
          const badge = container.firstElementChild as HTMLElement;
          expect(badge.className).toContain('bg-green');
          expect(badge.className).toContain('text-green');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map MEDIUM to yellow colors', () => {
    fc.assert(
      fc.property(
        fc.constant('MEDIUM' as Difficulty),
        (difficulty) => {
          const { unmount, container } = render(<DifficultyBadge difficulty={difficulty} />);
          
          const badge = container.firstElementChild as HTMLElement;
          expect(badge.className).toContain('bg-yellow');
          expect(badge.className).toContain('text-yellow');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should map HARD to red colors', () => {
    fc.assert(
      fc.property(
        fc.constant('HARD' as Difficulty),
        (difficulty) => {
          const { unmount, container } = render(<DifficultyBadge difficulty={difficulty} />);
          
          const badge = container.firstElementChild as HTMLElement;
          expect(badge.className).toContain('bg-red');
          expect(badge.className).toContain('text-red');
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should accept optional className prop', () => {
    fc.assert(
      fc.property(
        difficultyArbitrary,
        fc.string({ minLength: 1, maxLength: 20 }).filter(s => /^[a-z-]+$/.test(s)),
        (difficulty, customClass) => {
          const { unmount, container } = render(
            <DifficultyBadge difficulty={difficulty} className={customClass} />
          );
          
          const badge = container.firstElementChild as HTMLElement;
          expect(badge.className).toContain(customClass);
          
          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });
});
