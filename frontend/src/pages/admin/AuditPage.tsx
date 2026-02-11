import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { DashboardLayout } from '@/components/layout/DashboardLayout';
import type { BreadcrumbItem } from '@/components/layout/Breadcrumbs';
import { getApiClient } from '@/lib/api';
import type { AdminAuditAction, AdminAuditEvent } from '@/types';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';

const breadcrumbs: BreadcrumbItem[] = [
  { label: 'Admin', href: '/admin' },
  { label: 'Audit Log', href: '/admin/audit' },
];

const ACTIONS: AdminAuditAction[] = [
  'USER_ROLE_UPDATED',
  'PROBLEM_PUBLISHED',
  'PROBLEM_REJECTED',
  'PROBLEM_ARCHIVED',
  'PROBLEM_BULK_PUBLISHED',
  'PROBLEM_BULK_REJECTED',
  'PROBLEM_BULK_ARCHIVED',
];

function formatAction(action: AdminAuditAction) {
  return action
    .toLowerCase()
    .split('_')
    .map(w => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

function safeJsonPretty(json: string | null) {
  if (!json) return '';
  try {
    return JSON.stringify(JSON.parse(json), null, 2);
  } catch {
    return json;
  }
}

function AuditRow({ event }: { event: AdminAuditEvent }) {
  return (
    <div className="border-b last:border-b-0 py-3">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <div className="font-medium">{formatAction(event.action)}</div>
          <div className="text-sm text-muted-foreground">
            {event.actorEmail || 'Unknown'}
            {event.targetType ? ` • ${event.targetType}` : ''}
            {event.targetId ? `:${event.targetId}` : ''}
          </div>
          {event.metadataJson ? (
            <pre className="mt-2 max-h-40 overflow-auto rounded bg-muted p-3 text-xs">
              {safeJsonPretty(event.metadataJson)}
            </pre>
          ) : null}
        </div>
        <div className="text-xs text-muted-foreground whitespace-nowrap">
          {new Date(event.createdAt).toLocaleString()}
        </div>
      </div>
    </div>
  );
}

export default function AdminAuditPage() {
  const [action, setAction] = useState<AdminAuditAction | 'ALL'>('ALL');
  const [actorUserId, setActorUserId] = useState<string>('');
  const [page, setPage] = useState(0);

  const parsedActorUserId = useMemo(() => {
    const trimmed = actorUserId.trim();
    if (!trimmed) return undefined;
    const n = Number(trimmed);
    return Number.isFinite(n) ? n : undefined;
  }, [actorUserId]);

  const queryKey = ['admin-audit', { action, actorUserId: parsedActorUserId, page }];

  const { data, isLoading, error } = useQuery({
    queryKey,
    queryFn: () =>
      getApiClient().getAdminAuditEvents({
        action: action === 'ALL' ? undefined : action,
        actorUserId: parsedActorUserId,
        page,
        size: 20,
      }),
  });

  return (
    <DashboardLayout breadcrumbs={breadcrumbs}>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Audit Log</h1>
          <p className="text-muted-foreground mt-1">Admin activity history</p>
        </div>

        <Card className="p-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-end">
            <div className="space-y-1">
              <div className="text-sm font-medium">Action</div>
              <Select
                value={action}
                onValueChange={v => {
                  setAction(v as AdminAuditAction | 'ALL');
                  setPage(0);
                }}
              >
                <SelectTrigger className="w-[260px]">
                  <SelectValue placeholder="All actions" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All actions</SelectItem>
                  {ACTIONS.map(a => (
                    <SelectItem key={a} value={a}>
                      {formatAction(a)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1">
              <div className="text-sm font-medium">Actor User ID</div>
              <Input
                className="w-[180px]"
                placeholder="e.g. 1"
                value={actorUserId}
                onChange={e => {
                  setActorUserId(e.target.value);
                  setPage(0);
                }}
              />
            </div>

            <div className="flex gap-2">
              <Button
                variant="secondary"
                onClick={() => {
                  setAction('ALL');
                  setActorUserId('');
                  setPage(0);
                }}
              >
                Reset
              </Button>
            </div>
          </div>
        </Card>

        <Card className="p-4">
          {isLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-5 w-2/3" />
              <Skeleton className="h-5 w-1/2" />
              <Skeleton className="h-5 w-3/4" />
            </div>
          ) : error ? (
            <div className="text-sm text-red-600">
              Failed to load audit log.
              {error instanceof Error ? ` ${error.message}` : null}
            </div>
          ) : (
            <>
              <div className="space-y-0">
                {(data?.content ?? []).map(e => (
                  <AuditRow key={e.id} event={e} />
                ))}
                {data?.content?.length === 0 ? (
                  <div className="text-sm text-muted-foreground py-6 text-center">No audit events.</div>
                ) : null}
              </div>

              <div className="mt-4 flex items-center justify-between">
                <div className="text-xs text-muted-foreground">
                  Page {data ? data.page + 1 : page + 1} of {data?.totalPages ?? 1}
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    disabled={page <= 0}
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                  >
                    Prev
                  </Button>
                  <Button
                    variant="outline"
                    disabled={data ? page >= data.totalPages - 1 : true}
                    onClick={() => setPage(p => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </Card>
      </div>
    </DashboardLayout>
  );
}
