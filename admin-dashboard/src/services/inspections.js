import { api } from './api';

export const INSPECTION_STATUSES = ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

export async function listInspections({ status, from, to, limit = 20, offset = 0 } = {}) {
  const params = { limit, offset };
  if (status) params.status = status;
  if (from) params.from = from;
  if (to) params.to = to;
  const response = await api.get('/inspection', { params });
  return response.data; // { data: [...], total }
}

export async function getInspectionsCount(filters = {}) {
  const { total } = await listInspections({ ...filters, limit: 1, offset: 0 });
  return total;
}

// The backend has no ?inquiryId= filter (Phase 5 only supports status/date-range - see
// InspectionRepository/InspectionController). Fetches a generously-sized page and filters
// client-side instead. Fine at this admin tool's expected per-inquiry inspection count
// (a handful at most); if that stops being true, add a real backend filter rather than
// raising this limit further.
export async function listInspectionsForInquiry(inquiryId) {
  const response = await api.get('/inspection', { params: { limit: 100, offset: 0 } });
  return response.data.data.filter((inspection) => inspection.inquiryId === inquiryId);
}

export async function getInspection(id) {
  const response = await api.get(`/inspection/${id}`);
  return response.data;
}

// POST /api/inspection requires a valid inquiryId (404 if it doesn't exist) and a
// scheduledDate strictly in the future (400 otherwise) - both enforced server-side
// (BACKEND_PLAN.md Phase 5 / InspectionService.create()). There is no way to edit
// inquiryId or location after creation.
export async function createInspection(payload) {
  const response = await api.post('/inspection', payload);
  return response.data;
}

// PUT /api/inspection/{id} only ever accepts { status, notes, scheduledDate }. Presence of
// scheduledDate means "reschedule" and is validated as a future date; omitting it (e.g. a
// status-only update marking a past inspection COMPLETED) skips that check entirely - see
// InspectionService.update(). Callers must not pass anything else.
export async function updateInspection(id, { status, notes, scheduledDate } = {}) {
  const payload = {};
  if (status !== undefined) payload.status = status;
  if (notes !== undefined) payload.notes = notes;
  if (scheduledDate !== undefined) payload.scheduledDate = scheduledDate;
  const response = await api.put(`/inspection/${id}`, payload);
  return response.data;
}

export async function deleteInspection(id) {
  await api.delete(`/inspection/${id}`);
}
