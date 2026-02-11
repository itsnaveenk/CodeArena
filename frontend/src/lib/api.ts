import type {
  User,
  ProblemListItem,
  ProblemDetail,
  ProblemReviewDetail,
  Testcase,
  Submission,
  UserStats,
  RunRequest,
  RunResponse,
  SubmitRequest,
  SubmitResponse,
  ProblemFilter,
  PaginatedResponse,
  CreateProblemRequest,
  UpdateProblemRequest,
  CreateTestcaseRequest,
  Role,
  ProblemStatus,
  Difficulty,
  Verdict,
  AdminAuditAction,
  LeaderboardEntry,
  LeaderboardPeriod,
} from '@/types';

import type {
  ContestListItem,
  ContestDetail,
  ContestProblem,
  ContestProblemDetail,
  ContestLeaderboardEntry,
  ContestSubmission,
  ContestRegistration,
  ContestInvitation,
  ContestStatistics,
  ContestFilter,
  ContestVisibility,
  ContestCategory,
  TimerMode,
  ScoringModel,
} from '@/types/contest';

import type { AdminAuditEventsResponse } from '@/types';

export type { PaginatedResponse };

export interface UserListItem {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
  problemsSolved: number;
}

export interface PlatformStats {
  totalUsers: number;
  usersByRole: Record<Role, number>;
  totalProblems: number;
  problemsByStatus: Record<ProblemStatus, number>;
  problemsByDifficulty: Record<Difficulty, number>;
  totalSubmissions: number;
  submissionsToday: number;
  acceptanceRate: number;
}

export interface ProblemManagementItem extends ProblemDetail {
  authorId: number;
  authorName: string;
  testcaseCount: number;
  submissionCount: number;
  createdAt: string;
}

export interface MyProblemsFilter {
  status?: ProblemStatus;
  search?: string;
  page?: number;
  size?: number;
}

export interface UsersFilter {
  role?: Role;
  search?: string;
  page?: number;
  size?: number;
}

export interface AllProblemsFilter {
  status?: ProblemStatus;
  difficulty?: Difficulty;
  search?: string;
  page?: number;
  size?: number;
}

export interface SubmissionsFilter {
  verdict?: Verdict;
  userId?: number;
  problemId?: number;
  page?: number;
  size?: number;
}

export interface AuditFilter {
  action?: AdminAuditAction;
  actorUserId?: number;
  page?: number;
  size?: number;
}

export interface CreateContestRequest {
  title: string;
  description: string;
  visibility: ContestVisibility;
  category: ContestCategory;
  timerMode: TimerMode;
  scoringModel: ScoringModel;
  startTime: string;
  endTime: string;
  durationMinutes?: number;
  registrationStartTime: string;
  registrationEndTime: string;
  maxParticipants?: number;
  leaderboardFreezeMinutes?: number;
}

export interface UpdateContestRequest {
  title?: string;
  description?: string;
  visibility?: ContestVisibility;
  category?: ContestCategory;
  timerMode?: TimerMode;
  scoringModel?: ScoringModel;
  startTime?: string;
  endTime?: string;
  durationMinutes?: number;
  registrationStartTime?: string;
  registrationEndTime?: string;
  maxParticipants?: number;
  leaderboardFreezeMinutes?: number;
}

export interface AddContestProblemRequest {
  problemId: number;
  pointValue: number;
}

export interface ProblemOrderRequest {
  problemOrders: Array<{ problemId: number; displayOrder: number }>;
}

export interface ContestSubmitRequest {
  problemId: number;
  code: string;
  languageId: number;
}

export interface BulkInviteRequest {
  emails: string[];
}

export interface BulkInviteResult {
  email: string;
  success: boolean;
  message?: string;
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
}

export interface ContestEditorial {
  id: number;
  contestId: number;
  problemId: number;
  content: string;
  createdAt: string;
  updatedAt: string;
}

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export class ApiClientError extends Error {
  code: string;
  retryAfter?: number;

  constructor(code: string, message: string, retryAfter?: number) {
    super(message);
    this.code = code;
    this.retryAfter = retryAfter;
    this.name = 'ApiClientError';
  }
}

