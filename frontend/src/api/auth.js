import { api } from '../services/api.js'

export const authApi = {
  login:    (email, password) => api.post('/api/v1/auth/login', { email, password }),
  register: (payload)        => api.post('/api/v1/auth/register', payload),
}
