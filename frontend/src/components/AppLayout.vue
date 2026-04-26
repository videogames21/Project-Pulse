<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNotificationsStore } from '../stores/notifications'
import { defaultRoute } from '../router/index'

const auth   = useAuthStore()
const notifs = useNotificationsStore()
const router = useRouter()
const route  = useRoute()

const menuOpen = ref(false)

function toggleMenu() { menuOpen.value = !menuOpen.value }
function closeMenu()  { menuOpen.value = false }

function handleOutsideClick(e) {
  if (!e.target.closest('.sidebar-footer')) closeMenu()
}

onMounted(() => {
  document.addEventListener('click', handleOutsideClick)
  if (auth.user?.id) notifs.fetchFromServer(auth.user.id)
})
onUnmounted(() => document.removeEventListener('click', handleOutsideClick))

const NAV = {
  student: [
    { path: '/war',       label: 'Weekly Activity Report', icon: '📋' },
    { path: '/peer-eval', label: 'Peer Evaluation',        icon: '⭐' },
    { path: '/my-report', label: 'My Report',              icon: '📊' },
    { path: '/team-war',  label: 'Team WAR Report',        icon: '👥' },
  ],
  instructor: [
    { path: '/section-report', label: 'Section Peer Eval Report', icon: '📊' },
    { path: '/team-war',       label: 'Team WAR Report',          icon: '📋' },
    { path: '/students',       label: 'Students',                 icon: '👨‍🎓' },
  ],
  admin: [
    { path: '/admin/sections',      label: 'Sections',       icon: '🏛️' },
    { path: '/admin/teams',         label: 'Teams',          icon: '👥' },
    { path: '/admin/instructors',   label: 'Instructors',    icon: '🎓' },
    { path: '/admin/rubrics',       label: 'Rubrics',        icon: '📝' },
    { path: '/admin/invitations',   label: 'Invitations',    icon: '✉️' },
    { path: '/admin/assign-students', label: 'Assign Students', icon: '🎓' },
    { path: '/admin/students',        label: 'Students',        icon: '👨‍🎓' },
  ],
}

const navItems = computed(() => NAV[auth.user?.role] ?? [])

const pageTitle = computed(() => {
  const item = navItems.value.find(n => n.path === route.path)
  return item?.label ?? ''
})

const alertClass = (type) =>
  type === 'warning' ? 'alert-warning' :
  type === 'error'   ? 'alert-error'   :
  type === 'success' ? 'alert-success' : 'alert-info'

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

      <div class="sidebar-footer" style="position:relative">
        <div class="user-menu-trigger" @click.stop="toggleMenu">
          <div class="user-name">{{ auth.user?.name }}</div>
          <div class="user-role">{{ auth.user?.role }} <span style="font-size:.65rem;opacity:.7">{{ menuOpen ? '▼' : '▲' }}</span></div>
        </div>
        <div v-if="menuOpen" class="user-menu">
          <div class="user-menu-item" @click="closeMenu(); router.push('/account/settings')">Account Settings</div>
          <div class="user-menu-item" @click="closeMenu(); router.push('/account/password')">Change Password</div>
          <div class="user-menu-divider" />
          <div class="user-menu-item user-menu-signout" @click="logout">Sign Out</div>
        </div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <span class="topbar-title">{{ pageTitle }}</span>
        <span class="topbar-sub">{{ auth.user?.team ? auth.user.team + ' · ' : '' }}{{ auth.user?.section ?? '' }}</span>
      </header>

      <!-- Server notifications (e.g. team removal alerts) -->
      <div v-if="notifs.serverNotifs.length" style="padding:0 24px;padding-top:16px">
        <div
          v-for="n in notifs.serverNotifs"
          :key="n.id"
          class="alert alert-warning"
          style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px"
        >
          <span>{{ n.message }}</span>
          <button
            @click="notifs.dismissServer(n.id)"
            style="background:none;border:none;cursor:pointer;font-size:1.1rem;padding:0 4px;opacity:.7"
            title="Dismiss"
          >×</button>
        </div>
      </div>

      <!-- Persistent notifications -->
      <div v-if="notifs.notifications.length" style="padding:0 24px;padding-top:16px">
        <div
          v-for="n in notifs.notifications"
          :key="n.id"
          :class="['alert', alertClass(n.type)]"
          style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px"
        >
          <span>{{ n.message }}</span>
          <button
            @click="notifs.dismiss(n.id)"
            style="background:none;border:none;cursor:pointer;font-size:1.1rem;padding:0 4px;opacity:.7"
          >×</button>
        </div>
      </div>

      <main class="page">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.user-menu-trigger {
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  transition: background .15s;
}
.user-menu-trigger:hover { background: rgba(255,255,255,.1); }

.user-menu {
  position: absolute;
  bottom: calc(100% + 6px);
  left: 0; right: 0;
  background: #3a1159;
  border: 1px solid rgba(255,255,255,.15);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0,0,0,.4);
  z-index: 100;
}
.user-menu-item {
  padding: 10px 14px;
  font-size: .85rem;
  color: rgba(255,255,255,.88);
  cursor: pointer;
  transition: background .12s;
}
.user-menu-item:hover { background: rgba(255,255,255,.12); }
.user-menu-divider { height: 1px; background: rgba(255,255,255,.12); margin: 3px 0; }
.user-menu-signout { color: #ff9a9a; }
.user-menu-signout:hover { background: rgba(255,80,80,.18); }
</style>
