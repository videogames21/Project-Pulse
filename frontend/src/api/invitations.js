import { api } from '../services/api.js'

export const invitationsApi = {
  getAll()                  { return api.get('/api/v1/invitations') },
  generate(sectionId)       { return api.post('/api/v1/invitations', { sectionId }) },
  generateInstructorLink()  { return api.post('/api/v1/invitations/instructor', {}) },
  validateToken(t)          { return api.get(`/api/v1/invitations/${t}`) },
  disable(token)            { return api.patch(`/api/v1/invitations/${token}/disable`, {}) },
  enable(token)             { return api.patch(`/api/v1/invitations/${token}/enable`, {}) },
  remove(token)             { return api.delete(`/api/v1/invitations/${token}`) },
}
