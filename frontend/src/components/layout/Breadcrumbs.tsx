import { Link } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface BreadcrumbsProps {
  items: BreadcrumbItem[];
  className?: string;
}

export function Breadcrumbs({ items, className }: BreadcrumbsProps) {
  if (items.length === 0) return null;

  return (
    <nav
      aria-label="Breadcrumb"
      className={cn('flex items-center text-sm text-muted-foreground', className)}
    >
      <ol className="flex items-center gap-1 flex-wrap">
        {/* Home link */}
        <li className="flex items-center">
          <Link
            to="/dashboard"
            className="hover:text-foreground transition-colors p-1"
            aria-label="Home"
          >
            <Home className="h-4 w-4" />
          </Link>
        </li>

        {items.map((item, index) => {
          const isLast = index === items.length - 1;

          return (
            <li key={item.label} className="flex items-center">
              <ChevronRight className="h-4 w-4 mx-1 flex-shrink-0" />
              {isLast || !item.href ? (
                <span
                  className={cn(
                    'truncate max-w-[150px] md:max-w-[250px]',
                    isLast && 'text-foreground font-medium'
                  )}
                  title={item.label}
                >
                  {item.label}
                </span>
              ) : (
                <Link
                  to={item.href}
                  className="hover:text-foreground transition-colors truncate max-w-[150px] md:max-w-[250px]"
                  title={item.label}
                >
                  {item.label}
                </Link>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

export function generateBreadcrumbs(pathname: string): BreadcrumbItem[] {
  const pathSegments = pathname.split('/').filter(Boolean);
  const breadcrumbs: BreadcrumbItem[] = [];

  const labelMap: Record<string, string> = {
    dashboard: 'Dashboard',
    'my-problems': 'My Problems',
    'create-problem': 'Create Problem',
    'edit-problem': 'Edit Problem',
    admin: 'Admin',
    users: 'Users',
    problems: 'Problems',
    submissions: 'Submissions',
  };

  let currentPath = '';
  for (let i = 0; i < pathSegments.length; i++) {
    const segment = pathSegments[i];
    currentPath += `/${segment}`;

    if (/^\d+$/.test(segment)) {
      continue;
    }

    const label = labelMap[segment] || segment;
    const isLast = i === pathSegments.length - 1;

    breadcrumbs.push({
      label,
      href: isLast ? undefined : currentPath,
    });
  }

  return breadcrumbs;
}

export default Breadcrumbs;
