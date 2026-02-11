import { useState } from 'react';
import { useContests } from '@/hooks/useContests';
import { ContestCard } from '@/components/contest';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { SearchInput } from '@/components/ui/search-input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import type { ContestFilter, ContestStatus } from '@/types/contest';
import { ChevronLeft, ChevronRight, Trophy } from 'lucide-react';

const PAGE_SIZE = 20;

export default function ContestsPage() {
  const [filter, setFilter] = useState<ContestFilter>({ page: 0, size: PAGE_SIZE });
  const { data, isLoading, isError, error } = useContests(filter);

  const handlePageChange = (newPage: number) => {
    setFilter((prev) => ({ ...prev, page: newPage }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleStatusChange = (status: string) => {
    setFilter((prev) => ({ 
      ...prev, 
      status: status === 'ALL' ? undefined : status as ContestStatus,
      page: 0 
    }));
  };

  const handleSearchChange = (search: string) => {
    setFilter((prev) => ({ ...prev, search, page: 0 }));
  };

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="flex items-center gap-3 mb-8">
        <Trophy className="h-8 w-8" />
        <h1 className="text-3xl font-bold">Contests</h1>
      </div>
      
      <div className="flex flex-wrap gap-4 mb-6">
        <SearchInput
          value={filter.search || ''}
          onChange={handleSearchChange}
          placeholder="Search contests..."
          className="w-64"
        />
        <Select value={filter.status || 'ALL'} onValueChange={handleStatusChange}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Statuses</SelectItem>
            <SelectItem value="PUBLISHED">Upcoming</SelectItem>
            <SelectItem value="RUNNING">Live</SelectItem>
            <SelectItem value="FINISHED">Finished</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 10 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      ) : isError ? (
        <div className="text-center py-12">
          <p className="text-destructive mb-4">
            {error instanceof Error ? error.message : 'Failed to load contests'}
          </p>
          <Button onClick={() => setFilter({ ...filter })}>Try Again</Button>
        </div>
      ) : data?.content.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground">
          <Trophy className="h-12 w-12 mx-auto mb-4 opacity-50" />
          <p>No contests found matching your criteria.</p>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {data?.content.map((contest) => (
              <ContestCard key={contest.id} contest={contest} />
            ))}
          </div>

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
