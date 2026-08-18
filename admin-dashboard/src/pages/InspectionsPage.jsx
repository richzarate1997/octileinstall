import { useCallback, useEffect, useState } from 'react';
import ConfirmDialog from '../components/ConfirmDialog';
import InspectionForm from '../components/InspectionForm';
import InspectionRescheduleModal from '../components/InspectionRescheduleModal';
import InspectionTable from '../components/InspectionTable';
import { extractErrorMessage } from '../services/api';
import { listInquiries } from '../services/inquiries';
import { deleteInspection, INSPECTION_STATUSES, listInspections, updateInspection } from '../services/inspections';

const PAGE_SIZE = 20;

export default function InspectionsPage() {
  const [status, setStatus] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);

  const [inspections, setInspections] = useState([]);
  const [total, setTotal] = useState(0);
  const [inquiryNames, setInquiryNames] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [formState, setFormState] = useState(null); // { mode: 'create' | 'edit', inspection? }
  const [reschedulingInspection, setReschedulingInspection] = useState(null);
  const [deletingInspection, setDeletingInspection] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const refetch = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [inspectionsResult, inquiriesResult] = await Promise.all([
        listInspections({
          status: status || undefined,
          from: from || undefined,
          to: to || undefined,
          limit: PAGE_SIZE,
          offset: page * PAGE_SIZE,
        }),
        // Only used to label rows with a customer name instead of a bare id - see
        // InspectionTable. Same limit=100 scale assumption as services/inspections.js's
        // listInspectionsForInquiry().
        listInquiries({ limit: 100 }),
      ]);
      setInspections(inspectionsResult.data);
      setTotal(inspectionsResult.total);
      setInquiryNames(Object.fromEntries(inquiriesResult.data.map((inquiry) => [inquiry.id, inquiry.customerName])));
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [status, from, to, page]);

  useEffect(() => {
    refetch();
  }, [refetch]);

  function handleStatusChange(value) {
    setStatus(value);
    setPage(0);
  }

  function handleFromChange(value) {
    setFrom(value);
    setPage(0);
  }

  function handleToChange(value) {
    setTo(value);
    setPage(0);
  }

  async function handleMarkComplete(inspection) {
    setError('');
    try {
      await updateInspection(inspection.id, { status: 'COMPLETED' });
      await refetch();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleDeleteConfirmed() {
    setDeleting(true);
    try {
      await deleteInspection(deletingInspection.id);
      setDeletingInspection(null);
      await refetch();
    } catch (err) {
      setError(extractErrorMessage(err));
      setDeletingInspection(null);
    } finally {
      setDeleting(false);
    }
  }

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-white">Inspections</h1>
        <button
          onClick={() => setFormState({ mode: 'create' })}
          className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500"
        >
          New Inspection
        </button>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-4">
        <div className="flex items-center gap-2">
          <label htmlFor="status-filter" className="text-sm text-slate-400">
            Status
          </label>
          <select
            id="status-filter"
            value={status}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="rounded border border-slate-700 bg-slate-800 px-2 py-1 text-sm text-white"
          >
            <option value="">All</option>
            {INSPECTION_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-2">
          <label htmlFor="from-filter" className="text-sm text-slate-400">
            From
          </label>
          <input
            id="from-filter"
            type="date"
            value={from}
            onChange={(e) => handleFromChange(e.target.value)}
            className="rounded border border-slate-700 bg-slate-800 px-2 py-1 text-sm text-white"
          />
        </div>

        <div className="flex items-center gap-2">
          <label htmlFor="to-filter" className="text-sm text-slate-400">
            To
          </label>
          <input
            id="to-filter"
            type="date"
            value={to}
            onChange={(e) => handleToChange(e.target.value)}
            className="rounded border border-slate-700 bg-slate-800 px-2 py-1 text-sm text-white"
          />
        </div>
      </div>

      {error && (
        <p role="alert" className="mt-4 rounded bg-red-950 px-3 py-2 text-sm text-red-300">
          {error}
        </p>
      )}

      {loading ? (
        <p className="mt-6 text-slate-400">Loading…</p>
      ) : error && inspections.length === 0 ? null : (
        // Same empty-vs-error guard as InquiriesPage (Phase 2): only suppress the table
        // when there's truly nothing to show, so a Mark Complete/Delete failure keeps
        // showing existing rows instead of blanking the screen.
        <>
          <InspectionTable
            inspections={inspections}
            inquiryNames={inquiryNames}
            onEdit={(inspection) => setFormState({ mode: 'edit', inspection })}
            onReschedule={setReschedulingInspection}
            onMarkComplete={handleMarkComplete}
            onDelete={setDeletingInspection}
          />

          <div className="mt-4 flex items-center justify-between text-sm text-slate-400">
            <span>{total} total</span>
            <div className="flex items-center gap-3">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded bg-slate-800 px-3 py-1 disabled:opacity-40"
              >
                Previous
              </button>
              <span>
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= totalPages}
                className="rounded bg-slate-800 px-3 py-1 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}

      {formState && (
        <InspectionForm
          mode={formState.mode}
          inspection={formState.inspection}
          onClose={() => setFormState(null)}
          onSaved={() => {
            setFormState(null);
            refetch();
          }}
        />
      )}

      {reschedulingInspection && (
        <InspectionRescheduleModal
          inspection={reschedulingInspection}
          onClose={() => setReschedulingInspection(null)}
          onSaved={() => {
            setReschedulingInspection(null);
            refetch();
          }}
        />
      )}

      {deletingInspection && (
        <ConfirmDialog
          title="Delete Inspection"
          message={`Delete this inspection scheduled for ${new Date(deletingInspection.scheduledDate).toLocaleString()}? This cannot be undone.`}
          onConfirm={handleDeleteConfirmed}
          onCancel={() => setDeletingInspection(null)}
          confirming={deleting}
        />
      )}
    </div>
  );
}
