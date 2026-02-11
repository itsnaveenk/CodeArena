import { lazy, Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from '@/components/ui/sonner';
import { createQueryClient } from '@/lib/queryClient';
import { initializeApiClient } from '@/lib/api';
import { useAuthStore } from '@/stores/authStore';
import { useThemeStore } from '@/stores/themeStore';
import { PageLayout } from '@/components/layout/PageLayout';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { Skeleton } from '@/components/ui/skeleton';

const LandingPage = lazy(() => import('@/pages/LandingPage'));
const LoginPage = lazy(() => import('@/pages/LoginPage'));
const SignupPage = lazy(() => import('@/pages/SignupPage'));
const ProblemsPage = lazy(() => import('@/pages/ProblemsPage'));
const LeaderboardsPage = lazy(() => import('@/pages/LeaderboardsPage'));
const ProblemSolvePage = lazy(() => import('@/pages/ProblemSolvePage'));
const DashboardPage = lazy(() => import('@/pages/DashboardPage'));
const ProblemCreatorPage = lazy(() => import('@/pages/ProblemCreatorPage'));
const AdminDashboardPage = lazy(() => import('@/pages/AdminDashboardPage'));
const MyProblemsPage = lazy(() => import('@/pages/MyProblemsPage'));
const AdminUsersPage = lazy(() => import('@/pages/admin/UsersPage'));
const AdminProblemsPage = lazy(() => import('@/pages/admin/ProblemsPage'));
const AdminSubmissionsPage = lazy(() => import('@/pages/admin/SubmissionsPage'));
const AdminAuditPage = lazy(() => import('@/pages/admin/AuditPage'));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'));

const ContestsPage = lazy(() => import('@/pages/ContestsPage'));
const ContestDetailPage = lazy(() => import('@/pages/ContestDetailPage'));
const ContestProblemPage = lazy(() => import('@/pages/ContestProblemPage'));
const ContestLeaderboardPage = lazy(() => import('@/pages/ContestLeaderboardPage'));
const ContestResultsPage = lazy(() => import('@/pages/ContestResultsPage'));
const CreateContestPage = lazy(() => import('@/pages/CreateContestPage'));
const EditContestPage = lazy(() => import('@/pages/EditContestPage'));
const ManageContestPage = lazy(() => import('@/pages/ManageContestPage'));

const queryClient = createQueryClient();

function PageLoader() {
  return (
    <div className="container mx-auto py-8 px-4">
      <Skeleton className="h-8 w-48 mb-4" />
      <Skeleton className="h-64 w-full" />
    </div>
  );
}

function AppContent() {
  const { theme } = useThemeStore();

  useEffect(() => {
    initializeApiClient({
      getToken: () => useAuthStore.getState().token,
      onUnauthorized: () => useAuthStore.getState().clearAuth(),
    });
  }, []);

  useEffect(() => {
    document.documentElement.classList.remove('light', 'dark');
    document.documentElement.classList.add(theme);
  }, [theme]);

  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route element={<PageLayout />}>
            {/* Public routes */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/leaderboards" element={<LeaderboardsPage />} />

            {/* Protected routes */}
            <Route element={<ProtectedRoute />}>
              <Route path="/problems" element={<ProblemsPage />} />
              <Route path="/problems/:slug" element={<ProblemSolvePage />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              
              {/* Contest routes */}
              <Route path="/contests" element={<ContestsPage />} />
              <Route path="/contests/create" element={<CreateContestPage />} />
              <Route path="/contests/:slug" element={<ContestDetailPage />} />
              <Route path="/contests/:slug/problems/:problemId" element={<ContestProblemPage />} />
              <Route path="/contests/:slug/leaderboard" element={<ContestLeaderboardPage />} />
              <Route path="/contests/:slug/results" element={<ContestResultsPage />} />
              <Route path="/contests/:slug/edit" element={<EditContestPage />} />
              <Route path="/contests/:slug/manage" element={<ManageContestPage />} />
              
              {/* Problem setter routes */}
              <Route element={<RoleGuard allowedRoles={['PROBLEM_SETTER', 'ADMIN']} />}>
                <Route path="/my-problems" element={<MyProblemsPage />} />
                <Route path="/create-problem" element={<ProblemCreatorPage />} />
                <Route path="/edit-problem/:id" element={<ProblemCreatorPage />} />
              </Route>

              {/* Admin routes */}
              <Route element={<RoleGuard allowedRoles={['ADMIN']} />}>
                <Route path="/admin" element={<AdminDashboardPage />} />
                <Route path="/admin/users" element={<AdminUsersPage />} />
                <Route path="/admin/problems" element={<AdminProblemsPage />} />
                <Route path="/admin/submissions" element={<AdminSubmissionsPage />} />
                <Route path="/admin/audit" element={<AdminAuditPage />} />
              </Route>
            </Route>

            {/* 404 */}
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </Suspense>
      <Toaster position="bottom-right" richColors />
    </BrowserRouter>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AppContent />
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
