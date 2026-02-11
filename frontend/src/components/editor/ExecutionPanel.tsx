import type { ExecutionResult } from '@/hooks/useAsyncExecution';
import { VerdictBadge } from '@/components/dashboard/VerdictBadge';
import { CheckCircle, XCircle, Clock, Cpu, Loader2 } from 'lucide-react';

interface ExecutionPanelProps {
  executionResult?: ExecutionResult | null;
  isSubmitting?: boolean;
}

export function ExecutionPanel({ executionResult, isSubmitting }: ExecutionPanelProps) {
  if (executionResult) {
    const { status, runResult, submitResult, errorMessage, taskId } = executionResult;

    if (status === 'PENDING' || status === 'PROCESSING') {
      return (
        <div className="p-4 text-center">
          <div className="flex items-center justify-center gap-2 text-muted-foreground mb-2">
            <Loader2 className="h-5 w-5 animate-spin" />
            <span className="font-medium">
              {status === 'PENDING' ? 'Queued for execution...' : 'Executing your code...'}
            </span>
          </div>
          <p className="text-xs text-muted-foreground">
            {isSubmitting ? 'Submitting solution' : 'Running code'} • Task ID: {taskId.substring(0, 8)}
          </p>
        </div>
      );
    }

    if (status === 'FAILED') {
      return (
        <div className="p-4">
          <div className="flex items-center gap-2 mb-2">
            <XCircle className="h-5 w-5 text-red-500" />
            <span className="text-red-500 font-medium">Execution Failed</span>
          </div>
          <div className="text-sm text-muted-foreground">
            {errorMessage || 'An error occurred during execution. Please try again.'}
          </div>
        </div>
      );
    }

    if (status === 'SUCCESS') {
      if (submitResult) {
        return (
          <div className="p-4 space-y-4">
            <div className="flex items-center gap-3">
              <VerdictBadge verdict={submitResult.verdict} />
              {submitResult.verdict === 'ACCEPTED' ? (
                <CheckCircle className="h-5 w-5 text-green-500" />
              ) : (
                <XCircle className="h-5 w-5 text-red-500" />
              )}
            </div>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div className="flex items-center gap-2">
                <CheckCircle className="h-4 w-4 text-muted-foreground" />
                <span>
                  {submitResult.passedTestcases}/{submitResult.totalTestcases} test cases passed
                </span>
              </div>
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-muted-foreground" />
                <span>{submitResult.runtime} ms</span>
              </div>
              <div className="flex items-center gap-2">
                <Cpu className="h-4 w-4 text-muted-foreground" />
                <span>{(submitResult.memory / 1024).toFixed(2)} MB</span>
              </div>
            </div>
          </div>
        );
      }

      if (runResult) {
        const isSuccess = runResult.status === 'SUCCESS' || runResult.status === 'ACCEPTED';
        const statusIcon = isSuccess ? (
          <CheckCircle className="h-5 w-5 text-green-500" />
        ) : (
          <XCircle className="h-5 w-5 text-red-500" />
        );

        return (
          <div className="p-4 space-y-4">
            <div className="flex items-center gap-3">
              {statusIcon}
              <span className={`font-medium ${isSuccess ? 'text-green-500' : 'text-red-500'}`}>
                {runResult.status.replace(/_/g, ' ')}
              </span>
              <div className="flex items-center gap-4 text-sm text-muted-foreground">
                <span className="flex items-center gap-1">
                  <Clock className="h-4 w-4" />
                  {runResult.time} ms
                </span>
                <span className="flex items-center gap-1">
                  <Cpu className="h-4 w-4" />
                  {(runResult.memory / 1024).toFixed(2)} MB
                </span>
              </div>
            </div>

            {runResult.stdout && (
              <div>
                <h4 className="text-sm font-medium mb-2">Output</h4>
                <pre className="text-sm bg-muted p-3 rounded overflow-x-auto whitespace-pre-wrap">
                  {runResult.stdout}
                </pre>
              </div>
            )}

            {runResult.expectedOutput && (
              <div>
                <h4 className="text-sm font-medium mb-2">Expected Output</h4>
                <pre className="text-sm bg-muted p-3 rounded overflow-x-auto whitespace-pre-wrap">
                  {runResult.expectedOutput}
                </pre>
              </div>
            )}

            {runResult.stderr && (
              <div>
                <h4 className="text-sm font-medium mb-2 text-red-500">Error</h4>
                <pre className="text-sm bg-red-50 dark:bg-red-900/20 p-3 rounded overflow-x-auto whitespace-pre-wrap text-red-700 dark:text-red-400">
                  {runResult.stderr}
                </pre>
              </div>
            )}

            {runResult.compileOutput && (
              <div>
                <h4 className="text-sm font-medium mb-2 text-orange-500">Compilation Output</h4>
                <pre className="text-sm bg-orange-50 dark:bg-orange-900/20 p-3 rounded overflow-x-auto whitespace-pre-wrap text-orange-700 dark:text-orange-400">
                  {runResult.compileOutput}
                </pre>
              </div>
            )}
          </div>
        );
      }
    }
  }

  return (
    <div className="p-4 text-center text-muted-foreground">
      Run your code to see results here.
    </div>
  );
}
