import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import type { CreateContestRequest, UpdateContestRequest, AddContestProblemRequest, ProblemOrderRequest, ContestSubmitRequest } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';
import type { ContestFilter } from '@/types/contest';

export function useContests(filter?: ContestFilter) {
  return useQuery({
    queryKey: queryKeys.contests.list(filter),
    queryFn: () => getApiClient().getContests(filter),
    staleTime: 30_000,
  });
}

export function useContest(id: number) {
  return useQuery({
    queryKey: queryKeys.contests.detail(id),
    queryFn: () => getApiClient().getContest(id),
    staleTime: 30_000,
    enabled: id > 0,
  });
}

export function useContestBySlug(slug: string) {
  return useQuery({
    queryKey: queryKeys.contests.bySlug(slug),
    queryFn: () => getApiClient().getContestBySlug(slug),
    staleTime: 30_000,
    enabled: !!slug,
  });
}

export function useContestProblems(contestId: number) {
  return useQuery({
    queryKey: queryKeys.contests.problems(contestId),
    queryFn: () => getApiClient().getContestProblems(contestId),
    staleTime: 30_000,
    enabled: contestId > 0,
  });
}

export function useContestProblem(contestId: number, problemId: number) {
  return useQuery({
    queryKey: queryKeys.contests.problem(contestId, problemId),
    queryFn: () => getApiClient().getContestProblem(contestId, problemId),
    staleTime: 30_000,
    enabled: contestId > 0 && problemId > 0,
  });
}

export function useContestLeaderboard(contestId: number, page?: number, size?: number) {
  return useQuery({
    queryKey: queryKeys.contests.leaderboard(contestId, page),
    queryFn: () => getApiClient().getContestLeaderboard(contestId, page, size),
    staleTime: 10_000, // Refresh more frequently for live leaderboard
    enabled: contestId > 0,
    refetchInterval: 30_000, // Auto-refresh every 30 seconds
  });
}

export function useMyContestRanking(contestId: number) {
  return useQuery({
    queryKey: queryKeys.contests.myRanking(contestId),
    queryFn: () => getApiClient().getMyContestRanking(contestId),
    staleTime: 10_000,
    enabled: contestId > 0,
  });
}

export function useContestRegistration(contestId: number) {
  return useQuery({
    queryKey: queryKeys.contests.registration(contestId),
    queryFn: () => getApiClient().getContestRegistration(contestId),
    staleTime: 30_000,
    enabled: contestId > 0,
  });
}

export function useContestSubmissions(contestId: number, problemId?: number) {
  return useQuery({
    queryKey: queryKeys.contests.submissions(contestId, problemId),
    queryFn: () => getApiClient().getContestSubmissions(contestId, problemId),
    staleTime: 10_000,
    enabled: contestId > 0,
  });
}

export function useContestInvitations(contestId: number) {
  return useQuery({
    queryKey: queryKeys.contests.invitations(contestId),
    queryFn: () => getApiClient().getContestInvitations(contestId),
    staleTime: 30_000,
    enabled: contestId > 0,
  });
}

export function useContestStatistics(contestId: number) {
  return useQuery({
    queryKey: queryKeys.contests.statistics(contestId),
    queryFn: () => getApiClient().getContestStatistics(contestId),
    staleTime: 60_000,
    enabled: contestId > 0,
  });
}

export function useContestEditorials(contestId: number) {
  return useQuery({
    queryKey: queryKeys.contests.editorials(contestId),
    queryFn: () => getApiClient().getContestEditorials(contestId),
    staleTime: 60_000,
    enabled: contestId > 0,
  });
}

export function useMyContests() {
  return useQuery({
    queryKey: queryKeys.contests.myContests(),
    queryFn: () => getApiClient().getMyContests(),
    staleTime: 30_000,
  });
}


export function useCreateContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (data: CreateContestRequest) => getApiClient().createContest(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.all });
    },
  });
}

export function useUpdateContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateContestRequest }) => 
      getApiClient().updateContest(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.detail(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.all });
    },
  });
}

