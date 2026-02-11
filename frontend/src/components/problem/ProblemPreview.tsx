import { useMemo } from 'react';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';
import { DifficultyBadge } from './DifficultyBadge';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { Difficulty, Testcase } from '@/types';

interface ProblemPreviewProps {
  title: string;
  difficulty: Difficulty;
  tags: string[];
  statement: string;
  constraints: string;
  examples: Testcase[];
}

marked.setOptions({
  gfm: true,
  breaks: true,
});

const renderer = new marked.Renderer();
renderer.code = ({ text, lang }: { text: string; lang?: string }) => {
  const language = lang && hljs.getLanguage(lang) ? lang : 'plaintext';
  const highlighted = hljs.highlight(text, { language }).value;
  return `<pre class="hljs rounded-md overflow-x-auto"><code class="language-${language}">${highlighted}</code></pre>`;
};

marked.use({ renderer });

export function ProblemPreview({
  title,
  difficulty,
  tags,
  statement,
  constraints,
  examples,
}: ProblemPreviewProps) {
  const renderedStatement = useMemo(() => {
    if (!statement) return '';
    try {
      return marked.parse(statement) as string;
    } catch {
      return statement;
    }
  }, [statement]);

  const visibleExamples = useMemo(
    () => examples.filter((ex) => !ex.isHidden),
    [examples]
  );

  return (
    <div className="space-y-6" data-testid="problem-preview">
      {/* Header with title, difficulty, and tags */}
      <div>
        <div className="flex items-center gap-3 mb-2">
          <h1 className="text-2xl font-bold" data-testid="preview-title">
            {title || 'Untitled Problem'}
          </h1>
          <DifficultyBadge difficulty={difficulty} data-testid="preview-difficulty" />
        </div>
        {tags.length > 0 && (
          <div className="flex flex-wrap gap-1" data-testid="preview-tags">
            {tags.map((tag) => (
              <Badge key={tag} variant="secondary" className="text-xs">
                {tag}
              </Badge>
            ))}
          </div>
        )}
      </div>

      {/* Problem Statement */}
      <div className="prose prose-sm dark:prose-invert max-w-none">
        <h3 className="font-semibold mb-2">Problem Statement</h3>
        <div
          data-testid="preview-statement"
          dangerouslySetInnerHTML={{ __html: renderedStatement }}
        />
      </div>

      {/* Constraints */}
      {constraints && (
        <div>
          <h3 className="font-semibold mb-2">Constraints</h3>
          <div
            className="text-sm text-muted-foreground whitespace-pre-wrap font-mono bg-muted p-3 rounded"
            data-testid="preview-constraints"
          >
            {constraints}
          </div>
        </div>
      )}

      {/* Examples */}
      {visibleExamples.length > 0 && (
        <div>
          <h3 className="font-semibold mb-3">Examples</h3>
          <div className="space-y-4" data-testid="preview-examples">
            {visibleExamples.map((example, index) => (
              <Card key={example.id || index}>
                <CardHeader className="py-3">
                  <CardTitle className="text-sm">Example {index + 1}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3 pt-0">
                  <div>
                    <div className="text-xs font-medium text-muted-foreground mb-1">
                      Input:
                    </div>
                    <pre className="text-sm bg-muted p-2 rounded font-mono whitespace-pre-wrap">
                      {example.input}
                    </pre>
                  </div>
                  <div>
                    <div className="text-xs font-medium text-muted-foreground mb-1">
                      Output:
                    </div>
                    <pre className="text-sm bg-muted p-2 rounded font-mono whitespace-pre-wrap">
                      {example.expectedOutput}
                    </pre>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Empty state for no examples */}
      {visibleExamples.length === 0 && (
        <div className="text-sm text-muted-foreground italic">
          No visible examples added yet.
        </div>
      )}
    </div>
  );
}

export function containsMarkdown(text: string): boolean {
  const markdownPatterns = [
    /\*\*.*?\*\*/,      // Bold
    /\*.*?\*/,          // Italic
    /`.*?`/,            // Inline code
    /```[\s\S]*?```/,   // Code blocks
    /^#+\s/m,           // Headers
    /^\s*[-*+]\s/m,     // Lists
    /^\s*\d+\.\s/m,     // Numbered lists
    /\[.*?\]\(.*?\)/,   // Links
  ];
  
  return markdownPatterns.some((pattern) => pattern.test(text));
}

export function containsSyntaxHighlighting(html: string): boolean {
  return html.includes('class="hljs') || html.includes('class="language-');
}
