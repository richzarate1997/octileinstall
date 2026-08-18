import { useState } from 'react';
import { extractErrorMessage } from '../services/api';
import { updateInspection } from '../services/inspections';
import { toLocalInputValue } from '../utils/datetime';

// Deliberately separate from InspectionForm's Edit mode, even though both end up calling
// the same PUT /api/inspection/{id} - "I need to move this appointment" is a distinct,
// faster user intent from "I want to edit this record generally," and doesn't need status
// or notes in the way.
export default function InspectionRescheduleModal({ inspection, onClose, onSaved }) {
  const [scheduledDate, setScheduledDate] = useState(toLocalInputValue(inspection.scheduledDate));
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await updateInspection(inspection.id, { scheduledDate });
      onSaved();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
      <form onSubmit={handleSubmit} className="w-full max-w-sm space-y-4 rounded-lg bg-slate-900 p-6 shadow-xl">
        <h2 className="text-lg font-semibold text-white">Reschedule Inspection</h2>

        {error && (
          <p role="alert" className="rounded bg-red-950 px-3 py-2 text-sm text-red-300">
            {error}
          </p>
        )}

        <div>
          <label htmlFor="reschedule-date" className="block text-sm text-slate-300">
            New Date &amp; Time
          </label>
          <input
            id="reschedule-date"
            type="datetime-local"
            required
            value={scheduledDate}
            onChange={(e) => setScheduledDate(e.target.value)}
            className="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-blue-500 focus:outline-none"
          />
        </div>

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
            disabled={submitting}
            className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  );
}
