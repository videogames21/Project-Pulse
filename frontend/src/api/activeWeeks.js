import { api } from '../services/api.js'

export const activeWeeksApi = {
  get(sectionId)         { return api.get(`/api/v1/sections/${sectionId}/active-weeks`) },
  save(sectionId, dates) { return api.put(`/api/v1/sections/${sectionId}/active-weeks`, { activeWeekDates: dates }) },
}
