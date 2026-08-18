export default function ToastContainer({ toasts, onDismiss }) {
  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-4 right-4 z-[100] flex w-full max-w-sm flex-col gap-2 px-4 sm:px-0">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          role="status"
          className={`flex items-start justify-between gap-3 rounded px-4 py-3 text-sm font-body shadow-lg ${
            toast.type === 'error' ? 'bg-red-900 text-red-100' : 'bg-emerald-900 text-emerald-100'
          }`}
        >
          <span>{toast.message}</span>
          <button
            onClick={() => onDismiss(toast.id)}
            aria-label="Dismiss notification"
            className="shrink-0 text-base leading-none opacity-70 hover:opacity-100"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  );
}
