import type { Verdict } from '@/types';

export type ContestStatus = 'DRAFT' | 'PUBLISHED' | 'RUNNING' | 'FINISHED' | 'CANCELLED';
export type ContestVisibility = 'PUBLIC' | 'PRIVATE';
export type ContestCategory = 'PRACTICE' | 'RATED';
export type TimerMode = 'GLOBAL' | 'INDIVIDUAL';
export type ScoringModel = 'PARTIAL' | 'BINARY';
export type RegistrationStatus = 'REGISTERED' | 'WITHDRAWN';
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'REVOKED' | 'EXPIRED';
export type MedalType = 'GOLD' | 'SILVER' | 'BRONZE';

export interface ContestListItem {
  id: number;
  title: string;
  slug: string;
  status: ContestStatus;
  visibility: ContestVisibility;
  category: ContestCategory;
  timerMode: TimerMode;
  startTime: string;
  endTime: string;
  durationMinutes?: number;
  registeredCount: number;
  maxParticipants?: number;
  problemCount: number;
  totalPossiblePoints: number;
  createdByName: string;
  isRegistered: boolean;
  registrationStatus?: RegistrationStatus;
}

export interface ContestDetail extends ContestListItem {
  description: string;
  scoringModel: ScoringModel;
  registrationStartTime: string;
  registrationEndTime: string;
  leaderboardFreezeMinutes: number;
  createdBy: { id: number; name: string };
  createdAt: string;
  participantStartTime?: string;
  remainingTimeSeconds?: number;
  canRegister: boolean;
  canStart: boolean;
  canSubmit: boolean;
}

export interface ContestProblem {
  id: number;
  problemId: number;
  title: string;
  pointValue: number;
  displayOrder: number;
  solveCount: number;
  userBestPoints?: number;
  userSubmissionCount: number;
}

export interface ContestProblemDetail extends ContestProblem {
  statement: string;
  constraints: string;
  examples: Array<{ input: string; expectedOutput: string }>;
  starterCode: Record<string, string>;
}

export interface ProblemScore {
  problemId: number;
  points: number;
  solveTime?: string;
}

export interface ContestLeaderboardEntry {
  rank: number;
  userId: number;
  username: string;
  totalPoints: number;
  problemsSolved: number;
  totalTime: string;
  problemScores: ProblemScore[];
  medal?: MedalType;
  isCurrentUser: boolean;
}

export interface ContestSubmission {
  id: string;
  contestId: number;
  problemId: number;
  languageId: number;
  verdict: Verdict;
  runtime: number;
  memory: number;
  passedTestcases: number;
  totalTestcases: number;
  pointsEarned: number;
  submittedAt: string;
}

export interface ContestRegistration {
  contestId: number;
  userId: number;
  status: RegistrationStatus;
  registeredAt: string;
  participantStartTime?: string;
  remainingTimeSeconds?: number;
}

export interface ContestInvitation {
  id: number;
  contestId: number;
  invitedEmail: string;
  status: InvitationStatus;
  invitedAt: string;
}

export interface ContestStatistics {
  totalParticipants: number;
  totalSubmissions: number;
  averageScore: number;
  problemSolveRates: Record<number, number>;
}

export interface ContestFilter {
  status?: ContestStatus;
  search?: string;
  page?: number;
  size?: number;
}

export const CONTEST_STATUS_COLORS = {
  DRAFT: { bg: 'bg-gray-100 dark:bg-gray-800', text: 'text-gray-600 dark:text-gray-400' },
  PUBLISHED: { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-700 dark:text-blue-400' },
  RUNNING: { bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400' },
  FINISHED: { bg: 'bg-purple-100 dark:bg-purple-900/30', text: 'text-purple-700 dark:text-purple-400' },
  CANCELLED: { bg: 'bg-red-100 dark:bg-red-900/30', text: 'text-red-700 dark:text-red-400' },
} as const;

export const MEDAL_ICONS = {
  GOLD: '🥇',
  SILVER: '🥈',
  BRONZE: '🥉',
} as const;
