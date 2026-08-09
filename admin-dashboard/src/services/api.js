import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL;

export const api = axios.create({
  baseURL: API_URL,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// A 401 here means the stored token is missing/expired/invalid - clear it and let
// AuthContext (listening for this event) redirect to login. This can't call the auth
// context's logout() directly since this module lives outside the React tree.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.dispatchEvent(new Event('auth:unauthorized'));
    }
    return Promise.reject(error);
  },
);

// Every backend error response shares one shape: { status, error, message, fieldErrors? }
// (see BACKEND_PLAN.md Phase 6 / GlobalExceptionHandler). Unwrap it once here so components
// don't each need to know that shape.
export function extractErrorMessage(error) {
  const data = error?.response?.data;
  if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    return Object.values(data.fieldErrors).join(', ');
  }
  if (data?.message) {
    return data.message;
  }
  return 'Something went wrong. Please try again.';
}

export async function login(email, password) {
  const response = await api.post('/auth/login', { email, password });
  return response.data; // { token, expiresIn, userId, email, fullName, role }
}
