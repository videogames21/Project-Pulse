import { api } from '../services/api.js'

export const rubricsApi = {
  getAll() {
    return api.get('/api/v1/rubrics')
  },

  getById(id) {
    return api.get(`/api/v1/rubrics/${id}`)
  },
}
