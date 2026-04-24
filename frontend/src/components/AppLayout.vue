<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { defaultRoute } from '../router/index'

const auth   = useAuthStore()
const router = useRouter()
const route  = useRoute()

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
    { path: '/admin/sections',     label: 'Sections',     icon: '🏛️' },
    { path: '/admin/teams',        label: 'Teams',        icon: '👥' },
    { path: '/admin/instructors',  label: 'Instructors',  icon: '🎓' },
    { path: '/admin/rubrics',     label: 'Rubrics',     icon: '📝' },
    { path: '/admin/invitations',      label: 'Invitations',      icon: '✉️' },
    { path: '/admin/assign-students', label: 'Assign Students',  icon: '🎓' },
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
        <span class="topbar-sub">{{ auth.user?.team ? auth.user.team + ' · ' : '' }}{{ auth.user?.section ?? '' }}</span>
      </header>
      <main class="page">
        <slot />
      </main>
    </div>
  </div>
</template>
