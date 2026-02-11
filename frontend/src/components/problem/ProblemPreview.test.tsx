import { describe, it, expect, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import {
  ProblemPreview,
  containsMarkdown,
  containsSyntaxHighlighting,
} from './ProblemPreview';
import type { Difficulty, Testcase } from '@/types';

describe('ProblemPreview', () => {
  afterEach(() => {
    cleanup();
  });

  const defaultProps = {
    title: 'Two Sum',
    difficulty: 'EASY' as Difficulty,
    tags: ['array', 'hash-table'],
    statement: 'Given an array of integers...',
    constraints: '2 <= nums.length <= 10^4',
    examples: [
      { id: 1, input: '[2,7,11,15], target = 9', expectedOutput: '[0,1]', isHidden: false },
      { id: 2, input: '[3,2,4], target = 6', expectedOutput: '[1,2]', isHidden: false },
    ] as Testcase[],
  };

  describe('Unit Tests', () => {
    it('renders the problem title', () => {
      render(<ProblemPreview {...defaultProps} />);
      expect(screen.getByTestId('preview-title')).toHaveTextContent('Two Sum');
    });

    it('renders the difficulty badge', () => {
      render(<ProblemPreview {...defaultProps} />);
      expect(screen.getByText('EASY')).toBeInTheDocument();
    });

    it('renders all tags', () => {
      render(<ProblemPreview {...defaultProps} />);
      expect(screen.getByText('array')).toBeInTheDocument();
      expect(screen.getByText('hash-table')).toBeInTheDocument();
    });

    it('renders the problem statement', () => {
      render(<ProblemPreview {...defaultProps} />);
      expect(screen.getByTestId('preview-statement')).toHaveTextContent(
        'Given an array of integers...'
      );
    });

    it('renders constraints', () => {
      render(<ProblemPreview {...defaultProps} />);
      expect(screen.getByTestId('preview-constraints')).toHaveTextContent(
        '2 <= nums.length <= 10^4'
      );
    });

    it('renders visible examples', () => {
      render(<ProblemPreview {...defaultProps} />);
      expect(screen.getByText('Example 1')).toBeInTheDocument();
      expect(screen.getByText('Example 2')).toBeInTheDocument();
    });

    it('shows default title when title is empty', () => {
      render(<ProblemPreview {...defaultProps} title="" />);
      expect(screen.getByTestId('preview-title')).toHaveTextContent('Untitled Problem');
    });
  });

  describe('Property Tests', () => {
    it('Property 15: renders markdown as HTML, not raw markdown', () => {
      const markdownStatement = `
# Problem Title

This is **bold** and *italic* text.

Here is some \`inline code\`.

\`\`\`javascript
function solution(nums) {
  return nums;
}
\`\`\`
      `;

      render(<ProblemPreview {...defaultProps} statement={markdownStatement} />);

      const statementEl = screen.getByTestId('preview-statement');
      const html = statementEl.innerHTML;

      expect(html).not.toContain('**bold**');
      expect(html).not.toContain('*italic*');
      expect(html).not.toContain('```javascript');

      expect(html).toContain('<strong>');
      expect(html).toContain('<em>');
      expect(html).toContain('<code');
    });

    it('Property 15b: containsMarkdown utility correctly identifies markdown', () => {
      expect(containsMarkdown('**bold**')).toBe(true);
      expect(containsMarkdown('*italic*')).toBe(true);
      expect(containsMarkdown('`code`')).toBe(true);
      expect(containsMarkdown('```\ncode block\n```')).toBe(true);
      expect(containsMarkdown('# Header')).toBe(true);
      expect(containsMarkdown('- list item')).toBe(true);
      expect(containsMarkdown('[link](url)')).toBe(true);

      expect(containsMarkdown('plain text')).toBe(false);
      expect(containsMarkdown('no markdown here')).toBe(false);
    });

    it('Property 16: displays all required problem fields', () => {
      const props = {
        title: 'Test Problem',
        difficulty: 'MEDIUM' as Difficulty,
        tags: ['tag1', 'tag2', 'tag3'],
        statement: 'Problem statement here',
        constraints: 'Constraint text',
        examples: [
          { id: 1, input: 'input1', expectedOutput: 'output1', isHidden: false },
          { id: 2, input: 'input2', expectedOutput: 'output2', isHidden: false },
        ] as Testcase[],
      };

      render(<ProblemPreview {...props} />);

      expect(screen.getByTestId('preview-title')).toHaveTextContent('Test Problem');

      expect(screen.getByText('MEDIUM')).toBeInTheDocument();

      props.tags.forEach((tag) => {
        expect(screen.getByText(tag)).toBeInTheDocument();
      });

      expect(screen.getByTestId('preview-statement')).toHaveTextContent(
        'Problem statement here'
      );

      expect(screen.getByTestId('preview-constraints')).toHaveTextContent(
        'Constraint text'
      );

      expect(screen.getByText('Example 1')).toBeInTheDocument();
      expect(screen.getByText('Example 2')).toBeInTheDocument();
      expect(screen.getByText('input1')).toBeInTheDocument();
      expect(screen.getByText('output1')).toBeInTheDocument();
    });

    it('Property 16b: only displays visible examples, not hidden ones', () => {
      const props = {
        ...defaultProps,
        examples: [
          { id: 1, input: 'visible input', expectedOutput: 'visible output', isHidden: false },
          { id: 2, input: 'hidden input', expectedOutput: 'hidden output', isHidden: true },
          { id: 3, input: 'another visible', expectedOutput: 'another output', isHidden: false },
        ] as Testcase[],
      };

      render(<ProblemPreview {...props} />);

      expect(screen.getByText('visible input')).toBeInTheDocument();
      expect(screen.getByText('another visible')).toBeInTheDocument();

      expect(screen.queryByText('hidden input')).not.toBeInTheDocument();
      expect(screen.queryByText('hidden output')).not.toBeInTheDocument();

      expect(screen.getByText('Example 1')).toBeInTheDocument();
      expect(screen.getByText('Example 2')).toBeInTheDocument();
      expect(screen.queryByText('Example 3')).not.toBeInTheDocument();
    });

    it('Property 17: renders code blocks with syntax highlighting', () => {
      const statementWithCode = `
Here is a solution:

\`\`\`javascript
function twoSum(nums, target) {
  const map = new Map();
  for (let i = 0; i < nums.length; i++) {
    const complement = target - nums[i];
    if (map.has(complement)) {
      return [map.get(complement), i];
    }
    map.set(nums[i], i);
  }
  return [];
}
\`\`\`
      `;

      render(<ProblemPreview {...defaultProps} statement={statementWithCode} />);

      const statementEl = screen.getByTestId('preview-statement');
      const html = statementEl.innerHTML;

      expect(containsSyntaxHighlighting(html)).toBe(true);

      expect(html).toContain('<pre');
      expect(html).toContain('<code');
    });

    it('Property 17b: handles multiple code blocks with different languages', () => {
      const statementWithMultipleCodeBlocks = `
JavaScript solution:

\`\`\`javascript
const x = 1;
\`\`\`

Python solution:

\`\`\`python
x = 1
\`\`\`
      `;

      render(
        <ProblemPreview {...defaultProps} statement={statementWithMultipleCodeBlocks} />
      );

      const statementEl = screen.getByTestId('preview-statement');
      const html = statementEl.innerHTML;

      expect(html).toContain('language-javascript');
      expect(html).toContain('language-python');
    });

    it('Property 17c: handles code blocks without language specifier', () => {
      const statementWithPlainCode = `
\`\`\`
plain code block
\`\`\`
      `;

      render(<ProblemPreview {...defaultProps} statement={statementWithPlainCode} />);

      const statementEl = screen.getByTestId('preview-statement');
      const html = statementEl.innerHTML;

      expect(html).toContain('<pre');
      expect(html).toContain('<code');
    });
  });

  describe('Edge Cases', () => {
    it('handles empty statement gracefully', () => {
      render(<ProblemPreview {...defaultProps} statement="" />);
      expect(screen.getByTestId('preview-statement')).toBeInTheDocument();
    });

    it('handles empty tags array', () => {
      render(<ProblemPreview {...defaultProps} tags={[]} />);
      expect(screen.queryByTestId('preview-tags')).not.toBeInTheDocument();
    });

    it('handles empty examples array', () => {
      render(<ProblemPreview {...defaultProps} examples={[]} />);
      expect(screen.getByText('No visible examples added yet.')).toBeInTheDocument();
    });

    it('handles all hidden examples', () => {
      const props = {
        ...defaultProps,
        examples: [
          { id: 1, input: 'hidden1', expectedOutput: 'out1', isHidden: true },
          { id: 2, input: 'hidden2', expectedOutput: 'out2', isHidden: true },
        ] as Testcase[],
      };

      render(<ProblemPreview {...props} />);
      expect(screen.getByText('No visible examples added yet.')).toBeInTheDocument();
    });

    it('handles empty constraints', () => {
      render(<ProblemPreview {...defaultProps} constraints="" />);
      expect(screen.queryByTestId('preview-constraints')).not.toBeInTheDocument();
    });
  });
});
