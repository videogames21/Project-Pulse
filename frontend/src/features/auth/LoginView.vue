<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { defaultRoute } from '../../router/index'

const auth    = useAuthStore()
const router  = useRouter()
const selected = ref(null)

const ROLES = [
  { key: 'student',    label: 'Student',    icon: '🎓', desc: 'Submit WARs, peer evaluations, view your report' },
  { key: 'instructor', label: 'Instructor', icon: '👩‍🏫', desc: 'View team WAR and peer eval reports' },
  { key: 'admin',      label: 'Admin',      icon: '⚙️',  desc: 'Manage sections, teams, rubrics, and users' },
]

function enter() {
  if (!selected.value) return
  auth.login(selected.value)
  router.push(defaultRoute(selected.value))
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-logo">P</div>
      <h1 class="login-title">Project Pulse</h1>
      <p class="login-sub">TCU Senior Design — Student Performance Tracking</p>

      <p class="demo-note">Demo mode — select a role to explore</p>

      <div class="role-grid">
        <button
          v-for="r in ROLES"
          :key="r.key"
          :class="['role-card', { active: selected === r.key }]"
          @click="selected = r.key"
        >
          <span class="role-icon">{{ r.icon }}</span>
          <strong>{{ r.label }}</strong>
          <span class="role-desc">{{ r.desc }}</span>
        </button>
      </div>

      <button class="btn btn-primary" style="width:100%;justify-content:center;padding:11px;font-size:.95rem" :disabled="!selected" @click="enter">
        Enter as {{ selected ? ROLES.find(r => r.key === selected)?.label : '…' }}
      </button>

      <p class="login-footer">Texas Christian University · Dept. of Computer Science</p>
    </div>
  </div>
</template>

<style scoped>
.login-page  { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #3a1159, #4D1979 55%, #6b2ba0); padding: 20px; }
.login-panel { background: #fff; border-radius: 14px; padding: 36px; width: 100%; max-width: 500px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.login-logo  { display: inline-flex; align-items: center; justify-content: center; width: 52px; height: 52px; background: #4D1979; color: #fff; border-radius: 12px; font-size: 1.5rem; font-weight: 800; margin-bottom: 10px; }
.login-title { font-size: 1.7rem; font-weight: 800; color: #4D1979; }
.login-sub   { color: #6b6480; font-size: .85rem; margin-top: 3px; margin-bottom: 20px; }
.demo-note   { text-align: center; font-size: .76rem; color: #6b6480; background: #f5f4f8; border: 1px solid #e0dbe8; border-radius: 6px; padding: 5px 10px; margin-bottom: 14px; }
.role-grid   { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; margin-bottom: 18px; }
.role-card   { display: flex; flex-direction: column; align-items: center; gap: 5px; padding: 14px 8px; border: 2px solid #e0dbe8; border-radius: 10px; background: #fff; cursor: pointer; text-align: center; transition: all .15s; }
.role-card.active { border-color: #4D1979; background: #ede7f6; }
.role-icon   { font-size: 1.7rem; }
.role-desc   { font-size: .7rem; color: #6b6480; line-height: 1.3; font-weight: 400; }
.login-footer { text-align: center; font-size: .72rem; color: #9e9aaa; margin-top: 18px; }
</style>
