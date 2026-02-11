import type { PaginatedResponse } from '@/types';

export type AdminAuditAction =
  | 'USER_ROLE_UPDATED'
  | 'PROBLEM_PUBLISHED'
  | 'PROBLEM_REJECTED'
  | 'PROBLEM_ARCHIVED'
  | 'PROBLEM_BULK_PUBLISHED'
  | 'PROBLEM_BULK_REJECTED'
  | 'PROBLEM_BULK_ARCHIVED';

export type AdminAuditTargetType = 'USER' | 'PROBLEM';

export interface AdminAuditEvent {
  id: number;
  actorUserId: number | null;
  actorEmail: string | null;
  action: AdminAuditAction;
  targetType: AdminAuditTargetType | null;
  targetId: string | null;
  metadataJson: string | null;
  createdAt: string;
}

export type AdminAuditEventsResponse = PaginatedResponse<AdminAuditEvent>;
