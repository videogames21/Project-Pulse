<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { invitationsApi } from '../../api/invitations.js'
import { useAuthStore } from '../../stores/auth'

const route      = useRoute()
const router     = useRouter()
const auth       = useAuthStore()

const loading      = ref(true)
const validInvite  = ref(false)
const linkDisabled = ref(false)
const linkAccepted = ref(false)
const inviteData   = ref(null)

const isInstructor = computed(() => inviteData.value?.role === 'INSTRUCTOR')

const form = ref({
  firstName: '', middleInitial: '', lastName: '',
  email: '', password: '', confirmPassword: '', accessCode: '',
})
const fieldErrors = ref({})
const submitError = ref(null)
const submitting  = ref(false)

const passwordMismatch = computed(() =>
  isInstructor.value && form.value.confirmPassword !== '' && form.value.password !== form.value.confirmPassword
)

onMounted(async () => {
  try {
    const res = await invitationsApi.validateToken(route.params.token)
    if (res.data?.status === 'DISABLED') {
      linkDisabled.value = true
    } else if (res.data?.status === 'ACCEPTED') {
      linkAccepted.value = true
    } else {
      inviteData.value = res.data
      validInvite.value = true
    }
  } catch (e) {
    if (e.status === 404) {
      router.push('/login?linkNotFound=true')
    }
  } finally {
    loading.value = false
  }
})

