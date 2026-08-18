export default function StatCard({ label, value }) {
  return (
    <div className="rounded-lg border border-white/10 bg-slate-900 p-6 shadow">
      <p className="font-label text-sm uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-2 font-display text-4xl tracking-wide text-white">{value}</p>
    </div>
  );
}
