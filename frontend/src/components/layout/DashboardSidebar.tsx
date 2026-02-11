import { Link, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { cn } from '@/lib/utils';
import type { Role } from '@/types';
import {
  LayoutDashboard,
  FileText,
  PlusCircle,
  Users,
  ClipboardList,
  Activity,
  User,
  type LucideIcon,
} from 'lucide-react';

interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  roles: Role[];
}

type NavSectionId = 'ADMIN' | 'PROBLEMS' | 'ACCOUNT';

interface NavSection {
  id: NavSectionId;
  label: string;
  items: NavItem[];
}

const navSections: NavSection[] = [
  {
    id: 'ADMIN',
    label: 'Admin',
    items: [
      {
        label: 'Admin Overview',
        href: '/admin',
        icon: LayoutDashboard,
        roles: ['ADMIN'],
      },
      {
        label: 'Users',
        href: '/admin/users',
        icon: Users,
        roles: ['ADMIN'],
      },
      {
        label: 'All Problems',
        href: '/admin/problems',
        icon: ClipboardList,
        roles: ['ADMIN'],
      },
      {
        label: 'Submissions',
        href: '/admin/submissions',
        icon: Activity,
        roles: ['ADMIN'],
      },
    ],
  },
  {
    id: 'PROBLEMS',
    label: 'Problems',
    items: [
      {
        label: 'My Problems',
        href: '/my-problems',
        icon: FileText,
        roles: ['PROBLEM_SETTER', 'ADMIN'],
      },
      {
        label: 'Create Problem',
        href: '/create-problem',
        icon: PlusCircle,
        roles: ['PROBLEM_SETTER', 'ADMIN'],
      },
    ],
  },
  {
    id: 'ACCOUNT',
    label: 'Account',
    items: [
      {
        label: 'Dashboard',
        href: '/dashboard',
        icon: User,
        roles: ['USER', 'PROBLEM_SETTER', 'ADMIN'],
      },
    ],
  },
];

interface DashboardSidebarProps {
  onNavigate?: () => void;
}

export function DashboardSidebar({ onNavigate }: DashboardSidebarProps) {
  const location = useLocation();
  const { user } = useAuthStore();

  if (!user) return null;

  const getRoleBadgeColor = (role: Role) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
      case 'PROBLEM_SETTER':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200';
    }
  };

  const getRoleLabel = (role: Role) => {
    switch (role) {
      case 'ADMIN':
        return 'Admin';
      case 'PROBLEM_SETTER':
        return 'Problem Setter';
      default:
        return 'User';
    }
  };

  const isActiveNavItem = (href: string, pathname: string) => {
    if (href === '/admin') {
      return pathname === '/admin' || pathname.startsWith('/admin/');
    }

    if (href === '/dashboard') {
      return pathname === '/dashboard';
    }

    return pathname === href || pathname.startsWith(`${href}/`);
  };

  const visibleSections = navSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => item.roles.includes(user.role)),
    }))
    .filter((section) => section.items.length > 0);

  return (
    <div className="flex flex-col h-full">
      {/* User Info */}
      <div className="p-4 border-b">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
            <User className="h-5 w-5 text-primary" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-medium truncate">{user.name}</p>
            <span
              className={cn(
                'inline-block text-xs px-2 py-0.5 rounded-full',
                getRoleBadgeColor(user.role)
              )}
            >
              {getRoleLabel(user.role)}
            </span>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-4 space-y-5">
        {visibleSections.map((section) => (
          <div key={section.id} className="space-y-1">
            <p className="px-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              {section.label}
            </p>

            <div className="space-y-1">
              {section.items.map((item) => {
                const isActive = isActiveNavItem(item.href, location.pathname);

                return (
                  <Link
                    key={item.href}
                    to={item.href}
                    onClick={onNavigate}
                    className={cn(
                      'flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-primary text-primary-foreground'
                        : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                    )}
                  >
                    <item.icon className="h-4 w-4" />
                    {item.label}
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </nav>
    </div>
  );
}

export function getNavItemsForRole(role: Role): NavItem[] {
  return navSections.flatMap((section) => section.items).filter((item) => item.roles.includes(role));
}

export default DashboardSidebar;
