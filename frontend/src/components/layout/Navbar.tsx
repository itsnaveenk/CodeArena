import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { useThemeStore } from '@/stores/themeStore';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Moon, Sun, User, LogOut, Menu } from 'lucide-react';

interface NavbarProps {
  onMobileMenuClick?: () => void;
}

export function Navbar({ onMobileMenuClick }: NavbarProps) {
  const navigate = useNavigate();
  const { user, isAuthenticated, clearAuth } = useAuthStore();
  const { theme, toggleTheme } = useThemeStore();

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  const isProblemSetter = user?.role === 'PROBLEM_SETTER';
  const isAdmin = user?.role === 'ADMIN';

  return (
    <nav className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container mx-auto px-4">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-2">
              <span className="text-xl font-bold">CodeArena</span>
            </Link>

            {/* Desktop Navigation */}
            {isAuthenticated && (
              <div className="hidden md:flex items-center gap-4">
                <Link
                  to="/problems"
                  className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                >
                  Problems
                </Link>
                <Link
                  to="/contests"
                  className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                >
                  Contests
                </Link>
                <Link
                  to="/dashboard"
                  className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                >
                  Dashboard
                </Link>
                {isProblemSetter && (
                  <>
                    <Link
                      to="/my-problems"
                      className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                    >
                      My Problems
                    </Link>
                    <Link
                      to="/create-problem"
                      className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                    >
                      Create Problem
                    </Link>
                  </>
                )}
                {isAdmin && (
                    <Link
                        to="/admin"
                        className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                    >
                      Admin
                    </Link>
                )}
                <Link
                  to="/leaderboards"
                  className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                >
                  Leaderboards
                </Link>
              </div>
            )}
          </div>

          {/* Right side */}
          <div className="flex items-center gap-2">
            {/* Theme toggle */}
            <Button
              variant="ghost"
              size="icon"
              onClick={toggleTheme}
              aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
            >
              {theme === 'dark' ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
            </Button>

            {isAuthenticated ? (
              <>
                {/* User dropdown */}
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" className="hidden md:flex items-center gap-2">
                      <User className="h-4 w-4" />
                      <span>{user?.name}</span>
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem asChild>
                      <Link to="/dashboard">Profile</Link>
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={handleLogout}>
                      <LogOut className="h-4 w-4 mr-2" />
                      Logout
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>

                {/* Mobile menu button */}
                <Button
                  variant="ghost"
                  size="icon"
                  className="md:hidden"
                  onClick={onMobileMenuClick}
                  aria-label="Open menu"
                >
                  <Menu className="h-5 w-5" />
                </Button>
              </>
            ) : (
              <div className="flex items-center gap-2">
                <Button variant="ghost" asChild>
                  <Link to="/login">Login</Link>
                </Button>
                <Button asChild>
                  <Link to="/signup">Sign Up</Link>
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
