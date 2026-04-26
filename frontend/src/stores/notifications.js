import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { notificationsApi } from '../api/notifications.js'

const STORAGE_KEY = 'pp_notifications'

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const useNotificationsStore = defineStore('notifications', () => {
  // ── Local flash notifications (client-side only, persisted in localStorage) ──
  const notifications = ref(load())

  function add(type, message) {
    notifications.value.push({ id: Date.now(), type, message })
    persist()
  }

  function dismiss(id) {
    notifications.value = notifications.value.filter(n => n.id !== id)
    persist()
  }

  function clearAll() {
    notifications.value = []
    persist()
  }

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(notifications.value))
  }

  // ── Server notifications (persisted in DB, fetched on login) ─────────────────
  const serverNotifs = ref([])

  async function fetchFromServer(userId) {
    if (!userId) return
    try {
      const res = await notificationsApi.getUnread(userId)
      serverNotifs.value = res.data ?? []
    } catch {
      // silently ignore — network issues should not block the UI
    }
  }

  async function dismissServer(id) {
    try {
      await notificationsApi.markRead(id)
    } catch {
      // best-effort — remove from local list regardless
    }
    serverNotifs.value = serverNotifs.value.filter(n => n.id !== id)
  }

  return { notifications, add, dismiss, clearAll, serverNotifs, fetchFromServer, dismissServer }
})
