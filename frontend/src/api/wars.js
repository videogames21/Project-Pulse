import { api } from '../services/api.js'

export const warsApi = {
  getWAR: (studentId, weekStart) =>
    api.get(`/api/v1/wars/students/${studentId}/weeks/${weekStart}`),
  addActivity: (studentId, weekStart, payload) =>
    api.post(`/api/v1/wars/students/${studentId}/weeks/${weekStart}/activities`, payload),
  updateActivity: (studentId, weekStart, activityId, payload) =>
    api.put(`/api/v1/wars/students/${studentId}/weeks/${weekStart}/activities/${activityId}`, payload),
  deleteActivity: (studentId, weekStart, activityId) =>
    api.delete(`/api/v1/wars/students/${studentId}/weeks/${weekStart}/activities/${activityId}`),
  getTeamReport: (teamId, weekStart) =>
    api.get(`/api/v1/wars/teams/${teamId}/report`, { weekStart }),
  getStudentRangeReport: (studentId, startWeek, endWeek) =>
    api.get(`/api/v1/wars/students/${studentId}/report`, { startWeek, endWeek }),
}
