import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

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

  return { notifications, add, dismiss, clearAll }
})
