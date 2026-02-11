import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { AdminOverview } from '@/components/admin/AdminOverview';
import type { BreadcrumbItem } from '@/components/layout/Breadcrumbs';

const breadcrumbs: BreadcrumbItem[] = [{ label: 'Admin', href: '/admin' }];

export default function AdminDashboardPage() {
  return (
    <DashboardLayout breadcrumbs={breadcrumbs}>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Admin</h1>
          <p className="text-muted-foreground mt-1">
            Platform overview and admin tools
          </p>
        </div>

        <AdminOverview />
      </div>
    </DashboardLayout>
  );
}
