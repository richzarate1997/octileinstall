import Spinner from './Spinner';

export default function ConfirmDialog({ title, message, confirmLabel = 'Delete', onConfirm, onCancel, confirming = false }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
      <div className="w-full max-w-sm space-y-4 rounded-lg bg-slate-900 p-6 shadow-xl">
        <h2 className="text-lg font-semibold text-white">{title}</h2>
        <p className="text-sm text-slate-300">{message}</p>
        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="rounded bg-slate-800 px-4 py-2 text-sm text-white hover:bg-slate-700"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={confirming}
            className="flex items-center gap-2 rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-500 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {confirming && <Spinner size="sm" />}
            {confirming ? 'Deleting…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
