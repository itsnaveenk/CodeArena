import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useAllProblems,
  usePublishProblem,
  useArchiveProblem,
  useBulkPublish,
  useBulkReject,
  useBulkArchive,
} from '@/hooks/useAllProblems';
import { DataTable, type ColumnDef } from '@/components/ui/data-table';
import { SearchInput } from '@/components/ui/search-input';
import { SingleFilterDropdown } from '@/components/ui/filter-dropdown';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { StatusBadge } from '@/components/problem/StatusBadge';
import { DifficultyBadge } from '@/components/problem/DifficultyBadge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';
import {
  MoreHorizontal,
  Eye,
  Edit,
  CheckCircle,
  XCircle,
  Archive,
  ChevronDown,
} from 'lucide-react';
import type { ProblemStatus, Difficulty } from '@/types';
import type { ProblemManagementItem } from '@/lib/api';

const PAGE_SIZE = 10;

const STATUS_OPTIONS: { value: ProblemStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PENDING_REVIEW', label: 'Pending Review' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'ARCHIVED', label: 'Archived' },
];

const DIFFICULTY_OPTIONS: { value: Difficulty; label: string }[] = [
  { value: 'EASY', label: 'Easy' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HARD', label: 'Hard' },
];

export function AllProblemsTable() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<ProblemStatus | null>(null);
  const [difficultyFilter, setDifficultyFilter] = useState<Difficulty | null>(null);
  const [selectedProblems, setSelectedProblems] = useState<ProblemManagementItem[]>([]);
  const [bulkActionDialog, setBulkActionDialog] = useState<{
    open: boolean;
    action: 'publish' | 'reject' | 'archive' | null;
  }>({ open: false, action: null });

  const { data, isLoading } = useAllProblems({
    page,
    size: PAGE_SIZE,
    search: search || undefined,
    status: statusFilter || undefined,
    difficulty: difficultyFilter || undefined,
  });

  const publishMutation = usePublishProblem();
  const archiveMutation = useArchiveProblem();
  const bulkPublishMutation = useBulkPublish();
  const bulkRejectMutation = useBulkReject();
  const bulkArchiveMutation = useBulkArchive();

  const handleRowClick = (problem: ProblemManagementItem) => {
    navigate(`/edit-problem/${problem.id}`);
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedProblems(data?.content || []);
    } else {
      setSelectedProblems([]);
    }
  };

  const handleSelectProblem = (problem: ProblemManagementItem, checked: boolean) => {
    if (checked) {
      setSelectedProblems((prev) => [...prev, problem]);
    } else {
      setSelectedProblems((prev) => prev.filter((p) => p.id !== problem.id));
    }
  };

  const handleBulkAction = (action: 'publish' | 'reject' | 'archive') => {
    setBulkActionDialog({ open: true, action });
  };

  const confirmBulkAction = () => {
    const ids = selectedProblems.map((p) => p.id);
    switch (bulkActionDialog.action) {
      case 'publish':
        bulkPublishMutation.mutate(ids, {
          onSuccess: () => {
            setBulkActionDialog({ open: false, action: null });
            setSelectedProblems([]);
          },
        });
        break;
      case 'reject':
        bulkRejectMutation.mutate(ids, {
          onSuccess: () => {
            setBulkActionDialog({ open: false, action: null });
            setSelectedProblems([]);
          },
        });
        break;
      case 'archive':
        bulkArchiveMutation.mutate(ids, {
          onSuccess: () => {
            setBulkActionDialog({ open: false, action: null });
            setSelectedProblems([]);
          },
        });
        break;
    }
  };

  const getActionButtons = (problem: ProblemManagementItem) => {
    const actions: React.ReactNode[] = [];

    actions.push(
      <DropdownMenuItem key="view" onClick={() => navigate(`/problems/${problem.slug}`)}>
        <Eye className="h-4 w-4 mr-2" />
        View
      </DropdownMenuItem>
    );

    actions.push(
      <DropdownMenuItem key="edit" onClick={() => navigate(`/edit-problem/${problem.id}`)}>
        <Edit className="h-4 w-4 mr-2" />
        Edit
      </DropdownMenuItem>
    );

    if (problem.status === 'PENDING_REVIEW') {
      actions.push(<DropdownMenuSeparator key="sep1" />);
      actions.push(
        <DropdownMenuItem
          key="publish"
          onClick={() => publishMutation.mutate(problem.id)}
          className="text-green-600"
        >
          <CheckCircle className="h-4 w-4 mr-2" />
          Publish
        </DropdownMenuItem>
      );
    }

    if (problem.status === 'PUBLISHED') {
      actions.push(<DropdownMenuSeparator key="sep2" />);
      actions.push(
        <DropdownMenuItem
          key="archive"
          onClick={() => archiveMutation.mutate(problem.id)}
          className="text-slate-600"
        >
          <Archive className="h-4 w-4 mr-2" />
          Archive
        </DropdownMenuItem>
      );
    }

    return actions;
  };

  const columns: ColumnDef<ProblemManagementItem>[] = [
    {
      id: 'select',
      header: (
        <Checkbox
          checked={
            data?.content?.length
              ? selectedProblems.length === data.content.length
              : false
          }
          onCheckedChange={(checked) => handleSelectAll(!!checked)}
          aria-label="Select all"
        />
      ),
      cell: (row) => (
        <Checkbox
          checked={selectedProblems.some((p) => p.id === row.id)}
          onCheckedChange={(checked) => handleSelectProblem(row, !!checked)}
          aria-label={`Select ${row.title}`}
          onClick={(e) => e.stopPropagation()}
        />
      ),
      className: 'w-12',
    },
    {
      id: 'title',
      header: 'Title',
      cell: (row) => (
        <div>
          <div className="font-medium">{row.title}</div>
          <div className="text-sm text-muted-foreground">by {row.authorName}</div>
        </div>
      ),
    },
    {
      id: 'difficulty',
      header: 'Difficulty',
      cell: (row) => <DifficultyBadge difficulty={row.difficulty} />,
    },
    {
      id: 'status',
      header: 'Status',
      cell: (row) => <StatusBadge status={row.status} />,
    },
    {
      id: 'testcases',
      header: 'Test Cases',
      cell: (row) => row.testcaseCount,
    },
    {
      id: 'createdAt',
      header: 'Created',
      cell: (row) => new Date(row.createdAt).toLocaleDateString(),
    },
    {
      id: 'actions',
      header: '',
      cell: (row) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" onClick={(e) => e.stopPropagation()}>
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            {getActionButtons(row)}
          </DropdownMenuContent>
        </DropdownMenu>
      ),
      className: 'w-12',
    },
  ];

  const isBulkActionLoading =
    bulkPublishMutation.isPending ||
    bulkRejectMutation.isPending ||
    bulkArchiveMutation.isPending;

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-4">
        <SearchInput
          placeholder="Search by title or author..."
          value={search}
          onChange={setSearch}
          className="w-full sm:w-64"
        />
        <SingleFilterDropdown
          label="Status"
          options={STATUS_OPTIONS}
          value={statusFilter}
          onChange={setStatusFilter}
        />
        <SingleFilterDropdown
          label="Difficulty"
          options={DIFFICULTY_OPTIONS}
          value={difficultyFilter}
          onChange={setDifficultyFilter}
        />
      </div>

      {/* Bulk Actions */}
      {selectedProblems.length > 0 && (
        <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
          <span className="text-sm font-medium">
            {selectedProblems.length} selected
          </span>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm">
                Bulk Actions
                <ChevronDown className="h-4 w-4 ml-2" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              <DropdownMenuItem onClick={() => handleBulkAction('publish')}>
                <CheckCircle className="h-4 w-4 mr-2 text-green-600" />
                Publish All
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => handleBulkAction('reject')}>
                <XCircle className="h-4 w-4 mr-2 text-red-600" />
                Reject All
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => handleBulkAction('archive')}>
                <Archive className="h-4 w-4 mr-2 text-slate-600" />
                Archive All
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setSelectedProblems([])}
          >
            Clear Selection
          </Button>
        </div>
      )}

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
        onRowClick={handleRowClick}
        emptyMessage="No problems found"
      />

      {/* Bulk Action Confirmation Dialog */}
      <ConfirmDialog
        open={bulkActionDialog.open}
        onOpenChange={(open) =>
          !open && setBulkActionDialog({ open: false, action: null })
        }
        title={`${bulkActionDialog.action?.charAt(0).toUpperCase()}${bulkActionDialog.action?.slice(1)} ${selectedProblems.length} Problems`}
        description={
          <div className="space-y-2">
            <p>
              Are you sure you want to {bulkActionDialog.action} the following{' '}
              {selectedProblems.length} problem(s)?
            </p>
            <ul className="list-disc list-inside text-sm text-muted-foreground max-h-40 overflow-y-auto">
              {selectedProblems.map((p) => (
                <li key={p.id}>{p.title}</li>
              ))}
            </ul>
          </div>
        }
        confirmLabel={`${bulkActionDialog.action?.charAt(0).toUpperCase()}${bulkActionDialog.action?.slice(1)} All`}
        onConfirm={confirmBulkAction}
        isLoading={isBulkActionLoading}
        variant={bulkActionDialog.action === 'reject' ? 'destructive' : 'default'}
      />
    </div>
  );
}

export default AllProblemsTable;
