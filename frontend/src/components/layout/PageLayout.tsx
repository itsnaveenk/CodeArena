import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';
import { MobileNav } from './MobileNav';
import { useAuthStore } from '@/stores/authStore';

export function PageLayout() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const { isAuthenticated } = useAuthStore();

  return (
    <div className="min-h-screen flex flex-col">
      <Navbar onMobileMenuClick={() => setMobileNavOpen(true)} />
      {isAuthenticated && (
        <MobileNav open={mobileNavOpen} onOpenChange={setMobileNavOpen} />
      )}
      <main className="flex-1">
        <Outlet />
      </main>
    </div>
  );
}
