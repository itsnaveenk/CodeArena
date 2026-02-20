export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
}

export type Role = 'USER' | 'PROBLEM_SETTER' | 'ADMIN';

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  setAuth: (user: User, token: string) => void;
  clearAuth: () => void;
  setLoading: (loading: boolean) => void;
}

export interface ThemeState {
  theme: 'light' | 'dark';
  toggleTheme: () => void;
  setTheme: (theme: 'light' | 'dark') => void;
}

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type ProblemStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'ARCHIVED';
export type SolveStatus = 'SOLVED' | 'ATTEMPTED' | 'NOT_TRIED';

export interface ProblemListItem {
  id: number;
  title: string;
  slug: string;
  difficulty: Difficulty;
  tags: string[];
  solveStatus: SolveStatus;
}

export interface ProblemDetail {
  id: number;
  title: string;
  slug: string;
  statement: string;
  constraints: string;
  difficulty: Difficulty;
  tags: string[];
  status: ProblemStatus;
  examples: Testcase[];
  starterCode: Record<string, string>;
  rejectionReason?: string;
  rejectedAt?: string;
}

export interface ProblemReviewDetail {
  id: number;
  title: string;
  slug: string;
  statement: string;
  constraints: string;
  difficulty: Difficulty;
  tags: string[];
  status: ProblemStatus;
  authorId: number;
  authorName: string;
  authorEmail: string;
  testcases: Testcase[];
  starterCode: Record<string, string>;
  createdAt: string;
  updatedAt?: string;
  testcaseCount: number;
  visibleTestcaseCount: number;
  hiddenTestcaseCount: number;
  submissionCount: number;
  rejectionReason?: string;
  rejectedAt?: string;
  rejectedByName?: string;
}

export interface Testcase {
  id: number;
  input: string;
  expectedOutput: string;
  isHidden: boolean;
}

export type Verdict = 'ACCEPTED' | 'WRONG_ANSWER' | 'TLE' | 'RUNTIME_ERROR' | 'COMPILATION_ERROR';

export interface Submission {
  id: string;
  userId: number;
  problemId: number;
  problemTitle: string;
  languageId: number;
  code: string;
  verdict: Verdict;
  runtime: number;
  memory: number;
  passedTestcases: number;
  totalTestcases: number;
  createdAt: string;
}

export interface UserStats {
  totalSolved: number;
  totalAttempted: number;
  easySolved: number;
  mediumSolved: number;
  hardSolved: number;
}

export interface RunRequest {
  problemId: number;
  languageId: number;
  code: string;
  customInput?: string;
}

export type ExecutionStatus = 'SUCCESS' | 'ACCEPTED' | 'WRONG_ANSWER' | 'COMPILATION_ERROR' | 'RUNTIME_ERROR' | 'TIME_LIMIT_EXCEEDED';

export interface RunResponse {
  stdout: string;
  stderr: string;
  compileOutput: string;
  status: ExecutionStatus;
  time: number;
  memory: number;
  expectedOutput?: string;
}

export interface SubmitRequest {
  problemId: number;
  languageId: number;
  code: string;
}

export interface SubmitResponse {
  submissionId: string;
  verdict: Verdict;
  runtime: number;
  memory: number;
  passedTestcases: number;
  totalTestcases: number;
}

export interface ProblemFilter {
  difficulty?: Difficulty;
  tags?: string[];
  search?: string;
  page?: number;
  size?: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ApiError {
  code: string;
  message: string;
  timestamp: string;
  path: string;
  details?: Record<string, string>;
}

export interface CreateProblemRequest {
  title: string;
  statement: string;
  constraints?: string;
  difficulty: Difficulty;
  tags?: string[];
  starterCode?: Record<string, string>;
}

export interface UpdateProblemRequest {
  title?: string;
  statement?: string;
  constraints?: string;
  difficulty?: Difficulty;
  tags?: string[];
  starterCode?: Record<string, string>;
}

export interface CreateTestcaseRequest {
  input: string;
  expectedOutput: string;
  isHidden: boolean;
}

export const SUPPORTED_LANGUAGES = [
  { id: 62, name: 'Java', monacoLanguage: 'java', extension: '.java' },
  { id: 71, name: 'Python 3', monacoLanguage: 'python', extension: '.py' },
  { id: 54, name: 'C++', monacoLanguage: 'cpp', extension: '.cpp' },
  { id: 63, name: 'JavaScript', monacoLanguage: 'javascript', extension: '.js' },
  { id: 4, name: 'C', monacoLanguage: 'c', extension: '.c' },
  { id: 74, name: 'TypeScript', monacoLanguage: 'typescript', extension: '.ts' },
  { id: 60, name: 'Go', monacoLanguage: 'go', extension: '.go' },
  { id: 73, name: 'Rust', monacoLanguage: 'rust', extension: '.rs' },
] as const;

export type LanguageId = typeof SUPPORTED_LANGUAGES[number]['id'];

export const DIFFICULTY_COLORS = {
  EASY: { bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400' },
  MEDIUM: { bg: 'bg-yellow-100 dark:bg-yellow-900/30', text: 'text-yellow-700 dark:text-yellow-400' },
  HARD: { bg: 'bg-red-100 dark:bg-red-900/30', text: 'text-red-700 dark:text-red-400' },
} as const;

export const VERDICT_COLORS = {
  ACCEPTED: { bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400' },
  WRONG_ANSWER: { bg: 'bg-red-100 dark:bg-red-900/30', text: 'text-red-700 dark:text-red-400' },
  TLE: { bg: 'bg-orange-100 dark:bg-orange-900/30', text: 'text-orange-700 dark:text-orange-400' },
  RUNTIME_ERROR: { bg: 'bg-purple-100 dark:bg-purple-900/30', text: 'text-purple-700 dark:text-purple-400' },
  COMPILATION_ERROR: { bg: 'bg-gray-100 dark:bg-gray-900/30', text: 'text-gray-700 dark:text-gray-400' },
} as const;

export const SOLVE_STATUS_ICONS = {
  SOLVED: { icon: 'CheckCircle', color: 'text-green-500' },
  ATTEMPTED: { icon: 'Circle', color: 'text-yellow-500' },
  NOT_TRIED: { icon: null, color: 'text-gray-300' },
} as const;

export * from './adminAudit';
export * from './contest';
export * from './leaderboard';
