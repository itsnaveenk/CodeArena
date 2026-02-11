import { useState } from 'react';
import { useAdminSubmissions } from '@/hooks/useAdminSubmissions';
import { DataTable, type ColumnDef } from '@/components/ui/data-table';
import { SingleFilterDropdown } from '@/components/ui/filter-dropdown';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { RefreshCw } from 'lucide-react';
import { SubmissionDetailDialog } from './SubmissionDetailDialog';
import type { Verdict, Submission } from '@/types';
import { VERDICT_COLORS, SUPPORTED_LANGUAGES } from '@/types';

const PAGE_SIZE = 10;

const VERDICT_OPTIONS: { value: Verdict; label: string }[] = [
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'WRONG_ANSWER', label: 'Wrong Answer' },
  { value: 'TLE', label: 'Time Limit Exceeded' },
  { value: 'RUNTIME_ERROR', label: 'Runtime Error' },
  { value: 'COMPILATION_ERROR', label: 'Compilation Error' },
];

const getVerdictLabel = (verdict: Verdict): string => {
  switch (verdict) {
    case 'ACCEPTED':
      return 'Accepted';
    case 'WRONG_ANSWER':
      return 'Wrong Answer';
    case 'TLE':
      return 'TLE';
    case 'RUNTIME_ERROR':
      return 'Runtime Error';
    case 'COMPILATION_ERROR':
      return 'Compile Error';
    default:
      return verdict;
  }
};

const getLanguageName = (languageId: number): string => {
  const lang = SUPPORTED_LANGUAGES.find((l) => l.id === languageId);
  return lang?.name || `Language ${languageId}`;
};

export function SubmissionMonitor() {
  const [page, setPage] = useState(0);
  const [verdictFilter, setVerdictFilter] = useState<Verdict | null>(null);
  const [selectedSubmission, setSelectedSubmission] = useState<Submission | null>(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);

  const { data, isLoading, refetch, isFetching } = useAdminSubmissions({
    page,
    size: PAGE_SIZE,
    verdict: verdictFilter || undefined,
    autoRefresh: true,
    refreshInterval: 30_000,
  });

  const handleViewSubmission = (submission: Submission) => {
    setSelectedSubmission(submission);
    setDetailDialogOpen(true);
  };

  const columns: ColumnDef<Submission>[] = [
    {
      id: 'user',
      header: 'User',
      cell: (row) => (
        <div className="text-sm">
          <span className="font-medium">User #{row.userId}</span>
        </div>
      ),
    },
    {
      id: 'problem',
      header: 'Problem',
      cell: (row) => (
        <div className="max-w-[200px] truncate">
          <span className="font-medium">{row.problemTitle}</span>
        </div>
      ),
    },
    {
      id: 'language',
      header: 'Language',
      cell: (row) => (
        <span className="text-sm">{getLanguageName(row.languageId)}</span>
      ),
    },
    {
      id: 'verdict',
      header: 'Verdict',
      cell: (row) => {
        const colors = VERDICT_COLORS[row.verdict];
        return (
          <Badge
            variant="secondary"
            className={cn(colors.bg, colors.text, 'font-medium')}
          >
            {getVerdictLabel(row.verdict)}
          </Badge>
        );
      },
    },
    {
      id: 'runtime',
      header: 'Runtime',
      cell: (row) => (
        <span className="text-sm font-mono">{row.runtime}ms</span>
      ),
    },
    {
      id: 'memory',
      header: 'Memory',
      cell: (row) => (
        <span className="text-sm font-mono">{(row.memory / 1024).toFixed(1)}MB</span>
      ),
    },
    {
      id: 'testcases',
      header: 'Test Cases',
      cell: (row) => (
        <span className="text-sm">
          {row.passedTestcases}/{row.totalTestcases}
        </span>
      ),
    },
    {
      id: 'createdAt',
      header: 'Submitted',
      cell: (row) => (
        <span className="text-sm text-muted-foreground">
          {new Date(row.createdAt).toLocaleString()}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* Filters and Refresh */}
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="flex flex-col sm:flex-row gap-4">
          <SingleFilterDropdown
            label="Verdict"
            options={VERDICT_OPTIONS}
            value={verdictFilter}
            onChange={setVerdictFilter}
          />
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">
            Auto-refresh: 30s
          </span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            <RefreshCw className={cn('h-4 w-4 mr-2', isFetching && 'animate-spin')} />
            Refresh
          </Button>
        </div>
      </div>

      {/* Table */}
      <DataTable
        columns={columns}
        data={data?.content || []}
        isLoading={isLoading}
        pagination={
          data
            ? {
                page: data.page,
                pageSize: data.size,
                totalPages: data.totalPages,
                totalElements: data.totalElements,
              }
            : undefined
        }
        onPageChange={setPage}
        onRowClick={handleViewSubmission}
        emptyMessage="No submissions found"
      />

      {/* Submission Detail Dialog */}
      <SubmissionDetailDialog
        submission={selectedSubmission}
        open={detailDialogOpen}
        onOpenChange={setDetailDialogOpen}
      />
    </div>
  );
}

export default SubmissionMonitor;
