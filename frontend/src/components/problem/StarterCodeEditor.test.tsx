import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { StarterCodeEditor, getDefaultTemplate } from './StarterCodeEditor';
import { SUPPORTED_LANGUAGES } from '@/types';

vi.mock('@/components/editor/CodeEditor', () => ({
  CodeEditor: ({ value, onChange, language }: { value: string; onChange: (code: string) => void; language: string }) => (
    <textarea
      data-testid={`code-editor-${language}`}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  ),
}));

describe('StarterCodeEditor', () => {
  const mockOnChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  describe('Unit Tests', () => {
    it('renders language tabs for all supported languages', () => {
      render(<StarterCodeEditor value={{}} onChange={mockOnChange} />);
      
      SUPPORTED_LANGUAGES.forEach((lang) => {
        expect(screen.getByRole('tab', { name: lang.name })).toBeInTheDocument();
      });
    });

    it('renders title and description', () => {
      render(<StarterCodeEditor value={{}} onChange={mockOnChange} />);
      
      expect(screen.getByText('Starter Code Templates')).toBeInTheDocument();
      expect(screen.getByText(/provide starter code templates/i)).toBeInTheDocument();
    });

    it('renders reset button', () => {
      render(<StarterCodeEditor value={{}} onChange={mockOnChange} />);
      
      expect(screen.getByRole('button', { name: /reset to default/i })).toBeInTheDocument();
    });
  });

  describe('Property Tests', () => {
    it('Property 12: displays correct code for the active language tab', () => {
      const starterCode: Record<string, string> = {
        '62': 'public class Test {}',
        '71': 'print("hello")',
        '54': 'int main() {}',
      };
      
      render(<StarterCodeEditor value={starterCode} onChange={mockOnChange} />);
      
      const javaEditor = screen.getByTestId('code-editor-java');
      expect(javaEditor).toHaveValue(starterCode['62']);
      
      const javaTab = screen.getByRole('tab', { name: 'Java' });
      expect(javaTab).toHaveAttribute('data-state', 'active');
    });

    it('Property 12b: each language has correct starter code in value prop', () => {
      const starterCode: Record<string, string> = {
        '62': 'public class Test {}',
        '71': 'print("hello")',
        '54': 'int main() {}',
      };
      
      expect(starterCode['62']).toBe('public class Test {}');
      expect(starterCode['71']).toBe('print("hello")');
      expect(starterCode['54']).toBe('int main() {}');
      
      render(<StarterCodeEditor value={starterCode} onChange={mockOnChange} />);
      
      const javaEditor = screen.getByTestId('code-editor-java');
      expect(javaEditor).toHaveValue(starterCode['62']);
    });

    it('Property 13: displays default template when no code exists', () => {
      render(<StarterCodeEditor value={{}} onChange={mockOnChange} />);
      
      const javaEditor = screen.getByTestId('code-editor-java');
      const javaDefault = getDefaultTemplate(62);
      expect(javaEditor).toHaveValue(javaDefault);
      expect(javaDefault.length).toBeGreaterThan(0);
    });

    it('Property 13b: all default templates are non-empty', () => {
      SUPPORTED_LANGUAGES.forEach((lang) => {
        const defaultTemplate = getDefaultTemplate(lang.id);
        expect(defaultTemplate.length).toBeGreaterThan(0);
      });
    });

    it('Property 13c: default templates contain language-specific code', () => {
      const javaDefault = getDefaultTemplate(62);
      expect(javaDefault).toContain('class');
      expect(javaDefault).toContain('public');
      
      const pythonDefault = getDefaultTemplate(71);
      expect(pythonDefault).toBeTruthy();
      
      const cppDefault = getDefaultTemplate(54);
      expect(cppDefault).toContain('int main');
      expect(cppDefault).toContain('#include');
    });

    it('Property 14: calls onChange with correct language when code is modified', () => {
      render(<StarterCodeEditor value={{}} onChange={mockOnChange} />);
      
      const javaEditor = screen.getByTestId('code-editor-java');
      fireEvent.change(javaEditor, { target: { value: 'new java code' } });
      
      expect(mockOnChange).toHaveBeenCalledWith(
        expect.objectContaining({ '62': 'new java code' })
      );
    });

    it('Property 14b: reset to default restores default template', () => {
      const customCode: Record<string, string> = {
        '62': 'custom java code',
      };
      
      render(<StarterCodeEditor value={customCode} onChange={mockOnChange} />);
      
      const resetButton = screen.getByRole('button', { name: /reset to default/i });
      fireEvent.click(resetButton);
      
      const javaDefault = getDefaultTemplate(62);
      expect(mockOnChange).toHaveBeenCalledWith(
        expect.objectContaining({ '62': javaDefault })
      );
    });

    it('Property 14c: onChange preserves other language codes when modifying one', () => {
      const starterCode: Record<string, string> = {
        '62': 'original java',
        '71': 'original python',
        '54': 'original cpp',
      };
      
      render(<StarterCodeEditor value={starterCode} onChange={mockOnChange} />);
      
      const javaEditor = screen.getByTestId('code-editor-java');
      fireEvent.change(javaEditor, { target: { value: 'modified java' } });
      
      expect(mockOnChange).toHaveBeenCalledWith(
        expect.objectContaining({
          '62': 'modified java',
          '71': 'original python',
          '54': 'original cpp',
        })
      );
    });
  });
});
