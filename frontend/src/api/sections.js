import { api } from '../services/api.js'

export const sectionsApi = {
  getAll(name) {
    const path = name
      ? `/api/v1/sections?name=${encodeURIComponent(name)}`
      : '/api/v1/sections'
    return api.get(path)
  },

  getById(id) {
    return api.get(`/api/v1/sections/${id}`)
  },

  create(payload) {
    return api.post('/api/v1/sections', payload)
  },

  update(id, payload) {
    return api.put(`/api/v1/sections/${id}`, payload)
  },

  getRubricBySectionName(sectionName) {
    return api.get(`/api/v1/sections/name/${encodeURIComponent(sectionName)}/rubric`)
  },
}
