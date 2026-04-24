<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { usersApi } from '../../api/users'

const auth    = useAuthStore()
const name    = ref('')
const email   = ref('')
const profile = ref(null)
const loading = ref(true)
const saving  = ref(false)
const success = ref(null)
const error   = ref(null)

const originalName  = ref('')
const originalEmail = ref('')
const dialogOpen    = ref(false)

const pendingChanges = computed(() => {
  const changes = []
  if (name.value !== originalName.value)   changes.push({ field: 'Full Name', from: originalName.value,  to: name.value })
  if (email.value !== originalEmail.value) changes.push({ field: 'Email',     from: originalEmail.value, to: email.value })
  return changes
})

onMounted(async () => {
  try {
    const res = await usersApi.getProfile()
    profile.value = res.data
    name.value  = res.data.name
    email.value = res.data.email
    originalName.value  = res.data.name
    originalEmail.value = res.data.email
  } catch {
    error.value = 'Failed to load profile.'
  } finally {
    loading.value = false
  }
})

function requestSave() {
  error.value   = null
  success.value = null
  if (auth.user?.role === 'student') {
    dialogOpen.value = true
  } else {
    doSave()
  }
}

async function doSave() {
  dialogOpen.value = false
  saving.value = true
  try {
    const res = await usersApi.updateProfile({ name: name.value, email: email.value })
    auth.updateSession(res.data)
    originalName.value  = name.value
    originalEmail.value = email.value
    success.value = 'Profile updated.'
  } catch (e) {
    error.value = e.message || 'Failed to save profile.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AppLayout>
    <div class="settings-page">
      <h2 class="page-heading">Account Settings</h2>

      <div v-if="loading" class="card" style="color:#888;padding:24px">Loading…</div>

      <template v-else>
        <!-- Profile -->
        <div class="card">
          <h3 class="card-title">Profile</h3>

          <div v-if="success" class="alert alert-success">{{ success }}</div>
          <div v-if="error"   class="alert alert-error">{{ error }}</div>

          <div class="field">
            <label class="field-label">Full Name</label>
            <input v-model="name" type="text" class="field-input" placeholder="Your name" />
          </div>
          <div class="field">
            <label class="field-label">Email</label>
            <input v-model="email" type="email" class="field-input" placeholder="you@tcu.edu" />
          </div>

          <button class="btn btn-primary" :disabled="saving || pendingChanges.length === 0" @click="requestSave">
            {{ saving ? 'Saving…' : 'Save Changes' }}
          </button>
        </div>

        <!-- Student-only details -->
        <div v-if="auth.user?.role === 'student'" class="card">
          <h3 class="card-title">Student Details</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">Team</span>
              <span class="detail-value">{{ profile?.teamName ?? 'Not on a Team' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Section</span>
              <span class="detail-value">{{ profile?.sectionName ?? "Haven't been assigned to a section yet" }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Instructor</span>
              <span class="detail-value" v-if="profile?.instructorName">
                {{ profile.instructorName }}
                <span class="detail-sub">{{ profile.instructorEmail }}</span>
              </span>
              <span class="detail-value" v-else>Not assigned</span>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Student profile-change confirmation dialog -->
    <div v-if="dialogOpen" class="dialog-backdrop" @click.self="dialogOpen = false">
      <div class="dialog">
        <h3 class="dialog-title">Confirm Profile Settings</h3>

        <p class="dialog-policy">
          These settings are public to other students, instructors, and admins. Please do not enter
          inappropriate messages and follow your class and university guidelines. Please inform your
          instructor about changes to your name or contact as soon as possible.
        </p>

        <div v-if="pendingChanges.length" class="changes-table">
          <div class="changes-header">
            <span>Field</span><span>Current</span><span>New</span>
          </div>
          <div v-for="c in pendingChanges" :key="c.field" class="changes-row">
            <span class="change-field">{{ c.field }}</span>
            <span class="change-old">{{ c.from }}</span>
            <span class="change-new">{{ c.to }}</span>
          </div>
        </div>
        <p v-else class="no-changes">No changes detected.</p>

        <div class="dialog-actions">
          <button class="btn btn-ghost" @click="dialogOpen = false">Cancel</button>
          <button class="btn btn-primary" :disabled="!pendingChanges.length" @click="doSave">
            Confirm Profile Settings
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.settings-page { max-width: 560px; }
.page-heading  { font-size: 1.4rem; font-weight: 700; color: #4D1979; margin-bottom: 20px; }
.card          { background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,.07); }
.card-title    { font-size: 1rem; font-weight: 700; color: #4D1979; margin-bottom: 18px; }
.field         { margin-bottom: 14px; }
.field-label   { display: block; font-size: .82rem; font-weight: 600; color: #4a4060; margin-bottom: 5px; }
.field-input   { width: 100%; padding: 9px 12px; border: 1.5px solid #d8d3e3; border-radius: 8px; font-size: .9rem; box-sizing: border-box; outline: none; transition: border-color .15s; }
.field-input:focus { border-color: #4D1979; }
.btn           { display: inline-flex; align-items: center; padding: 9px 20px; border-radius: 8px; font-size: .88rem; font-weight: 600; cursor: pointer; border: none; }
.btn-primary   { background: #4D1979; color: #fff; }
.btn-primary:hover:not(:disabled) { background: #3a1159; }
.btn-ghost     { background: rgba(77,25,121,.08); color: #4D1979; }
.btn-ghost:hover { background: rgba(77,25,121,.15); }
.btn:disabled  { opacity: .55; cursor: default; }
.alert         { border-radius: 8px; padding: 10px 14px; font-size: .85rem; margin-bottom: 14px; }
.alert-success { background: #e8f5e9; border: 1px solid #a5d6a7; color: #2e7d32; }
.alert-error   { background: #fdecea; border: 1px solid #f5a3a3; color: #c62828; }
.detail-grid   { display: flex; flex-direction: column; gap: 14px; }
.detail-item   { display: flex; flex-direction: column; gap: 3px; }
.detail-label  { font-size: .75rem; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; color: #9e9aaa; }
.detail-value  { font-size: .92rem; color: #2d2540; font-weight: 500; }
.detail-sub    { display: block; font-size: .82rem; color: #6b6480; font-weight: 400; margin-top: 1px; }

.dialog-backdrop {
  position: fixed; inset: 0;
  background: rgba(0,0,0,.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 200;
}
.dialog {
  background: #fff; border-radius: 14px; padding: 28px 32px;
  width: 100%; max-width: 480px;
  box-shadow: 0 20px 60px rgba(0,0,0,.25);
}
.dialog-title  { font-size: 1.1rem; font-weight: 700; color: #4D1979; margin-bottom: 12px; }
.dialog-policy { font-size: .85rem; color: #5a5070; line-height: 1.55; background: #fdf6ff; border: 1px solid #dfc8f5; border-radius: 8px; padding: 12px 14px; margin-bottom: 18px; }
.changes-table { border: 1px solid #e5e0ed; border-radius: 8px; overflow: hidden; margin-bottom: 20px; font-size: .85rem; }
.changes-header { display: grid; grid-template-columns: 1fr 1.5fr 1.5fr; background: #f5f0fb; padding: 8px 12px; font-weight: 700; color: #4D1979; font-size: .78rem; text-transform: uppercase; letter-spacing: .04em; }
.changes-row    { display: grid; grid-template-columns: 1fr 1.5fr 1.5fr; padding: 9px 12px; border-top: 1px solid #ede8f5; }
.change-field  { font-weight: 600; color: #4a4060; }
.change-old    { color: #888; text-decoration: line-through; word-break: break-all; }
.change-new    { color: #2d7d32; font-weight: 600; word-break: break-all; }
.no-changes    { font-size: .85rem; color: #888; margin-bottom: 16px; }
.dialog-actions { display: flex; gap: 10px; justify-content: flex-end; }
</style>
