import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Deliberately minimal nav, not a styled sidebar - Phase 4 owns "Add navigation sidebar
// with logout" and full branding. This exists only so Phase 2's pages are reachable at all.
export default function Layout() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-slate-950 text-white">
      <nav className="flex items-center justify-between border-b border-slate-800 px-6 py-4">
        <div className="flex items-center gap-6">
          <span className="font-semibold">OC Tile Install — Admin</span>
          <NavLink to="/" end className={navLinkClass}>
            Dashboard
          </NavLink>
          <NavLink to="/inquiries" className={navLinkClass}>
            Inquiries
          </NavLink>
        </div>
        <div className="flex items-center gap-4 text-sm text-slate-400">
          <span>{user?.fullName}</span>
          <button
            onClick={logout}
            className="rounded bg-slate-800 px-3 py-1.5 text-white hover:bg-slate-700"
          >
            Log out
          </button>
        </div>
      </nav>
      <main className="p-6">
        <Outlet />
      </main>
    </div>
  );
}

function navLinkClass({ isActive }) {
  return `text-sm ${isActive ? 'font-medium text-white' : 'text-slate-400 hover:text-slate-200'}`;
}
