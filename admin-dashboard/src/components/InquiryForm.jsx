import { useState } from 'react';
import { extractErrorMessage } from '../services/api';
import { createInquiry, INQUIRY_STATUSES, PROJECT_TYPES, updateInquiry } from '../services/inquiries';

const fieldClass =
  'mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-blue-500 focus:outline-none';

// mode='create' posts customer info per BACKEND_PLAN.md's Inquiry create contract.
// mode='edit' only ever sends { status, notes } - that's all PUT /api/inquiry/{id}
// accepts. There is no backend endpoint to edit customer details after creation, so this
// form intentionally doesn't offer to - showing fields the API would silently ignore
// would be worse than not showing them.
export default function InquiryForm({ mode, inquiry, onClose, onSaved }) {
  const isEdit = mode === 'edit';

  const [customerName, setCustomerName] = useState('');
  const [customerEmail, setCustomerEmail] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [projectType, setProjectType] = useState(PROJECT_TYPES[0]);
  const [city, setCity] = useState('');
  const [message, setMessage] = useState('');

  const [status, setStatus] = useState(inquiry?.status ?? 'NEW');
  const [notes, setNotes] = useState(inquiry?.notes ?? '');

  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (isEdit) {
        await updateInquiry(inquiry.id, { status, notes });
      } else {
        await createInquiry({ customerName, customerEmail, customerPhone, projectType, city, message });
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
          <h2 className="text-lg font-semibold text-white">{isEdit ? 'Edit Inquiry' : 'New Inquiry'}</h2>
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
            <p className="text-sm text-slate-400">{inquiry.customerName}</p>

            <div>
              <label htmlFor="status" className="block text-sm text-slate-300">
                Status
              </label>
              <select
                id="status"
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className={fieldClass}
              >
                {INQUIRY_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
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
              <label htmlFor="customerName" className="block text-sm text-slate-300">
                Customer Name
              </label>
              <input
                id="customerName"
                required
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                className={fieldClass}
              />
            </div>

            <div>
              <label htmlFor="customerEmail" className="block text-sm text-slate-300">
                Customer Email
              </label>
              <input
                id="customerEmail"
                type="email"
                required
                value={customerEmail}
                onChange={(e) => setCustomerEmail(e.target.value)}
                className={fieldClass}
              />
            </div>

            <div>
              <label htmlFor="customerPhone" className="block text-sm text-slate-300">
                Customer Phone
              </label>
              <input
                id="customerPhone"
                value={customerPhone}
                onChange={(e) => setCustomerPhone(e.target.value)}
                className={fieldClass}
              />
            </div>

            <div>
              <label htmlFor="projectType" className="block text-sm text-slate-300">
                Project Type
              </label>
              <select
                id="projectType"
                value={projectType}
                onChange={(e) => setProjectType(e.target.value)}
                className={fieldClass}
              >
                {PROJECT_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="city" className="block text-sm text-slate-300">
                City
              </label>
              <input id="city" value={city} onChange={(e) => setCity(e.target.value)} className={fieldClass} />
            </div>

            <div>
              <label htmlFor="message" className="block text-sm text-slate-300">
                Message
              </label>
              <textarea
                id="message"
                rows={3}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
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
