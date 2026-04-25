import { api } from '../services/api.js'

export const invitationsApi = {
  getAll()              { return api.get('/api/v1/invitations') },
  generate()            { return api.post('/api/v1/invitations', {}) },
  validateToken(t)      { return api.get(`/api/v1/invitations/${t}`) },
  inviteInstructors(emails) { return api.post('/api/v1/invitations/instructors', { emails }) },
}
