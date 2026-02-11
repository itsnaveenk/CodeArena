import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { MyProblemsList } from '@/components/problem/MyProblemsList';
import type { BreadcrumbItem } from '@/components/layout/Breadcrumbs';

const breadcrumbs: BreadcrumbItem[] = [
  { label: 'My Problems' },
];

export default function MyProblemsPage() {
  return (
    <DashboardLayout breadcrumbs={breadcrumbs}>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">My Problems</h1>
          <p className="text-muted-foreground mt-1">
            Manage your created problems and track their review status
          </p>
        </div>
        <MyProblemsList />
      </div>
    </DashboardLayout>
  );
}
