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
