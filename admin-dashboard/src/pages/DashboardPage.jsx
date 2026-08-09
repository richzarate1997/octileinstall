import { useAuth } from '../context/AuthContext';

// Minimal placeholder so the Phase 1 login->redirect flow has a real destination to prove
// itself against. Stats cards, inquiry/inspection summaries, etc. are Phase 2.
export default function DashboardPage() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-slate-950 p-8 text-white">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Dashboard</h1>
        <button
          onClick={logout}
          className="rounded bg-slate-800 px-3 py-1.5 text-sm hover:bg-slate-700"
        >
          Log out
        </button>
      </div>
      <p className="mt-4 text-slate-300">
        Logged in as {user?.fullName} ({user?.email}) — role: {user?.role}
      </p>
      <p className="mt-2 text-sm text-slate-500">
        Inquiry and inspection management land in Phases 2/3.
      </p>
    </div>
  );
}
