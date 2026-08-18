import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/inquiries', label: 'Inquiries' },
  { to: '/inspections', label: 'Inspections' },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-950 font-body text-white lg:flex">
      {/* Mobile top bar - the sidebar below is hidden by default under lg, this is the
          only way to reach navigation until it's toggled open. */}
      <div className="flex items-center justify-between border-b border-slate-800 px-4 py-3 lg:hidden">
        <span className="font-display text-lg tracking-wide">OC Tile Install</span>
        <button
          onClick={() => setMobileNavOpen((open) => !open)}
          aria-label="Toggle navigation"
          aria-expanded={mobileNavOpen}
          className="rounded p-2 text-slate-300 hover:bg-slate-800 hover:text-white"
        >
          <MenuIcon />
        </button>
      </div>

      <aside
        className={`${
          mobileNavOpen ? 'flex' : 'hidden'
        } flex-col border-b border-slate-800 bg-slate-950 lg:sticky lg:top-0 lg:flex lg:h-screen lg:w-64 lg:shrink-0 lg:border-b-0 lg:border-r`}
      >
        <div className="hidden px-6 py-6 lg:block">
          <span className="font-display text-xl tracking-wide">OC Tile Install</span>
          <p className="font-label text-xs uppercase tracking-wide text-slate-400">Admin</p>
        </div>

        <nav className="flex flex-1 flex-col gap-1 px-3 py-3 font-label">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setMobileNavOpen(false)}
              className={navLinkClass}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-slate-800 px-3 py-3">
          <p className="truncate px-3 text-sm text-slate-400">{user?.fullName}</p>
          <button
            onClick={logout}
            className="mt-2 w-full rounded bg-slate-800 px-3 py-2 text-left text-sm text-white hover:bg-slate-700"
          >
            Log out
          </button>
        </div>
      </aside>

      <main className="flex-1 p-4 sm:p-6">
        <Outlet />
      </main>
    </div>
  );
}

function navLinkClass({ isActive }) {
  return `rounded px-3 py-2 text-sm tracking-wide ${
    isActive ? 'bg-blue-600 text-white' : 'text-slate-400 hover:bg-slate-800 hover:text-white'
  }`;
}

function MenuIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-6 w-6"
      aria-hidden="true"
    >
      <line x1="3" y1="6" x2="21" y2="6" />
      <line x1="3" y1="12" x2="21" y2="12" />
      <line x1="3" y1="18" x2="21" y2="18" />
    </svg>
  );
}
