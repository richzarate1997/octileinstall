import { useEffect, useState } from 'react';
import { extractErrorMessage } from '../services/api';
import { listInquiries } from '../services/inquiries';
import { createInspection, INSPECTION_STATUSES, updateInspection } from '../services/inspections';
import { toLocalInputValue } from '../utils/datetime';

const fieldClass =
  'mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-blue-500 focus:outline-none';

// mode='create' posts { inquiryId, scheduledDate, location } - the backend rejects an
// unknown inquiryId with 404 and a non-future scheduledDate with 400 (BACKEND_PLAN.md
// Phase 5 / InspectionService.create()).
// mode='edit' only ever sends { status, notes, scheduledDate } - PUT /api/inspection/{id}
// has no way to change inquiryId or location after creation, so those aren't offered here.
export default function InspectionForm({ mode, inspection, onClose, onSaved }) {
  const isEdit = mode === 'edit';

  const [inquiries, setInquiries] = useState([]);
  const [inquiriesError, setInquiriesError] = useState('');
  const [inquiryId, setInquiryId] = useState('');
  const [location, setLocation] = useState('');

  const [scheduledDate, setScheduledDate] = useState(
    isEdit ? toLocalInputValue(inspection.scheduledDate) : '',
  );
  const [status, setStatus] = useState(inspection?.status ?? 'SCHEDULED');
  const [notes, setNotes] = useState(inspection?.notes ?? '');

  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isEdit) return;
    let cancelled = false;

    async function loadInquiries() {
      try {
        const result = await listInquiries({ limit: 100 });
        if (!cancelled) {
          setInquiries(result.data);
          if (result.data.length > 0) setInquiryId(String(result.data[0].id));
        }
      } catch (err) {
        if (!cancelled) setInquiriesError(extractErrorMessage(err));
      }
    }

    loadInquiries();
    return () => {
      cancelled = true;
    };
  }, [isEdit]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (isEdit) {
        await updateInspection(inspection.id, { status, notes, scheduledDate });
      } else {
        await createInspection({ inquiryId: Number(inquiryId), scheduledDate, location });
      }
      onSaved();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
      <form
        onSubmit={handleSubmit}
        className="max-h-[85vh] w-full max-w-lg space-y-4 overflow-y-auto rounded-lg bg-slate-900 p-6 shadow-xl"
      >
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-white">{isEdit ? 'Edit Inspection' : 'New Inspection'}</h2>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-white">
            ✕
          </button>
        </div>

        {error && (
          <p role="alert" className="rounded bg-red-950 px-3 py-2 text-sm text-red-300">
            {error}
          </p>
        )}

        {isEdit ? (
          <>
            <div>
              <label htmlFor="status" className="block text-sm text-slate-300">
                Status
              </label>
              <select id="status" value={status} onChange={(e) => setStatus(e.target.value)} className={fieldClass}>
                {INSPECTION_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="scheduledDate" className="block text-sm text-slate-300">
                Date &amp; Time
              </label>
              <input
                id="scheduledDate"
                type="datetime-local"
                required
                value={scheduledDate}
                onChange={(e) => setScheduledDate(e.target.value)}
                className={fieldClass}
              />
            </div>

            <div>
              <label htmlFor="notes" className="block text-sm text-slate-300">
                Notes
              </label>
              <textarea
                id="notes"
                rows={4}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className={fieldClass}
              />
            </div>
          </>
        ) : (
          <>
            <div>
              <label htmlFor="inquiryId" className="block text-sm text-slate-300">
                Inquiry
              </label>
              {inquiriesError ? (
                <p role="alert" className="mt-1 text-sm text-red-300">
                  {inquiriesError}
                </p>
              ) : (
                <select
                  id="inquiryId"
                  required
                  value={inquiryId}
                  onChange={(e) => setInquiryId(e.target.value)}
                  className={fieldClass}
                >
                  {inquiries.length === 0 && <option value="">No inquiries available</option>}
                  {inquiries.map((inquiry) => (
                    <option key={inquiry.id} value={inquiry.id}>
                      {inquiry.customerName} — {inquiry.projectType || 'No project type'} ({inquiry.status})
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div>
              <label htmlFor="scheduledDate" className="block text-sm text-slate-300">
                Date &amp; Time
              </label>
              <input
                id="scheduledDate"
                type="datetime-local"
                required
                value={scheduledDate}
                onChange={(e) => setScheduledDate(e.target.value)}
                className={fieldClass}
              />
            </div>

            <div>
              <label htmlFor="location" className="block text-sm text-slate-300">
                Location
              </label>
              <input
                id="location"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                className={fieldClass}
              />
            </div>
          </>
        )}

        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded bg-slate-800 px-4 py-2 text-sm text-white hover:bg-slate-700"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting || (!isEdit && !inquiryId)}
            className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  );
}
