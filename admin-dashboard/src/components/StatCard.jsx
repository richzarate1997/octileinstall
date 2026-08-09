export default function StatCard({ label, value }) {
  return (
    <div className="rounded-lg bg-slate-900 p-6 shadow">
      <p className="text-sm text-slate-400">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-white">{value}</p>
    </div>
  );
}
