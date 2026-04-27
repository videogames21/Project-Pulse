import { api } from '../services/api.js'

export const usersApi = {
  getMe()                  { return api.get('/api/v1/users/me') },
  getProfile()             { return api.get('/api/v1/users/me/profile') },
  updateProfile(data)      { return api.put('/api/v1/users/me', data) },
  changePassword(data)     { return api.put('/api/v1/users/me/password', data) },
  getById(id)              { return api.get(`/api/v1/users/${id}`) },
  getInstructorById(id)    { return api.get(`/api/v1/users/${id}`) },
  getStudents()            { return api.get('/api/v1/users?role=STUDENT') },
  getUnassignedStudents(sectionId) {
    const params = new URLSearchParams({ role: 'STUDENT', unassigned: 'true' })
    if (sectionId) params.set('sectionId', sectionId)
    return api.get(`/api/v1/users?${params}`)
  },
  getInstructors(name, activeOnly = false) {
    const params = new URLSearchParams({ role: 'INSTRUCTOR' })
    if (name?.trim()) params.set('name', name.trim())
    if (activeOnly)   params.set('status', 'ACTIVE')
    return api.get(`/api/v1/users?${params}`)
  },
  deactivateInstructor(id, reason) { return api.patch(`/api/v1/users/${id}/deactivate`, { reason }) },
  reactivateInstructor(id) { return api.patch(`/api/v1/users/${id}/reactivate`) },
}
