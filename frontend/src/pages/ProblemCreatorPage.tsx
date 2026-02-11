import { useNavigate, useParams } from 'react-router-dom';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { ProblemEditor } from '@/components/problem/ProblemEditor';
import type { BreadcrumbItem } from '@/components/layout/Breadcrumbs';

export default function ProblemCreatorPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditMode = !!id;

  const breadcrumbs: BreadcrumbItem[] = [
    { label: 'Dashboard', href: '/dashboard' },
    { label: 'My Problems', href: '/my-problems' },
    { label: isEditMode ? 'Edit Problem' : 'Create Problem' },
  ];

  const handleCancel = () => {
    navigate('/my-problems');
  };

  return (
    <DashboardLayout breadcrumbs={breadcrumbs}>
      <ProblemEditor
        problemId={id ? Number(id) : undefined}
        onCancel={handleCancel}
      />
    </DashboardLayout>
  );
}
