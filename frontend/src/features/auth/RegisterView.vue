<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { invitationsApi } from '../../api/invitations.js'

const route      = useRoute()
const loading    = ref(true)
const validInvite = ref(false)
const inviteData = ref(null)

onMounted(async () => {
  try {
    const res = await invitationsApi.validateToken(route.params.token)
    inviteData.value = res.data
    validInvite.value = true
  } catch {
    validInvite.value = false
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;background:var(--bg)">
    <div class="card" style="max-width:460px;width:100%;padding:32px">

      <div v-if="loading" style="text-align:center;color:var(--text-muted)">
        Validating invitation link…
      </div>

      <div v-else-if="validInvite">
        <h2 style="margin:0 0 8px 0">Welcome to Project Pulse</h2>
        <p class="muted" style="margin:0 0 16px 0">You've been invited to join as a student.</p>
        <div class="alert alert-info">
          Account setup is coming soon. Check back after your admin notifies you.
        </div>
        <RouterLink to="/login" style="display:inline-block;margin-top:16px">Return to Login</RouterLink>
      </div>

      <div v-else>
        <h2 style="margin:0 0 16px 0">Invalid Invitation</h2>
        <div class="alert alert-warning">
          This invitation link is invalid or has expired. Please contact your admin.
        </div>
        <RouterLink to="/login" style="display:inline-block;margin-top:16px">Return to Login</RouterLink>
      </div>

    </div>
  </div>
</template>
