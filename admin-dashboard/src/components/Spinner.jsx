const SIZE_CLASSES = {
  sm: 'h-4 w-4 border-2',
  md: 'h-8 w-8 border-4',
};

export default function Spinner({ size = 'md', className = '' }) {
  return (
    <span
      role="status"
      aria-label="Loading"
      className={`inline-block animate-spin rounded-full border-slate-700 border-t-blue-500 ${SIZE_CLASSES[size]} ${className}`}
    />
  );
}