interface ApiClientConfig {
  getToken: () => string | null;
  onUnauthorized: () => void;
}

class ApiClient {
  private config: ApiClientConfig;

  constructor(config: ApiClientConfig) {
    this.config = config;
  }

  private async request<T>(
    method: string,
    path: string,
    options?: {
      body?: unknown;
      params?: Record<string, string>;
      requiresAuth?: boolean;
    }
  ): Promise<T> {
    const url = new URL(`${API_BASE_URL}${path}`);
    if (options?.params) {
      Object.entries(options.params).forEach(([key, value]) => {
        if (value !== undefined && value !== '') {
          url.searchParams.append(key, value);
        }
      });
    }

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    if (options?.requiresAuth !== false) {
      const token = this.config.getToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }

    const response = await fetch(url.toString(), {
      method,
      headers,
      body: options?.body ? JSON.stringify(options.body) : undefined,
    });

    if (response.status === 401) {
      this.config.onUnauthorized();
      throw new ApiClientError('UNAUTHORIZED', 'Session expired. Please log in again.');
    }

    if (response.status === 403) {
      throw new ApiClientError('FORBIDDEN', 'You do not have permission to perform this action.');
    }

    if (response.status === 429) {
      const retryAfter = parseInt(response.headers.get('Retry-After') || '60', 10);
      throw new ApiClientError('RATE_LIMITED', `Too many requests. Try again in ${retryAfter} seconds.`, retryAfter);
    }

    if (!response.ok) {
      let errorData;
      try {
        errorData = await response.json();
      } catch {
        errorData = { code: 'ERROR', message: 'An error occurred' };
      }
      throw new ApiClientError(errorData.code || 'ERROR', errorData.message || 'An error occurred');
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json();
  }

  signup(data: { name: string; email: string; password: string }) {
    return this.request<{ token: string; user: User }>('POST', '/auth/signup', {
      body: data,
      requiresAuth: false,
    });
  }

  login(data: { email: string; password: string }) {
    return this.request<{ token: string; user: User }>('POST', '/auth/login', {
      body: data,
      requiresAuth: false,
    });
  }

  getProblems(filter: ProblemFilter) {
    const params: Record<string, string> = {};
    if (filter.difficulty) params.difficulty = filter.difficulty;
    if (filter.tags?.length) params.tags = filter.tags.join(',');
    if (filter.search) params.search = filter.search;
    if (filter.page !== undefined) params.page = String(filter.page);
    if (filter.size !== undefined) params.size = String(filter.size);

    return this.request<PaginatedResponse<ProblemListItem>>('GET', '/problems', { params });
  }

  getProblem(id: number) {
    return this.request<ProblemDetail>('GET', `/problems/${id}`);
  }

  getProblemBySlug(slug: string) {
    return this.request<ProblemDetail>('GET', `/problems/slug/${slug}`);
  }

  createProblem(data: CreateProblemRequest) {
    return this.request<ProblemDetail>('POST', '/problems', { body: data });
  }

  updateProblem(id: number, data: UpdateProblemRequest) {
    return this.request<ProblemDetail>('PUT', `/problems/${id}`, { body: data });
  }

  requestReview(id: number) {
    return this.request<void>('POST', `/problems/${id}/request-review`);
  }

  addTestcase(problemId: number, data: CreateTestcaseRequest) {
    return this.request<Testcase>('POST', `/problems/${problemId}/testcases`, { body: data });
  }

  getTestcases(problemId: number) {
    return this.request<Testcase[]>('GET', `/problems/${problemId}/testcases`);
  }

  deleteTestcase(testcaseId: number) {
    return this.request<void>('DELETE', `/testcases/${testcaseId}`);
  }

  runCode(data: RunRequest) {
    return this.request<RunResponse>('POST', '/execute/run', { body: data });
  }

  submitCode(data: SubmitRequest) {
    return this.request<SubmitResponse>('POST', '/execute/submit', { body: data });
  }

  runCodeAsync(data: RunRequest) {
    return this.request<{ taskId: string }>('POST', '/execute/run-async', { body: data });
  }

  submitCodeAsync(data: SubmitRequest) {
    return this.request<{ taskId: string }>('POST', '/execute/submit-async', { body: data });
  }

  getExecutionResult(taskId: string) {
    return this.request<{
      taskId: string;
      status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED';
      runResult?: RunResponse;
      submitResult?: SubmitResponse;
      errorMessage?: string;
      createdAt: string;
      completedAt?: string;
    }>('GET', `/execute/result/${taskId}`);
  }

  getCurrentUser() {
    return this.request<User>('GET', '/me');
  }

  getUserStats() {
    return this.request<UserStats>('GET', '/me/stats');
  }

  getUserSubmissions(params?: { problemId?: number; page?: number; size?: number }) {
    const queryParams: Record<string, string> = {};
    if (params?.problemId) queryParams.problemId = String(params.problemId);
    if (params?.page !== undefined) queryParams.page = String(params.page);
    if (params?.size !== undefined) queryParams.size = String(params.size);

    return this.request<PaginatedResponse<Submission>>('GET', '/me/submissions', { params: queryParams });
  }

  getPendingProblems(page?: number, size?: number) {
    const params: Record<string, string> = {};
    if (page !== undefined) params.page = String(page);
    if (size !== undefined) params.size = String(size);

    return this.request<PaginatedResponse<ProblemDetail>>('GET', '/admin/problems/pending', { params });
  }

  publishProblem(id: number) {
    return this.request<void>('POST', `/admin/problems/${id}/publish`);
  }

  rejectProblem(id: number, reason: string) {
    return this.request<void>('POST', `/admin/problems/${id}/reject`, { body: { reason } });
  }

  getProblemForReview(id: number) {
    return this.request<ProblemReviewDetail>('GET', `/admin/problems/${id}/review`);
  }

  archiveProblem(id: number) {
    return this.request<void>('POST', `/admin/problems/${id}/archive`);
  }

  updateUserRole(userId: number, role: Role) {
    return this.request<void>('PUT', `/admin/users/${userId}/role`, { body: { role } });
  }

  getAllSubmissions(params?: { page?: number; size?: number }) {
    const queryParams: Record<string, string> = {};
    if (params?.page !== undefined) queryParams.page = String(params.page);
    if (params?.size !== undefined) queryParams.size = String(params.size);

    return this.request<PaginatedResponse<Submission>>('GET', '/admin/submissions', { params: queryParams });
  }

  getMyProblems(filter?: MyProblemsFilter) {
    const params: Record<string, string> = {};
    if (filter?.status) params.status = filter.status;
    if (filter?.search) params.search = filter.search;
    if (filter?.page !== undefined) params.page = String(filter.page);
    if (filter?.size !== undefined) params.size = String(filter.size);

    return this.request<PaginatedResponse<ProblemDetail>>('GET', '/me/problems', { params });
  }

  updateTestcase(testcaseId: number, data: CreateTestcaseRequest) {
    return this.request<Testcase>('PUT', `/testcases/${testcaseId}`, { body: data });
  }

  deleteProblem(id: number) {
    return this.request<void>('DELETE', `/problems/${id}`);
  }

  getUsers(filter?: UsersFilter) {
    const params: Record<string, string> = {};
    if (filter?.role) params.role = filter.role;
    if (filter?.search) params.search = filter.search;
    if (filter?.page !== undefined) params.page = String(filter.page);
    if (filter?.size !== undefined) params.size = String(filter.size);

    return this.request<PaginatedResponse<UserListItem>>('GET', '/admin/users', { params });
  }

  getAdminStats() {
    return this.request<PlatformStats>('GET', '/admin/stats');
  }

  getAllProblemsAdmin(filter?: AllProblemsFilter) {
    const params: Record<string, string> = {};
    if (filter?.status) params.status = filter.status;
    if (filter?.difficulty) params.difficulty = filter.difficulty;
    if (filter?.search) params.search = filter.search;
    if (filter?.page !== undefined) params.page = String(filter.page);
    if (filter?.size !== undefined) params.size = String(filter.size);

    return this.request<PaginatedResponse<ProblemManagementItem>>('GET', '/admin/problems', { params });
  }

  getSubmissionsAdmin(filter?: SubmissionsFilter) {
    const params: Record<string, string> = {};
    if (filter?.verdict) params.verdict = filter.verdict;
    if (filter?.userId !== undefined) params.userId = String(filter.userId);
    if (filter?.problemId !== undefined) params.problemId = String(filter.problemId);
    if (filter?.page !== undefined) params.page = String(filter.page);
    if (filter?.size !== undefined) params.size = String(filter.size);

    return this.request<PaginatedResponse<Submission>>('GET', '/admin/submissions', { params });
  }

  getAdminAuditEvents(filter?: AuditFilter) {
    const params: Record<string, string> = {};
    if (filter?.action) params.action = filter.action;
    if (filter?.actorUserId !== undefined) params.actorUserId = String(filter.actorUserId);
    if (filter?.page !== undefined) params.page = String(filter.page);
    if (filter?.size !== undefined) params.size = String(filter.size);

    return this.request<AdminAuditEventsResponse>('GET', '/admin/audit', { params });
  }

  getLeaderboards(params?: { period?: LeaderboardPeriod; limit?: number }) {
    const queryParams: Record<string, string> = {};
    if (params?.period) queryParams.period = params.period;
    if (params?.limit !== undefined) queryParams.limit = String(params.limit);

    return this.request<LeaderboardEntry[]>('GET', '/leaderboards', { params: queryParams, requiresAuth: false });
  }

  bulkPublishProblems(ids: number[]) {
    return this.request<void>('POST', '/admin/problems/bulk/publish', { body: { ids } });
  }

  bulkRejectProblems(ids: number[]) {
    return this.request<void>('POST', '/admin/problems/bulk/reject', { body: { ids } });
  }

  bulkArchiveProblems(ids: number[]) {
    return this.request<void>('POST', '/admin/problems/bulk/archive', { body: { ids } });
  }

  getContests(filter?: ContestFilter) {
    const params: Record<string, string> = {};
    if (filter?.status) params.status = filter.status;
    if (filter?.search) params.search = filter.search;
    if (filter?.page !== undefined) params.page = String(filter.page);
    if (filter?.size !== undefined) params.size = String(filter.size);

    return this.request<PaginatedResponse<ContestListItem>>('GET', '/contests', { params });
  }

  getContest(id: number) {
    return this.request<ContestDetail>('GET', `/contests/${id}`);
  }

  getContestBySlug(slug: string) {
    return this.request<ContestDetail>('GET', `/contests/slug/${slug}`);
  }

  createContest(data: CreateContestRequest) {
    return this.request<ContestDetail>('POST', '/contests', { body: data });
  }

  updateContest(id: number, data: UpdateContestRequest) {
    return this.request<ContestDetail>('PUT', `/contests/${id}`, { body: data });
  }

  deleteContest(id: number) {
    return this.request<void>('DELETE', `/contests/${id}`);
  }

  publishContest(id: number) {
    return this.request<void>('POST', `/contests/${id}/publish`);
  }

  cancelContest(id: number) {
    return this.request<void>('POST', `/contests/${id}/cancel`);
  }

  validateContest(id: number) {
    return this.request<ValidationResult>('GET', `/contests/${id}/validate`);
  }

  getContestProblems(contestId: number) {
    return this.request<ContestProblem[]>('GET', `/contests/${contestId}/problems`);
  }

  getContestProblem(contestId: number, problemId: number) {
    return this.request<ContestProblemDetail>('GET', `/contests/${contestId}/problems/${problemId}`);
  }

  addContestProblem(contestId: number, data: AddContestProblemRequest) {
    return this.request<ContestProblem>('POST', `/contests/${contestId}/problems`, { body: data });
  }

  removeContestProblem(contestId: number, problemId: number) {
    return this.request<void>('DELETE', `/contests/${contestId}/problems/${problemId}`);
  }

  updateContestProblemOrder(contestId: number, data: ProblemOrderRequest) {
    return this.request<void>('PUT', `/contests/${contestId}/problems/order`, { body: data });
  }

  registerForContest(contestId: number) {
    return this.request<ContestRegistration>('POST', `/contests/${contestId}/register`);
  }

  withdrawFromContest(contestId: number) {
    return this.request<void>('DELETE', `/contests/${contestId}/register`);
  }

  startContest(contestId: number) {
    return this.request<ContestRegistration>('POST', `/contests/${contestId}/start`);
  }

  getContestRegistration(contestId: number) {
    return this.request<ContestRegistration>('GET', `/contests/${contestId}/registration`);
  }

  inviteToContest(contestId: number, email: string) {
    return this.request<ContestInvitation>('POST', `/contests/${contestId}/invitations`, { body: { email } });
  }

  bulkInviteToContest(contestId: number, emails: string[]) {
    return this.request<BulkInviteResult[]>('POST', `/contests/${contestId}/invitations/bulk`, { body: { emails } });
  }

  getContestInvitations(contestId: number) {
    return this.request<ContestInvitation[]>('GET', `/contests/${contestId}/invitations`);
  }

  revokeContestInvitation(contestId: number, invitationId: number) {
    return this.request<void>('DELETE', `/contests/${contestId}/invitations/${invitationId}`);
  }

  acceptContestInvitation(token: string) {
    return this.request<ContestRegistration>('POST', `/contests/invitations/${token}/accept`);
  }

  declineContestInvitation(token: string) {
    return this.request<void>('POST', `/contests/invitations/${token}/decline`);
  }

  submitContestSolution(contestId: number, data: ContestSubmitRequest) {
    return this.request<ContestSubmission>('POST', `/contests/${contestId}/submissions`, { body: data });
  }

  getContestSubmissions(contestId: number, problemId?: number) {
    const params: Record<string, string> = {};
    if (problemId !== undefined) params.problemId = String(problemId);

    return this.request<ContestSubmission[]>('GET', `/contests/${contestId}/submissions`, { params });
  }

  saveDraft(contestId: number, problemId: number, code: string, languageId: number) {
    return this.request<void>('PUT', `/contests/${contestId}/problems/${problemId}/draft`, {
      body: { code, languageId },
    });
  }

  getDraft(contestId: number, problemId: number) {
    return this.request<{ code: string; languageId: number; savedAt: string } | null>(
      'GET', `/contests/${contestId}/problems/${problemId}/draft`
    );
  }

  getContestLeaderboard(contestId: number, page?: number, size?: number) {
    const params: Record<string, string> = {};
    if (page !== undefined) params.page = String(page);
    if (size !== undefined) params.size = String(size);

    return this.request<PaginatedResponse<ContestLeaderboardEntry>>('GET', `/contests/${contestId}/leaderboard`, { params });
  }

  getMyContestRanking(contestId: number) {
    return this.request<ContestLeaderboardEntry>('GET', `/contests/${contestId}/leaderboard/me`);
  }

  getContestStatistics(contestId: number) {
    return this.request<ContestStatistics>('GET', `/contests/${contestId}/statistics`);
  }

  getContestEditorials(contestId: number) {
    return this.request<ContestEditorial[]>('GET', `/contests/${contestId}/editorials`);
  }

  createContestEditorial(contestId: number, problemId: number, content: string) {
    return this.request<ContestEditorial>('POST', `/contests/${contestId}/editorials`, { body: { problemId, content } });
  }

  getMyContests() {
    return this.request<ContestListItem[]>('GET', '/me/contests');
  }
}

let apiClientInstance: ApiClient | null = null;

export const initializeApiClient = (config: ApiClientConfig) => {
  apiClientInstance = new ApiClient(config);
  return apiClientInstance;
};

export const getApiClient = () => {
  if (!apiClientInstance) {
    throw new Error('API client not initialized. Call initializeApiClient first.');
  }
  return apiClientInstance;
};

export { ApiClient };
