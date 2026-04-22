import { api } from '../services/api.js'

export const invitationsApi = {
  getAll()         { return api.get('/v1/invitations') },
  generate()       { return api.post('/v1/invitations', {}) },
  validateToken(t) { return api.get(`/v1/invitations/${t}`) },
}
