import { api } from '../services/api.js'

export const notificationsApi = {
  getUnread: (userId) => api.get(`/api/v1/notifications/users/${userId}/unread`),
  markRead:  (id)     => api.put(`/api/v1/notifications/${id}/read`),
}
