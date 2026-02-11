import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { Breadcrumbs } from '@/components/layout/Breadcrumbs';
import { SubmissionMonitor } from '@/components/admin/SubmissionMonitor';

export default function SubmissionsPage() {
  const breadcrumbItems = [
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'Submission Monitoring', href: '/admin/submissions' }
  ];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <Breadcrumbs items={breadcrumbItems} />

        <div>
          <h1 className="text-3xl font-bold tracking-tight">Submission Monitoring</h1>
          <p className="text-muted-foreground mt-2">
            Monitor and review all code submissions across the platform in real-time.
          </p>
        </div>

        <SubmissionMonitor />
      </div>
    </DashboardLayout>
  );
}
