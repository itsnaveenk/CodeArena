import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import { DifficultyBadge } from '@/components/problem/DifficultyBadge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { ProblemReviewDialog } from './ProblemReviewDialog';
import { ChevronLeft, ChevronRight, Eye } from 'lucide-react';

const PAGE_SIZE = 10;

export function PendingProblemTable() {
  const [page, setPage] = useState(0);
  const [selectedProblemId, setSelectedProblemId] = useState<number | null>(null);
  const [reviewDialogOpen, setReviewDialogOpen] = useState(false);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['admin', 'pending', page],
    queryFn: () => getApiClient().getPendingProblems(page, PAGE_SIZE),
  });

  const handleReviewClick = (problemId: number) => {
    setSelectedProblemId(problemId);
    setReviewDialogOpen(true);
  };

  const handleReviewSuccess = () => {
    refetch();
  };

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full" />
        ))}
      </div>
    );
  }

  if (!data?.content.length) {
    return (
      <p className="text-center text-muted-foreground py-8">
        No pending problems to review.
      </p>
    );
  }

  return (
    <>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>ID</TableHead>
            <TableHead>Title</TableHead>
            <TableHead>Difficulty</TableHead>
            <TableHead>Author</TableHead>
            <TableHead>Submitted</TableHead>
            <TableHead>Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.content.map((problem) => (
            <TableRow key={problem.id}>
              <TableCell>{problem.id}</TableCell>
              <TableCell className="font-medium">{problem.title}</TableCell>
              <TableCell>
                <DifficultyBadge difficulty={problem.difficulty} />
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">
                Author #{problem.id}
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {/* Add created date when available */}
                Recently
              </TableCell>
              <TableCell>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => handleReviewClick(problem.id)}
                >
                  <Eye className="h-4 w-4 mr-1" />
                  Review
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      {data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-4 mt-4">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(p => p - 1)}
            disabled={page === 0}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(p => p + 1)}
            disabled={page === data.totalPages - 1}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}

      <ProblemReviewDialog
        problemId={selectedProblemId}
        open={reviewDialogOpen}
        onOpenChange={setReviewDialogOpen}
        onSuccess={handleReviewSuccess}
      />
    </>
  );
}
