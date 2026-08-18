import { format } from 'date-fns';

export default function InquiryTable({ inquiries, onView, onEdit, onDelete }) {
  if (inquiries.length === 0) {
    return <p className="mt-6 text-slate-400">No inquiries found.</p>;
  }

  return (
    // min-w ensures narrow viewports get a real horizontal scroll (via the overflow-x-auto
    // wrapper in InquiriesPage) instead of columns getting crushed illegible.
    <table className="mt-6 w-full min-w-[640px] text-left text-sm text-slate-300">
      <thead className="border-b border-slate-800 font-label text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th className="py-2 pr-4">Customer</th>
          <th className="py-2 pr-4">Project Type</th>
          <th className="py-2 pr-4">Status</th>
          <th className="py-2 pr-4">Created</th>
          <th className="py-2 pr-4">Actions</th>
        </tr>
      </thead>
      <tbody>
        {inquiries.map((inquiry) => (
          <tr key={inquiry.id} className="border-b border-slate-900">
            <td className="py-2 pr-4">{inquiry.customerName}</td>
            <td className="py-2 pr-4">{inquiry.projectType || '—'}</td>
            <td className="py-2 pr-4">
              <span className="rounded bg-slate-800 px-2 py-0.5 text-xs">{inquiry.status}</span>
            </td>
            <td className="py-2 pr-4">{format(new Date(inquiry.createdAt), 'PP')}</td>
            <td className="space-x-3 py-2 pr-4">
              <button onClick={() => onView(inquiry)} className="text-blue-400 hover:underline">
                View
              </button>
              <button onClick={() => onEdit(inquiry)} className="text-blue-400 hover:underline">
                Edit
              </button>
              <button onClick={() => onDelete(inquiry)} className="text-red-400 hover:underline">
                Delete
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
