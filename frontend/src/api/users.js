import { api } from '../services/api.js'

export const usersApi = {
  getById(id)            { return api.get(`/api/v1/users/${id}`) },
  getStudents()          { return api.get('/api/v1/users?role=STUDENT') },
  getUnassignedStudents(){ return api.get('/api/v1/users?role=STUDENT&unassigned=true') },
}
