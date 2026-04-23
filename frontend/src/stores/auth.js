import { defineStore } from 'pinia'
import { ref } from 'vue'
import { DEMO_USERS } from '../data/mockData'
import { usersApi } from '../api/users.js'
import { teamsApi } from '../api/teams.js'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)

  function login(role) {
    user.value = { ...DEMO_USERS[role] }
  }

  // Fetches live teamId + team name from the API and updates the session.
  // Silent on failure (backend may not be running in pure frontend dev).
  async function refreshTeam() {
    if (!user.value || user.value.role !== 'student') return
    try {
      const userRes = await usersApi.getById(user.value.id)
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

  function logout() {
    user.value = null
  }

  return { user, login, logout, refreshTeam }
})
