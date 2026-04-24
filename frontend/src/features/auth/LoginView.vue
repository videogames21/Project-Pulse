<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { defaultRoute } from '../../router/index'

const auth    = useAuthStore()
const router  = useRouter()
const route   = useRoute()

const email    = ref('')
const password = ref('')
const error    = ref(null)
const loading  = ref(false)

async function submit() {
  error.value = null
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    router.push(defaultRoute(auth.user.role))
  } catch (e) {
    error.value = e.message || 'Login failed.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-logo">P</div>
      <h1 class="login-title">Project Pulse</h1>
      <p class="login-sub">TCU Senior Design — Student Performance Tracking</p>

      <div v-if="route.query.registered" class="alert alert-success" style="margin-bottom:16px">
        Account created! Please sign in.
      </div>

      <div v-if="route.query.passwordChanged" class="alert alert-success" style="margin-bottom:16px">
        Password changed successfully. Please sign in with your new password.
      </div>

      <div v-if="route.query.linkNotFound" class="alert alert-warning" style="margin-bottom:16px">
        Invitation link not found. Please contact your admin for a new link.
      </div>

      <div v-if="error" class="alert alert-warning" style="margin-bottom:16px">
        {{ error }}
      </div>

      <form @submit.prevent="submit">
        <div class="field">
          <label class="field-label">Email</label>
          <input v-model="email" type="email" class="field-input" placeholder="you@tcu.edu" required autocomplete="email" />
        </div>

        <div class="field">
          <label class="field-label">Password</label>
          <input v-model="password" type="password" class="field-input" required autocomplete="current-password" />
        </div>

        <button type="submit" class="btn btn-primary" style="width:100%;justify-content:center;padding:11px;font-size:.95rem;margin-top:4px" :disabled="loading">
          {{ loading ? 'Signing in…' : 'Sign In' }}
        </button>
      </form>

      <p class="login-footer">Texas Christian University · Dept. of Computer Science</p>
    </div>
  </div>
</template>

<style scoped>
.login-page  { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #3a1159, #4D1979 55%, #6b2ba0); padding: 20px; }
.login-panel { background: #fff; border-radius: 14px; padding: 36px; width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.login-logo  { display: inline-flex; align-items: center; justify-content: center; width: 52px; height: 52px; background: #4D1979; color: #fff; border-radius: 12px; font-size: 1.5rem; font-weight: 800; margin-bottom: 10px; }
.login-title { font-size: 1.7rem; font-weight: 800; color: #4D1979; }
.login-sub   { color: #6b6480; font-size: .85rem; margin-top: 3px; margin-bottom: 24px; }
.field       { margin-bottom: 14px; }
.field-label { display: block; font-size: .82rem; font-weight: 600; color: #4a4060; margin-bottom: 5px; }
.field-input { width: 100%; padding: 9px 12px; border: 1.5px solid #d8d3e3; border-radius: 8px; font-size: .9rem; box-sizing: border-box; outline: none; transition: border-color .15s; }
.field-input:focus { border-color: #4D1979; }
.login-footer { text-align: center; font-size: .72rem; color: #9e9aaa; margin-top: 20px; }
.alert-success { background: #e8f5e9; border: 1px solid #a5d6a7; color: #2e7d32; border-radius: 8px; padding: 10px 14px; font-size: .85rem; }
.alert-warning { background: #fff3e0; border: 1px solid #ffcc80; color: #e65100; border-radius: 8px; padding: 10px 14px; font-size: .85rem; }
</style>
