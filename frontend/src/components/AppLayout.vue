<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { defaultRoute } from '../router/index'
import { api } from '../services/api.js'

const auth   = useAuthStore()
const router = useRouter()
const route  = useRoute()

const notifications     = ref([])
const showNotifications = ref(false)

async function loadNotifications() {
  if (auth.user?.role !== 'student') return
  try {
    const res = await api.get(`/api/v1/notifications?userId=${auth.user.id}`)
    notifications.value = res.data ?? []
  } catch { /* silently ignore */ }
}

async function dismissNotification(id) {
  try {
    await api.patch(`/api/v1/notifications/${id}/seen?userId=${auth.user.id}`)
    notifications.value = notifications.value.filter(n => n.id !== id)
  } catch { /* silently ignore */ }
}

onMounted(loadNotifications)

const NAV = {
  student: [
    { path: '/war',       label: 'Weekly Activity Report', icon: '📋' },
    { path: '/peer-eval', label: 'Peer Evaluation',        icon: '⭐' },
    { path: '/my-report', label: 'My Report',              icon: '📊' },
  ],
  instructor: [
    { path: '/section-report', label: 'Section Peer Eval Report', icon: '📊' },
    { path: '/team-war',       label: 'Team WAR Report',          icon: '📋' },
  ],
  admin: [
    { path: '/admin/sections',    label: 'Sections',    icon: '🏛️' },
    { path: '/admin/teams',       label: 'Teams',       icon: '👥' },
    { path: '/admin/rubrics',     label: 'Rubrics',     icon: '📝' },
    { path: '/admin/invitations', label: 'Invitations', icon: '✉️' },
  ],
}

const navItems = computed(() => NAV[auth.user?.role] ?? [])

const pageTitle = computed(() => {
  const item = navItems.value.find(n => n.path === route.path)
  return item?.label ?? ''
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <h2>Project Pulse</h2>
        <p>TCU Senior Design</p>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-label">Navigation</div>
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          custom
          v-slot="{ navigate, isActive }"
        >
          <div :class="['nav-item', { active: isActive }]" @click="navigate">
            <span>{{ item.icon }}</span>
            {{ item.label }}
          </div>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="user-name">{{ auth.user?.name }}</div>
        <div class="user-role">{{ auth.user?.role }}</div>
        <button class="btn btn-sm" style="width:100%;justify-content:center;background:rgba(255,255,255,.12);color:#fff;border:1px solid rgba(255,255,255,.2)" @click="logout">
          Sign Out
        </button>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <span class="topbar-title">{{ pageTitle }}</span>
        <div style="display:flex;align-items:center;gap:16px">
          <span class="topbar-sub">{{ auth.user?.team ? auth.user.team + ' · ' : '' }}{{ auth.user?.section ?? '' }}</span>
          <!-- Notification bell (students only) -->
          <div v-if="auth.user?.role === 'student'" style="position:relative">
            <button
              style="background:none;border:none;cursor:pointer;font-size:1.2rem;position:relative;padding:4px"
              @click="showNotifications = !showNotifications"
              :aria-label="`${notifications.length} unread notifications`"
            >
              🔔
              <span
                v-if="notifications.length"
                style="position:absolute;top:0;right:0;background:var(--gold,#c69214);color:#fff;border-radius:50%;font-size:.6rem;width:16px;height:16px;display:flex;align-items:center;justify-content:center;font-weight:700"
              >{{ notifications.length }}</span>
            </button>
            <!-- Dropdown -->
            <div
              v-if="showNotifications"
              style="position:absolute;right:0;top:110%;width:300px;background:#fff;border:1px solid var(--border,#e2e8f0);border-radius:8px;box-shadow:0 4px 12px rgba(0,0,0,.12);z-index:100;max-height:320px;overflow-y:auto"
            >
              <div style="padding:12px 16px;font-weight:600;border-bottom:1px solid var(--border,#e2e8f0)">Notifications</div>
              <div v-if="notifications.length === 0" style="padding:16px;color:var(--muted,#64748b);font-size:.875rem">No new notifications</div>
              <div
                v-for="n in notifications"
                :key="n.id"
                style="padding:12px 16px;border-bottom:1px solid var(--border,#e2e8f0);display:flex;justify-content:space-between;align-items:flex-start;gap:8px"
              >
                <span style="font-size:.875rem">{{ n.message }}</span>
                <button
                  style="background:none;border:none;cursor:pointer;color:var(--muted,#64748b);font-size:.75rem;white-space:nowrap;flex-shrink:0"
                  @click="dismissNotification(n.id)"
                >Dismiss</button>
              </div>
            </div>
          </div>
        </div>
      </header>
      <main class="page">
        <slot />
      </main>
    </div>
  </div>
</template>
