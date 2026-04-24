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
}
