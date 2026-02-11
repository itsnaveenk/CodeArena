import { useState } from 'react';
import { useProblems } from '@/hooks/useProblems';
import { ProblemCard } from '@/components/problem/ProblemCard';
import { ProblemFilters } from '@/components/problem/ProblemFilters';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import type { ProblemFilter } from '@/types';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const PAGE_SIZE = 20;

export default function ProblemsPage() {
  const [filter, setFilter] = useState<ProblemFilter>({ page: 0, size: PAGE_SIZE });
  const { data, isLoading, isError, error } = useProblems(filter);

  const handlePageChange = (newPage: number) => {
    setFilter((prev) => ({ ...prev, page: newPage }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-3xl font-bold mb-8">Problems</h1>
      
      <ProblemFilters filter={filter} onFilterChange={setFilter} />

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 10 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      ) : isError ? (
        <div className="text-center py-12">
          <p className="text-destructive mb-4">
            {error instanceof Error ? error.message : 'Failed to load problems'}
          </p>
          <Button onClick={() => setFilter({ ...filter })}>Try Again</Button>
        </div>
      ) : data?.content.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground">
          <p>No problems found matching your criteria.</p>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {data?.content.map((problem) => (
              <ProblemCard key={problem.id} problem={problem} />
            ))}
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 mt-8">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(data.page - 1)}
                disabled={data.page === 0}
              >
                <ChevronLeft className="h-4 w-4 mr-1" />
                Previous
              </Button>
              <span className="text-sm text-muted-foreground">
                Page {data.page + 1} of {data.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(data.page + 1)}
                disabled={data.page >= data.totalPages - 1}
              >
                Next
                <ChevronRight className="h-4 w-4 ml-1" />
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
