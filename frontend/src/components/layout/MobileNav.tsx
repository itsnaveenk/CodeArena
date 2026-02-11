import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { LogOut, User, Code, LayoutDashboard, Shield, PlusCircle, Trophy } from 'lucide-react';

interface MobileNavProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function MobileNav({ open, onOpenChange }: MobileNavProps) {
  const navigate = useNavigate();
  const { user, clearAuth } = useAuthStore();

  const handleLogout = () => {
    clearAuth();
    onOpenChange(false);
    navigate('/login');
  };

  const handleNavigation = (path: string) => {
    onOpenChange(false);
    navigate(path);
  };

  const canCreateProblem = user?.role === 'PROBLEM_SETTER' || user?.role === 'ADMIN';
  const isAdmin = user?.role === 'ADMIN';

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="left" className="w-[280px]">
        <SheetHeader>
          <SheetTitle>CodeArena</SheetTitle>
        </SheetHeader>
        <div className="flex flex-col gap-2 mt-6">
          {/* User info */}
          <div className="flex items-center gap-3 px-2 py-3 border-b mb-2">
            <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center">
              <User className="h-5 w-5" />
            </div>
            <div>
              <p className="font-medium">{user?.name}</p>
              <p className="text-sm text-muted-foreground">{user?.email}</p>
            </div>
          </div>

          {/* Navigation links */}
          <Button
            variant="ghost"
            className="justify-start"
            onClick={() => handleNavigation('/problems')}
          >
            <Code className="h-4 w-4 mr-3" />
            Problems
          </Button>

          <Button
            variant="ghost"
            className="justify-start"
            onClick={() => handleNavigation('/contests')}
          >
            <Trophy className="h-4 w-4 mr-3" />
            Contests
          </Button>

          <Button
            variant="ghost"
            className="justify-start"
            onClick={() => handleNavigation('/dashboard')}
          >
            <LayoutDashboard className="h-4 w-4 mr-3" />
            Dashboard
          </Button>

          {canCreateProblem && (
            <Button
              variant="ghost"
              className="justify-start"
              onClick={() => handleNavigation('/create-problem')}
            >
              <PlusCircle className="h-4 w-4 mr-3" />
              Create Problem
            </Button>
          )}

          {isAdmin && (
            <Button
              variant="ghost"
              className="justify-start"
              onClick={() => handleNavigation('/admin')}
            >
              <Shield className="h-4 w-4 mr-3" />
              Admin
            </Button>
          )}

          <Button
            variant="ghost"
            className="justify-start"
            onClick={() => handleNavigation('/leaderboards')}
          >
            <Trophy className="h-4 w-4 mr-3" />
            Leaderboards
          </Button>

          <div className="border-t mt-2 pt-2">
            <Button
              variant="ghost"
              className="justify-start w-full text-destructive hover:text-destructive"
              onClick={handleLogout}
            >
              <LogOut className="h-4 w-4 mr-3" />
              Logout
            </Button>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}
