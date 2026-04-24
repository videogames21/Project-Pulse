import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../services/api.js'
import { usersApi } from '../api/users.js'
import { teamsApi } from '../api/teams.js'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)

  async function login(email, password) {
    const res = await api.post('/api/v1/auth/login', { email, password })
    _saveSession(res.data)
  }

  async function register(payload) {
    const res = await api.post('/api/v1/auth/register', payload)
    _saveSession(res.data)
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    user.value = null
  }

  function initFromStorage() {
    const stored = localStorage.getItem('user')
    if (stored) user.value = JSON.parse(stored)
  }

  function _saveSession(data) {
    localStorage.setItem('token', data.token)
    const u = { id: data.id, name: data.name, email: data.email, role: data.role.toLowerCase(), teamId: null }
    localStorage.setItem('user', JSON.stringify(u))
    user.value = u
  }

  function updateSession(data) {
    localStorage.setItem('token', data.token)
    const u = { ...user.value, id: data.id, name: data.name, email: data.email, role: data.role.toLowerCase() }
    localStorage.setItem('user', JSON.stringify(u))
    user.value = u
  }

  // Fetches live teamId + team name from the API and updates the session.
  // Silent on failure (backend may not be running in pure frontend dev).
  async function refreshTeam() {
    if (!user.value || user.value.role !== 'student') return
    try {
      const userRes = await usersApi.getMe()
      const { teamId } = userRes.data

      if (teamId) {
        const teamRes = await teamsApi.getById(teamId)
        const team = teamRes.data
        user.value = { ...user.value, teamId, team: team.name, section: team.sectionName }
      } else {
        user.value = { ...user.value, teamId: null, team: null, section: null }
      }
    } catch {
      // API unavailable — keep existing user data unchanged
    }
  }

  return { user, login, register, logout, initFromStorage, refreshTeam, updateSession }
})