async function submit() {
  submitError.value = null
  fieldErrors.value = {}

  if (isInstructor.value && form.value.password !== form.value.confirmPassword) {
    submitError.value = 'Passwords do not match.'
    return
  }

  submitting.value = true
  try {
    const payload = {
      firstName: form.value.firstName,
      lastName:  form.value.lastName,
      email:     form.value.email,
      password:  form.value.password,
      token:     route.params.token,
    }
    if (isInstructor.value) {
      payload.middleInitial = form.value.middleInitial || null
      payload.accessCode    = form.value.accessCode
    }
    await auth.register(payload)
    router.push('/login?registered=true')
  } catch (e) {
    if (e.status === 400 && e.data?.data) {
      fieldErrors.value = e.data.data
    } else if (e.status === 400 && e.message?.toLowerCase().includes('access code')) {
      submitError.value = e.message
    } else if (e.status === 409) {
      submitError.value = 'This link has already been used. Please contact your admin for a new link.'
    } else if (e.status === 410) {
      submitError.value = e.message
    } else {
      submitError.value = e.message || 'Registration failed.'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="reg-page">
    <div class="reg-panel">
      <div class="login-logo">P</div>
      <h1 class="reg-title">Project Pulse</h1>

      <div v-if="loading" style="text-align:center;color:#6b6480;padding:24px 0">
        Validating invitation link…
      </div>

      <!-- Valid invite form -->
      <div v-else-if="validInvite">
        <p class="reg-sub">{{ isInstructor ? 'Set up your instructor account.' : 'Create your account to get started.' }}</p>
        <div v-if="!isInstructor && inviteData?.sectionName" class="section-notice">
          You are signing up for Section <strong>{{ inviteData.sectionName }}</strong>
        </div>

        <div v-if="submitError" class="alert alert-warning" style="margin-bottom:14px">
          {{ submitError }}
        </div>

        <form @submit.prevent="submit">
          <!-- Name row -->
          <div class="row-fields" :style="isInstructor ? 'grid-template-columns:1fr .4fr 1fr' : ''">
            <div class="field">
              <label class="field-label">First Name</label>
              <input v-model="form.firstName" type="text" class="field-input" :class="{ error: fieldErrors.firstName }" placeholder="Alice" required />
              <span v-if="fieldErrors.firstName" class="field-error">{{ fieldErrors.firstName }}</span>
            </div>
            <div v-if="isInstructor" class="field">
              <label class="field-label">M.I.</label>
              <input v-model="form.middleInitial" type="text" class="field-input" maxlength="1" placeholder="A" />
            </div>
            <div class="field">
              <label class="field-label">Last Name</label>
              <input v-model="form.lastName" type="text" class="field-input" :class="{ error: fieldErrors.lastName }" placeholder="Chen" required />
              <span v-if="fieldErrors.lastName" class="field-error">{{ fieldErrors.lastName }}</span>
            </div>
          </div>

          <div class="field">
            <label class="field-label">Email</label>
            <input v-model="form.email" type="email" class="field-input" :class="{ error: fieldErrors.email }" placeholder="you@tcu.edu" required autocomplete="email" />
            <span v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</span>
          </div>

          <div class="field">
            <label class="field-label">Password <span class="muted-label">(min 8 characters)</span></label>
            <input v-model="form.password" type="password" class="field-input" :class="{ error: fieldErrors.password }" required autocomplete="new-password" />
            <span v-if="fieldErrors.password" class="field-error">{{ fieldErrors.password }}</span>
          </div>

          <!-- Confirm password — instructor only -->
          <div v-if="isInstructor" class="field">
            <label class="field-label">Confirm Password</label>
            <input v-model="form.confirmPassword" type="password" class="field-input" :class="{ error: passwordMismatch }" required autocomplete="new-password" />
            <span v-if="passwordMismatch" class="field-error">Passwords do not match.</span>
          </div>

          <!-- Access code — instructor only -->
          <div v-if="isInstructor" class="field">
            <label class="field-label">Access Code <span class="muted-label">(provided by admin)</span></label>
            <input v-model="form.accessCode" type="text" class="field-input" :class="{ error: fieldErrors.accessCode }" maxlength="6" placeholder="a1B2c3" required />
            <span v-if="fieldErrors.accessCode" class="field-error">{{ fieldErrors.accessCode }}</span>
          </div>

          <button
            type="submit"
            class="btn btn-primary"
            style="width:100%;justify-content:center;padding:11px;font-size:.95rem;margin-top:4px"
            :disabled="submitting || (isInstructor && passwordMismatch)"
          >
            {{ submitting ? 'Creating account…' : (isInstructor ? 'Create Instructor Account' : 'Create Account') }}
          </button>
        </form>

        <p style="text-align:center;margin-top:16px;font-size:.82rem">
          Already have an account? <RouterLink to="/login">Sign in</RouterLink>
        </p>
      </div>

      <!-- Disabled link -->
      <div v-else-if="linkDisabled">
        <h2 style="margin:0 0 16px 0;color:#4D1979">Invitation Link Disabled</h2>
        <div class="alert alert-disabled" style="margin-bottom:16px">
          This invitation link has been disabled by an administrator. Please contact your admin for a new link.
        </div>
        <RouterLink to="/login" style="display:inline-block;margin-top:4px">Return to Login</RouterLink>
      </div>

      <!-- Already-used instructor link -->
      <div v-else-if="linkAccepted">
        <h2 style="margin:0 0 16px 0;color:#4D1979">Invitation Link Already Used</h2>
        <div class="alert alert-disabled" style="margin-bottom:16px">
          This invitation link has already been used to create an account. Please contact your admin for a new link.
        </div>
        <RouterLink to="/login" style="display:inline-block;margin-top:4px">Return to Login</RouterLink>
      </div>

      <!-- Not found fallback (shouldn't normally show — we redirect) -->
      <div v-else>
        <h2 style="margin:0 0 16px 0;color:#c62828">Invalid Invitation</h2>
        <div class="alert alert-warning">
          This invitation link is not valid. Please contact your admin for a new link.
        </div>
        <RouterLink to="/login" style="display:inline-block;margin-top:16px">Return to Login</RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reg-page   { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #3a1159, #4D1979 55%, #6b2ba0); padding: 20px; }
.reg-panel  { background: #fff; border-radius: 14px; padding: 36px; width: 100%; max-width: 520px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.login-logo { display: inline-flex; align-items: center; justify-content: center; width: 52px; height: 52px; background: #4D1979; color: #fff; border-radius: 12px; font-size: 1.5rem; font-weight: 800; margin-bottom: 10px; }
.reg-title  { font-size: 1.7rem; font-weight: 800; color: #4D1979; margin: 0 0 4px 0; }
.reg-sub    { color: #6b6480; font-size: .85rem; margin: 0 0 20px 0; }
.row-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field      { margin-bottom: 14px; }
.field-label  { display: block; font-size: .82rem; font-weight: 600; color: #4a4060; margin-bottom: 5px; }
.muted-label  { font-weight: 400; color: #9e9aaa; }
.field-input  { width: 100%; padding: 9px 12px; border: 1.5px solid #d8d3e3; border-radius: 8px; font-size: .9rem; box-sizing: border-box; outline: none; transition: border-color .15s; }
.field-input:focus { border-color: #4D1979; }
.field-input.error { border-color: #c62828; }
.field-error  { font-size: .75rem; color: #c62828; margin-top: 3px; display: block; }
.alert-warning  { background: #fff3e0; border: 1px solid #ffcc80; color: #e65100; border-radius: 8px; padding: 10px 14px; font-size: .85rem; }
.alert-disabled { background: #f3e8ff; border: 1px solid #c084fc; color: #7c3aed; border-radius: 8px; padding: 10px 14px; font-size: .85rem; }
.section-notice { background: #f3e8ff; border: 1.5px solid #c084fc; color: #4D1979; border-radius: 8px; padding: 10px 14px; font-size: .875rem; margin-bottom: 16px; }
</style>
