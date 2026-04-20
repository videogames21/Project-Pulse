import { defineStore } from 'pinia'
import { ref } from 'vue'
import { DEMO_USERS } from '../data/mockData'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)

  function login(role) {
    user.value = DEMO_USERS[role]
  }

  function logout() {
    user.value = null
  }

  return { user, login, logout }
})
