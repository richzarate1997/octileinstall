import { createContext, useCallback, useContext, useRef, useState } from 'react';
import ToastContainer from '../components/ToastContainer';

const ToastContext = createContext(null);

const AUTO_DISMISS_MS = 4000;

// Scope: toasts are for the OUTCOME of an action the user just took (create/update/
// delete succeeded or failed) - ephemeral, non-blocking, fine to miss. They deliberately
// do NOT replace the inline error banners full-page list loads use (those need to stay
// visible to explain why the page looks empty, not vanish after 4 seconds), and they
// don't replace inline form-validation errors either (the user is already looking right
// at the form; a floating duplicate would just be noise).
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const idRef = useRef(0);
  const timers = useRef({});

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
    clearTimeout(timers.current[id]);
    delete timers.current[id];
  }, []);

  const showToast = useCallback(
    (message, type = 'success') => {
      const id = idRef.current++;
      setToasts((current) => [...current, { id, message, type }]);
      timers.current[id] = setTimeout(() => dismiss(id), AUTO_DISMISS_MS);
    },
    [dismiss],
  );

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <ToastContainer toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}
