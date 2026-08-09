import { api } from './api';

// Mirrors Inquiry.Status / Inquiry.ProjectType exactly (backend/src/main/java/com/octileinstall/entity/Inquiry.java).
// ProjectType values are the canonical @JsonValue display labels the backend both accepts
// and always echoes back in responses - using anything else here (e.g. the shorter values
// index.html's own quote form submits) would make an edit form's pre-filled dropdown fail
// to match its own options.
export const INQUIRY_STATUSES = ['NEW', 'CONTACTED', 'QUOTED', 'SCHEDULED', 'COMPLETED', 'ARCHIVED'];

export const PROJECT_TYPES = [
  'Custom Shower',
  'Bathroom Floors & Walls',
  'Kitchen Backsplashes',
  'Flooring - Tile, Marble & Mosaic',
  'Tub Surrounds',
  'Repairs',
];

export async function listInquiries({ status, limit = 20, offset = 0 } = {}) {
  const params = { limit, offset };
  if (status) params.status = status;
  const response = await api.get('/inquiry', { params });
  return response.data; // { data: [...], total }
}

export async function getInquiriesCount(filters = {}) {
  const { total } = await listInquiries({ ...filters, limit: 1, offset: 0 });
  return total;
}

export async function getInquiry(id) {
  const response = await api.get(`/inquiry/${id}`);
  return response.data;
}

export async function createInquiry(payload) {
  const response = await api.post('/inquiry', payload);
  return response.data;
}

// PUT /api/inquiry/{id} only ever accepts { status, notes } - the backend has no endpoint
// to edit customer details after creation (BACKEND_PLAN.md Phase 4). Callers must not pass
// anything else; it would be silently ignored rather than erroring.
export async function updateInquiry(id, { status, notes }) {
  const response = await api.put(`/inquiry/${id}`, { status, notes });
  return response.data;
}

export async function deleteInquiry(id) {
  await api.delete(`/inquiry/${id}`);
}
