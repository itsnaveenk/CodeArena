import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { CodeEditor } from '@/components/editor/CodeEditor';
import type { Submission, Verdict } from '@/types';
import { VERDICT_COLORS, SUPPORTED_LANGUAGES } from '@/types';
import { Clock, Cpu, CheckCircle, XCircle, User, FileText } from 'lucide-react';

interface SubmissionDetailDialogProps {
  submission: Submission | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const getVerdictLabel = (verdict: Verdict): string => {
  switch (verdict) {
    case 'ACCEPTED':
      return 'Accepted';
    case 'WRONG_ANSWER':
      return 'Wrong Answer';
    case 'TLE':
      return 'Time Limit Exceeded';
    case 'RUNTIME_ERROR':
      return 'Runtime Error';
    case 'COMPILATION_ERROR':
      return 'Compilation Error';
    default:
      return verdict;
  }
};

const getLanguageInfo = (languageId: number) => {
  const lang = SUPPORTED_LANGUAGES.find((l) => l.id === languageId);
  return lang || { name: `Language ${languageId}`, monacoLanguage: 'plaintext' };
};

export function SubmissionDetailDialog({
  submission,
  open,
  onOpenChange,
}: SubmissionDetailDialogProps) {
  if (!submission) {
    return null;
  }

  const verdictColors = VERDICT_COLORS[submission.verdict];
  const languageInfo = getLanguageInfo(submission.languageId);
  const isAccepted = submission.verdict === 'ACCEPTED';

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-3">
            Submission Details
            <Badge
              variant="secondary"
              className={cn(verdictColors.bg, verdictColors.text, 'font-medium')}
            >
              {getVerdictLabel(submission.verdict)}
            </Badge>
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Summary Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <Card>
              <CardContent className="pt-4">
                <div className="flex items-center gap-2">
                  <User className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm text-muted-foreground">User</span>
                </div>
                <p className="text-lg font-semibold mt-1">#{submission.userId}</p>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-4">
                <div className="flex items-center gap-2">
                  <FileText className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm text-muted-foreground">Problem</span>
                </div>
                <p className="text-lg font-semibold mt-1 truncate" title={submission.problemTitle}>
                  {submission.problemTitle}
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-4">
                <div className="flex items-center gap-2">
                  <Clock className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm text-muted-foreground">Runtime</span>
                </div>
                <p className="text-lg font-semibold mt-1 font-mono">{submission.runtime}ms</p>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-4">
                <div className="flex items-center gap-2">
                  <Cpu className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm text-muted-foreground">Memory</span>
                </div>
                <p className="text-lg font-semibold mt-1 font-mono">
                  {(submission.memory / 1024).toFixed(1)}MB
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Test Cases Result */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base flex items-center gap-2">
                {isAccepted ? (
                  <CheckCircle className="h-5 w-5 text-green-500" />
                ) : (
                  <XCircle className="h-5 w-5 text-red-500" />
                )}
                Test Cases
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-4">
                <div className="text-2xl font-bold">
                  {submission.passedTestcases}/{submission.totalTestcases}
                </div>
                <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                  <div
                    className={cn(
                      'h-full transition-all',
                      isAccepted ? 'bg-green-500' : 'bg-red-500'
                    )}
                    style={{
                      width: `${(submission.passedTestcases / submission.totalTestcases) * 100}%`,
                    }}
                  />
                </div>
                <span className="text-sm text-muted-foreground">
                  {((submission.passedTestcases / submission.totalTestcases) * 100).toFixed(0)}%
                </span>
              </div>
            </CardContent>
          </Card>

          {/* Submission Info */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Submission Info</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-muted-foreground">Submission ID:</span>
                  <span className="ml-2 font-mono">{submission.id}</span>
                </div>
                <div>
                  <span className="text-muted-foreground">Language:</span>
                  <span className="ml-2">{languageInfo.name}</span>
                </div>
                <div>
                  <span className="text-muted-foreground">Submitted:</span>
                  <span className="ml-2">
                    {new Date(submission.createdAt).toLocaleString()}
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground">Problem ID:</span>
                  <span className="ml-2 font-mono">{submission.problemId}</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Code */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Submitted Code</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="border rounded-md overflow-hidden" style={{ height: '400px' }}>
                <CodeEditor
                  value={submission.code}
                  language={languageInfo.monacoLanguage}
                  onChange={() => {}}
                  readOnly
                />
              </div>
            </CardContent>
          </Card>
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default SubmissionDetailDialog;
