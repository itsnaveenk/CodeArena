import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { Breadcrumbs } from '@/components/layout/Breadcrumbs';
import { AllProblemsTable } from '@/components/admin/AllProblemsTable';

export default function ProblemsPage() {
  const breadcrumbItems = [
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Problem Management', href: '/admin/problems' }
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <Breadcrumbs items={breadcrumbItems} />

        <div>
          <h1 className="text-3xl font-bold tracking-tight">Problem Management</h1>
          <p className="text-muted-foreground mt-2">
            Review, publish, and manage all coding problems across the platform.
          </p>
        </div>

        <AllProblemsTable />
      </div>
    </DashboardLayout>
  );
}