export function useDeleteContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (id: number) => getApiClient().deleteContest(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.all });
    },
  });
}

export function usePublishContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (id: number) => getApiClient().publishContest(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.detail(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.all });
    },
  });
}

export function useCancelContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (id: number) => getApiClient().cancelContest(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.detail(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.all });
    },
  });
}

export function useAddContestProblem() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ contestId, data }: { contestId: number; data: AddContestProblemRequest }) =>
      getApiClient().addContestProblem(contestId, data),
    onSuccess: (_, { contestId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.problems(contestId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.detail(contestId) });
    },
  });
}

export function useRemoveContestProblem() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ contestId, problemId }: { contestId: number; problemId: number }) =>
      getApiClient().removeContestProblem(contestId, problemId),
    onSuccess: (_, { contestId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.problems(contestId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.detail(contestId) });
    },
  });
}

export function useUpdateContestProblemOrder() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ contestId, data }: { contestId: number; data: ProblemOrderRequest }) =>
      getApiClient().updateContestProblemOrder(contestId, data),
    onSuccess: (_, { contestId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.problems(contestId) });
    },
  });
}

export function useRegisterForContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (contestId: number) => getApiClient().registerForContest(contestId),
    onSuccess: (_, contestId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.registration(contestId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.detail(contestId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.myContests() });
    },
  });
}

export function useWithdrawFromContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (contestId: number) => getApiClient().withdrawFromContest(contestId),
    onSuccess: (_, contestId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.registration(contestId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.detail(contestId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.myContests() });
    },
  });
}

export function useStartContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (contestId: number) => getApiClient().startContest(contestId),
    onSuccess: (_, contestId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.registration(contestId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.detail(contestId) });
    },
  });
}

export function useInviteToContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ contestId, email }: { contestId: number; email: string }) =>
      getApiClient().inviteToContest(contestId, email),
    onSuccess: (_, { contestId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.invitations(contestId) });
    },
  });
}

export function useBulkInviteToContest() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ contestId, emails }: { contestId: number; emails: string[] }) =>
      getApiClient().bulkInviteToContest(contestId, emails),
    onSuccess: (_, { contestId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.invitations(contestId) });
    },
  });
}

export function useRevokeContestInvitation() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ contestId, invitationId }: { contestId: number; invitationId: number }) =>
      getApiClient().revokeContestInvitation(contestId, invitationId),
    onSuccess: (_, { contestId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.invitations(contestId) });
    },
  });
}

export function useAcceptContestInvitation() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (token: string) => getApiClient().acceptContestInvitation(token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.myContests() });
    },
  });
}

export function useDeclineContestInvitation() {
  return useMutation({
    mutationFn: (token: string) => getApiClient().declineContestInvitation(token),
  });
}

export function useContestSubmit() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ contestId, data }: { contestId: number; data: ContestSubmitRequest }) =>
      getApiClient().submitContestSolution(contestId, data),
    onSuccess: (_, { contestId, data }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.submissions(contestId, data.problemId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.leaderboard(contestId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.myRanking(contestId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.problems(contestId) });
    },
  });
}

export function useCreateContestEditorial() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ contestId, problemId, content }: { contestId: number; problemId: number; content: string }) =>
      getApiClient().createContestEditorial(contestId, problemId, content),
    onSuccess: (_, { contestId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.editorials(contestId) });
    },
  });
}

export function useGetDraft(contestId: number, problemId: number) {
  return useQuery({
    queryKey: queryKeys.contests.draft(contestId, problemId),
    queryFn: () => getApiClient().getDraft(contestId, problemId),
    staleTime: 60_000,
    enabled: contestId > 0 && problemId > 0,
  });
}

export function useSaveDraft() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ contestId, problemId, code, languageId }: { contestId: number; problemId: number; code: string; languageId: number }) =>
      getApiClient().saveDraft(contestId, problemId, code, languageId),
    onSuccess: (_, { contestId, problemId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contests.draft(contestId, problemId) });
    },
  });
}
