import { useCallback, useEffect, useState } from 'react';
import ConfirmDialog from '../components/ConfirmDialog';
import InquiryDetailModal from '../components/InquiryDetailModal';
import InquiryForm from '../components/InquiryForm';
import InquiryTable from '../components/InquiryTable';
import { extractErrorMessage } from '../services/api';
import { deleteInquiry, INQUIRY_STATUSES, listInquiries } from '../services/inquiries';

const PAGE_SIZE = 20;

export default function InquiriesPage() {
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [inquiries, setInquiries] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [formState, setFormState] = useState(null); // { mode: 'create' | 'edit', inquiry? }
  const [viewingInquiry, setViewingInquiry] = useState(null);
  const [deletingInquiry, setDeletingInquiry] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const refetch = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const result = await listInquiries({
        status: status || undefined,
        limit: PAGE_SIZE,
        offset: page * PAGE_SIZE,
      });
      setInquiries(result.data);
      setTotal(result.total);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [status, page]);

  useEffect(() => {
    refetch();
  }, [refetch]);

  function handleStatusChange(value) {
    setStatus(value);
    // A page offset from the previous filter is meaningless once the result set changes.
    setPage(0);
  }

  async function handleDeleteConfirmed() {
    setDeleting(true);
    try {
      await deleteInquiry(deletingInquiry.id);
      setDeletingInquiry(null);
      await refetch();
    } catch (err) {
      setError(extractErrorMessage(err));
      setDeletingInquiry(null);
    } finally {
      setDeleting(false);
    }
  }

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-white">Inquiries</h1>
        <button
          onClick={() => setFormState({ mode: 'create' })}
          className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500"
        >
          New Inquiry
        </button>
      </div>

      <div className="mt-4 flex items-center gap-3">
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
          {INQUIRY_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <p role="alert" className="mt-4 rounded bg-red-950 px-3 py-2 text-sm text-red-300">
          {error}
        </p>
      )}

      {loading ? (
        <p className="mt-6 text-slate-400">Loading…</p>
      ) : error && inquiries.length === 0 ? null : (
        // A failed refetch (e.g. after a delete) leaves `inquiries` holding the last
        // successful load rather than clearing it, so this only suppresses the table when
        // there's truly nothing to show - it must not also hide still-valid stale data
        // just because a later action errored.
        <>
          <InquiryTable
            inquiries={inquiries}
            onView={setViewingInquiry}
            onEdit={(inquiry) => setFormState({ mode: 'edit', inquiry })}
            onDelete={setDeletingInquiry}
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
        <InquiryForm
          mode={formState.mode}
          inquiry={formState.inquiry}
          onClose={() => setFormState(null)}
          onSaved={() => {
            setFormState(null);
            refetch();
          }}
        />
      )}

      {viewingInquiry && <InquiryDetailModal inquiry={viewingInquiry} onClose={() => setViewingInquiry(null)} />}

      {deletingInquiry && (
        <ConfirmDialog
          title="Delete Inquiry"
          message={`Delete the inquiry from ${deletingInquiry.customerName}? This cannot be undone.`}
          onConfirm={handleDeleteConfirmed}
          onCancel={() => setDeletingInquiry(null)}
          confirming={deleting}
        />
      )}
    </div>
  );
}
