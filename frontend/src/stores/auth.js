import { defineStore } from 'pinia'
import { ref } from 'vue'
import { DEMO_USERS } from '../data/mockData'
import { usersApi } from '../api/users.js'
import { teamsApi } from '../api/teams.js'
import { useNotificationsStore } from './notifications.js'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)

  function login(role) {
    user.value = { ...DEMO_USERS[role] }
  }

  // Fetches live teamId + team name from the API and updates the session.
  // Detects team removal and queues a notification for the student.
  // Silent on failure (backend may not be running in pure frontend dev).
  async function refreshTeam() {
    if (!user.value || user.value.role !== 'student') return
    try {
      const previousTeamId = user.value.teamId ?? null
      const userRes = await usersApi.getById(user.value.id)
      const { teamId } = userRes.data

      if (teamId) {
        const teamRes = await teamsApi.getById(teamId)
        const team = teamRes.data
        user.value = { ...user.value, teamId, team: team.name, section: team.sectionName }
      } else {
        if (previousTeamId) {
          useNotificationsStore().add('warning', 'You have been removed from your team. Please contact your instructor or admin.')
        }
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
