import { format } from 'date-fns';

const STATUS_COLORS = {
  SCHEDULED: 'bg-green-900 text-green-300',
  IN_PROGRESS: 'bg-blue-900 text-blue-300',
  COMPLETED: 'bg-slate-800 text-slate-300',
  CANCELLED: 'bg-red-950 text-red-300',
};

export default function InspectionTable({ inspections, inquiryNames, onEdit, onReschedule, onMarkComplete, onDelete }) {
  if (inspections.length === 0) {
    return <p className="mt-6 text-slate-400">No inspections found.</p>;
  }

  return (
    <table className="mt-6 w-full text-left text-sm text-slate-300">
      <thead className="border-b border-slate-800 text-xs uppercase text-slate-500">
        <tr>
          <th className="py-2 pr-4">Date &amp; Time</th>
          <th className="py-2 pr-4">Inquiry</th>
          <th className="py-2 pr-4">Location</th>
          <th className="py-2 pr-4">Status</th>
          <th className="py-2 pr-4">Actions</th>
        </tr>
      </thead>
      <tbody>
        {inspections.map((inspection) => (
          <tr key={inspection.id} className="border-b border-slate-900">
            <td className="py-2 pr-4">{format(new Date(inspection.scheduledDate), 'PPp')}</td>
            <td className="py-2 pr-4">{inquiryNames[inspection.inquiryId] ?? `Inquiry #${inspection.inquiryId}`}</td>
            <td className="py-2 pr-4">{inspection.location || '—'}</td>
            <td className="py-2 pr-4">
              <span
                className={`rounded px-2 py-0.5 text-xs ${STATUS_COLORS[inspection.status] ?? 'bg-slate-800 text-slate-300'}`}
              >
                {inspection.status}
              </span>
            </td>
            <td className="space-x-3 py-2 pr-4">
              <button onClick={() => onEdit(inspection)} className="text-blue-400 hover:underline">
                Edit
              </button>
              <button onClick={() => onReschedule(inspection)} className="text-blue-400 hover:underline">
                Reschedule
              </button>
              {inspection.status !== 'COMPLETED' && (
                <button onClick={() => onMarkComplete(inspection)} className="text-green-400 hover:underline">
                  Mark Complete
                </button>
              )}
              <button onClick={() => onDelete(inspection)} className="text-red-400 hover:underline">
                Delete
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
