import { api } from '../services/api'

export default {
  search(params) {
    return api.get('/api/v1/students', params)
  },
  getById(id) {
    return api.get(`/api/v1/students/${id}`)
  },
  delete(id, adminPassword) {
    return api.delete(`/api/v1/students/${id}`, { adminPassword }, { skipAutoLogout: true })
  },
}
