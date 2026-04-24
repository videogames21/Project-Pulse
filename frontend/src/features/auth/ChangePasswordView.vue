<script setup>
import { ref, computed } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { usersApi } from '../../api/users'

const auth   = useAuthStore()
const router = useRouter()

const newPassword     = ref('')
const confirmPassword = ref('')
const currentPassword = ref('')
const dialogOpen      = ref(false)
const dialogError     = ref(null)
const dialogLoading   = ref(false)

const mismatch    = computed(() => confirmPassword.value && newPassword.value !== confirmPassword.value)
const tooShort    = computed(() => newPassword.value.length > 0 && newPassword.value.length < 8)
const canSubmit   = computed(() => newPassword.value.length >= 8 && newPassword.value === confirmPassword.value)

function openDialog() {
  currentPassword.value = ''
  dialogError.value = null
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
}

async function confirmChange() {
  dialogError.value = null
  dialogLoading.value = true
  try {
    await usersApi.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    })
    dialogOpen.value = false
    auth.logout()
    router.push('/login?passwordChanged=true')
  } catch (e) {
    if (e.status === 401) {
      dialogError.value = 'Current password is incorrect. Please enter your original password, not your new one.'
    } else {
      dialogError.value = e.message || 'Something went wrong.'
    }
  } finally {
    dialogLoading.value = false
  }
}
</script>

<template>
  <AppLayout>
    <div class="pw-page">
      <h2 class="page-heading">Change Password</h2>

      <div class="card">
        <div class="field">
          <label class="field-label">New Password</label>
          <input v-model="newPassword" type="password" class="field-input" placeholder="Min. 8 characters" />
          <span v-if="tooShort" class="field-error">Must be at least 8 characters.</span>
        </div>
        <div class="field">
          <label class="field-label">Confirm New Password</label>
          <input v-model="confirmPassword" type="password" class="field-input" placeholder="Repeat new password" />
          <span v-if="mismatch" class="field-error">Passwords do not match.</span>
        </div>

        <button class="btn btn-primary" :disabled="!canSubmit" @click="openDialog">
          Change Password
        </button>
      </div>
    </div>

    <!-- Confirmation dialog -->
    <div v-if="dialogOpen" class="dialog-backdrop" @click.self="closeDialog">
      <div class="dialog">
        <h3 class="dialog-title">Confirm Password Change</h3>
        <p class="dialog-body">
          For your account security, enter your current password to confirm this change.
          If you have security concerns, contact your administrator.
          <strong>Note: confirming will log you out and you will need to sign in again with your new password.</strong>
        </p>

        <div v-if="dialogError" class="alert alert-error" style="margin-bottom:14px">{{ dialogError }}</div>

        <div class="field">
          <label class="field-label">Current Password</label>
          <input v-model="currentPassword" type="password" class="field-input" placeholder="Your current password" @keyup.enter="confirmChange" />
        </div>

        <div style="display:flex;gap:10px;margin-top:18px">
          <button class="btn btn-ghost" @click="closeDialog">Cancel</button>
          <button class="btn btn-primary" :disabled="!currentPassword || dialogLoading" @click="confirmChange">
            {{ dialogLoading ? 'Confirming…' : 'Confirm Change Password' }}
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.pw-page      { max-width: 480px; }
.page-heading { font-size: 1.4rem; font-weight: 700; color: #4D1979; margin-bottom: 20px; }
.card         { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,.07); }
.field        { margin-bottom: 14px; }
.field-label  { display: block; font-size: .82rem; font-weight: 600; color: #4a4060; margin-bottom: 5px; }
.field-input  { width: 100%; padding: 9px 12px; border: 1.5px solid #d8d3e3; border-radius: 8px; font-size: .9rem; box-sizing: border-box; outline: none; transition: border-color .15s; }
.field-input:focus { border-color: #4D1979; }
.field-error  { display: block; font-size: .78rem; color: #c62828; margin-top: 4px; }
.btn          { display: inline-flex; align-items: center; padding: 9px 20px; border-radius: 8px; font-size: .88rem; font-weight: 600; cursor: pointer; border: none; }
.btn-primary  { background: #4D1979; color: #fff; }
.btn-primary:hover:not(:disabled) { background: #3a1159; }
.btn-ghost    { background: rgba(77,25,121,.08); color: #4D1979; }
.btn-ghost:hover { background: rgba(77,25,121,.15); }
.btn:disabled { opacity: .55; cursor: default; }
.alert        { border-radius: 8px; padding: 10px 14px; font-size: .85rem; }
.alert-success { background: #e8f5e9; border: 1px solid #a5d6a7; color: #2e7d32; }
.alert-error  { background: #fdecea; border: 1px solid #f5a3a3; color: #c62828; }

.dialog-backdrop {
  position: fixed; inset: 0;
  background: rgba(0,0,0,.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 200;
}
.dialog {
  background: #fff; border-radius: 14px; padding: 28px 32px;
  width: 100%; max-width: 420px;
  box-shadow: 0 20px 60px rgba(0,0,0,.25);
}
.dialog-title { font-size: 1.1rem; font-weight: 700; color: #4D1979; margin-bottom: 10px; }
.dialog-body  { font-size: .87rem; color: #5a5070; margin-bottom: 18px; line-height: 1.5; }
</style>
