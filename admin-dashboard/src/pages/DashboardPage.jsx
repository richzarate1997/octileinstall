import { useEffect, useState } from 'react';
import Spinner from '../components/Spinner';
import StatCard from '../components/StatCard';
import { extractErrorMessage } from '../services/api';
import { getInquiriesCount } from '../services/inquiries';
import { getInspectionsCount } from '../services/inspections';

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadStats() {
      setLoading(true);
      setError('');
      try {
        const [total, newCount, scheduledInspections] = await Promise.all([
          getInquiriesCount(),
          getInquiriesCount({ status: 'NEW' }),
          getInspectionsCount({ status: 'SCHEDULED' }),
        ]);
        if (!cancelled) {
          setStats({ total, newCount, scheduledInspections });
        }
      } catch (err) {
        if (!cancelled) setError(extractErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadStats();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div>
      <h1 className="text-2xl tracking-wide text-white">Dashboard</h1>

      {error && (
        <p role="alert" className="mt-4 rounded bg-red-950 px-3 py-2 text-sm text-red-300">
          {error}
        </p>
      )}

      {loading ? (
        <div className="mt-6 flex items-center gap-2 text-slate-400">
          <Spinner size="sm" />
          <span>Loading…</span>
        </div>
      ) : stats ? (
        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
          <StatCard label="Total Inquiries" value={stats.total} />
          <StatCard label="New Inquiries" value={stats.newCount} />
          <StatCard label="Scheduled Inspections" value={stats.scheduledInspections} />
        </div>
      ) : null}
    </div>
  );
}
