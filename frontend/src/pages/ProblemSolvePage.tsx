import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useProblemBySlug } from '@/hooks/useProblems';
import { useCodeStorage } from '@/hooks/useCodeStorage';
import { useAsyncExecution } from '@/hooks/useAsyncExecution';
import { CodeEditor } from '@/components/editor/CodeEditor';
import { LanguageSelector } from '@/components/editor/LanguageSelector';
import { ExecutionPanel } from '@/components/editor/ExecutionPanel';
import { ProblemDescription } from '@/components/problem/ProblemDescription';
import { TestcaseDisplay } from '@/components/problem/TestcaseDisplay';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { SUPPORTED_LANGUAGES } from '@/types';
import { Play, Send, RotateCcw } from 'lucide-react';
import { toast } from '@/lib/toast';

export default function ProblemSolvePage() {
  const { slug } = useParams<{ slug: string }>();
  const { data: problem, isLoading, isError } = useProblemBySlug(slug || '');

  const [languageId, setLanguageId] = useState<number>(SUPPORTED_LANGUAGES[0].id);

  const starterCode = problem?.starterCode?.[String(languageId)] || '';
  const { code, setCode, clearCode } = useCodeStorage(
    problem?.id || 0,
    languageId,
    starterCode
  );

  const monacoLanguage = SUPPORTED_LANGUAGES.find(l => l.id === languageId)?.monacoLanguage || 'java';

  const runExecution = useAsyncExecution(
    { problemId: problem?.id || 0, languageId, code },
    'run',
    {
      onSuccess: (result) => {
        if (result.runResult) {
          const isSuccess = result.runResult.status === 'SUCCESS' || result.runResult.status === 'ACCEPTED';
          if (isSuccess) {
            toast.success('Code executed successfully');
          }
        }
      },
      onError: (error) => {
        toast.error(error.message);
      },
    }
  );

  const submitExecution = useAsyncExecution(
    { problemId: problem?.id || 0, languageId, code },
    'submit',
    {
      onSuccess: (result) => {
        if (result.submitResult) {
          if (result.submitResult.verdict === 'ACCEPTED') {
            toast.success('Accepted! All test cases passed.');
          } else {
            toast.error(result.submitResult.verdict.replace(/_/g, ' '));
          }
        }
      },
      onError: (error) => {
        toast.error(error.message);
      },
    }
  );

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        if (e.shiftKey) {
          handleSubmit();
        } else {
          handleRun();
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [code, languageId, problem?.id]);

  const handleRun = useCallback(() => {
    if (!problem || !code.trim()) {
      toast.error('Please write some code first');
      return;
    }
    runExecution.reset();
    submitExecution.reset();
    runExecution.execute();
  }, [problem, code, runExecution, submitExecution]);

  const handleSubmit = useCallback(() => {
    if (!problem || !code.trim()) {
      toast.error('Please write some code first');
      return;
    }
    runExecution.reset();
    submitExecution.reset();
    submitExecution.execute();
  }, [problem, code, runExecution, submitExecution]);

  const handleReset = () => {
    clearCode();
    runExecution.reset();
    submitExecution.reset();
    toast.info('Code reset to starter template');
  };

  const isRunning = runExecution.isLoading || runExecution.isPolling;
  const isSubmitting = submitExecution.isLoading || submitExecution.isPolling;
  const isExecuting = isRunning || isSubmitting;

  if (isLoading) {
    return (
      <div className="h-[calc(100vh-4rem)] flex">
        <div className="w-1/2 p-6 border-r overflow-auto">
          <Skeleton className="h-8 w-64 mb-4" />
          <Skeleton className="h-4 w-full mb-2" />
          <Skeleton className="h-4 w-3/4 mb-2" />
          <Skeleton className="h-64 w-full" />
        </div>
        <div className="w-1/2 flex flex-col">
          <Skeleton className="h-12 w-full" />
          <Skeleton className="flex-1" />
        </div>
      </div>
    );
  }

  if (isError || !problem) {
    return (
      <div className="h-[calc(100vh-4rem)] flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold mb-2">Problem Not Found</h2>
          <p className="text-muted-foreground">The problem you're looking for doesn't exist.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-[calc(100vh-4rem)] flex">
      {/* Left panel - Problem description */}
      <div className="w-1/2 border-r flex flex-col">
        <Tabs defaultValue="description" className="flex-1 flex flex-col">
          <TabsList className="w-full justify-start rounded-none border-b px-4">
            <TabsTrigger value="description">Description</TabsTrigger>
            <TabsTrigger value="examples">Examples</TabsTrigger>
          </TabsList>
          <TabsContent value="description" className="flex-1 overflow-auto p-6 mt-0">
            <ProblemDescription problem={problem} />
          </TabsContent>
          <TabsContent value="examples" className="flex-1 overflow-auto p-6 mt-0">
            <TestcaseDisplay testcases={problem.examples} />
          </TabsContent>
        </Tabs>
      </div>

      {/* Right panel - Code editor */}
      <div className="w-1/2 flex flex-col">
        {/* Editor toolbar */}
        <div className="flex items-center justify-between p-3 border-b">
          <LanguageSelector
            value={languageId}
            onChange={setLanguageId}
            disabled={isExecuting}
          />
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={handleReset}
              disabled={isExecuting}
            >
              <RotateCcw className="h-4 w-4 mr-1" />
              Reset
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleRun}
              disabled={isExecuting}
            >
              <Play className="h-4 w-4 mr-1" />
              Run
            </Button>
            <Button
              size="sm"
              onClick={handleSubmit}
              disabled={isExecuting}
            >
              <Send className="h-4 w-4 mr-1" />
              Submit
            </Button>
          </div>
        </div>

        {/* Code editor */}
        <div className="flex-1 min-h-0">
          <CodeEditor
            value={code}
            onChange={setCode}
            language={monacoLanguage}
          />
        </div>

        {/* Execution results */}
        <div className="h-48 border-t overflow-auto">
          <ExecutionPanel
            executionResult={submitExecution.result || runExecution.result}
            isSubmitting={isSubmitting}
          />
        </div>
      </div>
    </div>
  );
}
