import { api } from '../services/api.js'

export const peerEvaluationsApi = {
  submit: (payload) =>
    api.post('/api/v1/peer-evaluations', payload),

  update: (id, payload) =>
    api.put(`/api/v1/peer-evaluations/${id}`, payload),

  getById: (id) =>
    api.get(`/api/v1/peer-evaluations/${id}`),

  getStudentReport: (studentId, weekStart) =>
    api.get(`/api/v1/peer-evaluations/students/${studentId}/report`, { weekStart }),

  getSectionReport: (sectionName, weekStart) =>
    api.get(`/api/v1/peer-evaluations/sections/${encodeURIComponent(sectionName)}/report`, { weekStart }),

  getStudentRangeReport: (studentId, startWeek, endWeek) =>
    api.get(`/api/v1/peer-evaluations/students/${studentId}/report/range`, { startWeek, endWeek }),
}
