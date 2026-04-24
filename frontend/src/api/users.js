import { api } from '../services/api.js'

export const usersApi = {
  getMe()                { return api.get('/api/v1/users/me') },
  getProfile()           { return api.get('/api/v1/users/me/profile') },
  updateProfile(data)    { return api.put('/api/v1/users/me', data) },
  changePassword(data)   { return api.put('/api/v1/users/me/password', data) },
  getById(id)            { return api.get(`/api/v1/users/${id}`) },
  getStudents()          { return api.get('/api/v1/users?role=STUDENT') },
  getUnassignedStudents(){ return api.get('/api/v1/users?role=STUDENT&unassigned=true') },
}
