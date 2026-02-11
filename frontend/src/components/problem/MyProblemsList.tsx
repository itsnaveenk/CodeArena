import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMyProblems, useDeleteProblem, useRequestReview } from '@/hooks/useMyProblems';
import { DataTable, type ColumnDef } from '@/components/ui/data-table';
import { SearchInput } from '@/components/ui/search-input';
import { SingleFilterDropdown } from '@/components/ui/filter-dropdown';
import { StatusBadge } from '@/components/problem/StatusBadge';
import { DifficultyBadge } from '@/components/problem/DifficultyBadge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { MoreHorizontal, Eye, Edit, Trash2, Send, Plus } from 'lucide-react';
import type { ProblemDetail, ProblemStatus } from '@/types';

const PAGE_SIZE = 10;

const STATUS_OPTIONS: { value: ProblemStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PENDING_REVIEW', label: 'Pending Review' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'ARCHIVED', label: 'Archived' },
];

export function MyProblemsList() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<ProblemStatus | null>(null);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; problem: ProblemDetail | null }>({
    open: false,
    problem: null,
  });
  const [reviewDialog, setReviewDialog] = useState<{ open: boolean; problem: ProblemDetail | null }>({
    open: false,
    problem: null,
  });

  const { data, isLoading } = useMyProblems({
    page,
    size: PAGE_SIZE,
    search: search || undefined,
    status: statusFilter || undefined,
  });

  const deleteMutation = useDeleteProblem();
  const reviewMutation = useRequestReview();

  const handleDelete = () => {
    if (deleteDialog.problem) {
      deleteMutation.mutate(deleteDialog.problem.id, {
        onSuccess: () => setDeleteDialog({ open: false, problem: null }),
      });
    }
  };

  const handleRequestReview = () => {
    if (reviewDialog.problem) {
      reviewMutation.mutate(reviewDialog.problem.id, {
        onSuccess: () => setReviewDialog({ open: false, problem: null }),
      });
    }
  };

  const columns: ColumnDef<ProblemDetail>[] = [
    {
      id: 'title',
      header: 'Title',
      cell: (row) => (
        <div className="font-medium">{row.title}</div>
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
      id: 'tags',
      header: 'Tags',
      cell: (row) => (
        <div className="flex flex-wrap gap-1">
          {row.tags.slice(0, 3).map((tag) => (
            <span
              key={tag}
              className="text-xs bg-muted px-2 py-0.5 rounded"
            >
              {tag}
            </span>
          ))}
          {row.tags.length > 3 && (
            <span className="text-xs text-muted-foreground">
              +{row.tags.length - 3}
            </span>
          )}
        </div>
      ),
    },
    {
      id: 'actions',
      header: '',
      cell: (row) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => navigate(`/problems/${row.slug}`)}>
              <Eye className="h-4 w-4 mr-2" />
              View
            </DropdownMenuItem>
            
            {(row.status === 'DRAFT' || row.status === 'REJECTED') && (
              <DropdownMenuItem onClick={() => navigate(`/edit-problem/${row.id}`)}>
                <Edit className="h-4 w-4 mr-2" />
                Edit
              </DropdownMenuItem>
            )}
            
            {row.status === 'DRAFT' && (
              <>
                <DropdownMenuItem
                  onClick={() => setReviewDialog({ open: true, problem: row })}
                >
                  <Send className="h-4 w-4 mr-2" />
                  Submit for Review
                </DropdownMenuItem>
                <DropdownMenuItem
                  className="text-destructive"
                  onClick={() => setDeleteDialog({ open: true, problem: row })}
                >
                  <Trash2 className="h-4 w-4 mr-2" />
                  Delete
                </DropdownMenuItem>
              </>
            )}
            
            {row.status === 'REJECTED' && (
              <DropdownMenuItem
                onClick={() => setReviewDialog({ open: true, problem: row })}
              >
                <Send className="h-4 w-4 mr-2" />
                Resubmit for Review
              </DropdownMenuItem>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      ),
      className: 'w-12',
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row gap-4 justify-between">
        <div className="flex flex-col sm:flex-row gap-2">
          <SearchInput
            placeholder="Search problems..."
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
        </div>
        <Button onClick={() => navigate('/create-problem')}>
          <Plus className="h-4 w-4 mr-2" />
          Create Problem
        </Button>
      </div>

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
        onRowClick={(row) => {
          if (row.status === 'DRAFT' || row.status === 'REJECTED') {
            navigate(`/edit-problem/${row.id}`);
          }
        }}
        emptyMessage="No problems found. Create your first problem!"
      />

      <ConfirmDialog
        open={deleteDialog.open}
        onOpenChange={(open) => !open && setDeleteDialog({ open: false, problem: null })}
        title="Delete Problem"
        description={`Are you sure you want to delete "${deleteDialog.problem?.title}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="destructive"
        onConfirm={handleDelete}
        isLoading={deleteMutation.isPending}
      />

      <ConfirmDialog
        open={reviewDialog.open}
        onOpenChange={(open) => !open && setReviewDialog({ open: false, problem: null })}
        title="Submit for Review"
        description={`Submit "${reviewDialog.problem?.title}" for admin review? Once submitted, you won't be able to edit it until it's reviewed.`}
        confirmLabel="Submit"
        onConfirm={handleRequestReview}
        isLoading={reviewMutation.isPending}
      />
    </div>
  );
}

export default MyProblemsList;
