import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Spinner from '../components/Spinner';
import { useAuth } from '../context/AuthContext';
import { extractErrorMessage } from '../services/api';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const redirectTo = location.state?.from?.pathname ?? '/';

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await login(email, password);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950 px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm space-y-4 rounded-lg bg-slate-900 p-8 shadow-xl"
      >
        <h1 className="text-xl tracking-wide text-white">OC Tile Install — Admin</h1>

        {error && (
          <p role="alert" className="rounded bg-red-950 px-3 py-2 text-sm text-red-300">
            {error}
          </p>
        )}

        <div>
          <label htmlFor="email" className="block text-sm text-slate-300">
            Email
          </label>
          <input
            id="email"
            type="email"
            required
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-blue-500 focus:outline-none"
          />
        </div>

        <div>
          <label htmlFor="password" className="block text-sm text-slate-300">
            Password
          </label>
          <input
            id="password"
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-blue-500 focus:outline-none"
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="flex w-full items-center justify-center gap-2 rounded bg-blue-600 px-3 py-2 font-medium text-white hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {submitting && <Spinner size="sm" />}
          {submitting ? 'Signing in…' : 'Sign In'}
        </button>
      </form>
    </div>
  );
}
