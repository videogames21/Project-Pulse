import { api } from '../services/api.js'

export const teamsApi = {
  getAll(params = {}) {
    const query = new URLSearchParams(params).toString()
    return api.get(`/api/v1/teams${query ? '?' + query : ''}`)
  },
  getById(id)                    { return api.get(`/api/v1/teams/${id}`) },
  assignStudents(id, ids)        { return api.post(`/api/v1/teams/${id}/students`, { studentIds: ids }) },
  removeStudent(id, sid)         { return api.delete(`/api/v1/teams/${id}/students/${sid}`) },
  assignInstructor(id, iid)      { return api.post(`/api/v1/teams/${id}/instructors`, { instructorId: iid }) },
  removeInstructor(id, iid)      { return api.delete(`/api/v1/teams/${id}/instructors/${iid}`) },
}
