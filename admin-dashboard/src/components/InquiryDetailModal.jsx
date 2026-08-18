import { format } from 'date-fns';
import { useEffect, useState } from 'react';
import Spinner from './Spinner';
import { extractErrorMessage } from '../services/api';
import { listInspectionsForInquiry } from '../services/inspections';

export default function InquiryDetailModal({ inquiry, onClose }) {
  const [inspections, setInspections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError('');
      try {
        const results = await listInspectionsForInquiry(inquiry.id);
        if (!cancelled) setInspections(results);
      } catch (err) {
        if (!cancelled) setError(extractErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [inquiry.id]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
      <div className="max-h-[85vh] w-full max-w-lg space-y-4 overflow-y-auto rounded-lg bg-slate-900 p-6 shadow-xl">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-white">{inquiry.customerName}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white">
            ✕
          </button>
        </div>

        <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
          <DetailRow label="Email" value={inquiry.customerEmail} />
          <DetailRow label="Phone" value={inquiry.customerPhone || '—'} />
          <DetailRow label="Project Type" value={inquiry.projectType || '—'} />
          <DetailRow label="City" value={inquiry.city || '—'} />
          <DetailRow label="Status" value={inquiry.status} />
          <DetailRow label="Created" value={format(new Date(inquiry.createdAt), 'PPp')} />
        </dl>

        {inquiry.message && (
          <div>
            <h3 className="text-sm font-medium text-slate-300">Message</h3>
            <p className="mt-1 whitespace-pre-wrap text-sm text-slate-400">{inquiry.message}</p>
          </div>
        )}

        {inquiry.notes && (
          <div>
            <h3 className="text-sm font-medium text-slate-300">Notes</h3>
            <p className="mt-1 whitespace-pre-wrap text-sm text-slate-400">{inquiry.notes}</p>
          </div>
        )}

        <div>
          <h3 className="text-sm font-medium text-slate-300">Inspection History</h3>
          {loading ? (
            <p className="mt-1 text-sm text-slate-500">Loading…</p>
          ) : error ? (
            <p role="alert" className="mt-1 text-sm text-red-300">
              {error}
            </p>
          ) : inspections.length === 0 ? (
            <p className="mt-1 text-sm text-slate-500">No inspections scheduled yet.</p>
          ) : (
            <ul className="mt-1 space-y-1 text-sm text-slate-400">
              {inspections.map((inspection) => (
                <li key={inspection.id}>
                  {format(new Date(inspection.scheduledDate), 'PPp')} — {inspection.status}
                  {inspection.location ? ` — ${inspection.location}` : ''}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

function DetailRow({ label, value }) {
  return (
    <div>
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-slate-200">{value}</dd>
    </div>
  );
}
