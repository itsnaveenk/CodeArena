import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';
import { VerdictBadge } from './VerdictBadge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
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
import { SUPPORTED_LANGUAGES } from '@/types';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const PAGE_SIZE = 10;

export function SubmissionHistory() {
  const [page, setPage] = useState(0);
  
  const { data, isLoading } = useQuery({
    queryKey: queryKeys.user.submissions({ page }),
    queryFn: () => getApiClient().getUserSubmissions({ page, size: PAGE_SIZE }),
  });

  const getLanguageName = (id: number) => {
    return SUPPORTED_LANGUAGES.find(l => l.id === id)?.name || 'Unknown';
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Recent Submissions</CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : data?.content.length === 0 ? (
          <p className="text-center text-muted-foreground py-8">
            No submissions yet. Start solving problems!
          </p>
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Problem</TableHead>
                  <TableHead>Verdict</TableHead>
                  <TableHead>Language</TableHead>
                  <TableHead>Runtime</TableHead>
                  <TableHead>Date</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data?.content.map((submission) => (
                  <TableRow key={submission.id}>
                    <TableCell>
                      <Link
                        to={`/problems/${submission.problemId}`}
                        className="hover:underline"
                      >
                        {submission.problemTitle}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <VerdictBadge verdict={submission.verdict} />
                    </TableCell>
                    <TableCell>{getLanguageName(submission.languageId)}</TableCell>
                    <TableCell>{submission.runtime} ms</TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDate(submission.createdAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            {data && data.totalPages > 1 && (
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
                  disabled={page >= data.totalPages - 1}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
