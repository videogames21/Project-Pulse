<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { invitationsApi } from '../../api/invitations.js'

const invitations   = ref([])
const loading       = ref(false)
const generatedLink = ref('')
const copied        = ref(false)
const flash         = ref({ type: '', text: '' })

const STATUS_CLS = { PENDING: 'badge-orange', ACCEPTED: 'badge-green', EXPIRED: 'badge-gray' }

async function loadInvitations() {
  try {
    const res = await invitationsApi.getAll()
    invitations.value = res.data ?? []
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to load invitations.' }
  }
}

async function generate() {
  loading.value = true
  generatedLink.value = ''
  try {
    const res = await invitationsApi.generate()
    generatedLink.value = res.data.registrationLink
    await loadInvitations()
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to generate link. Please try again.' }
  } finally {
    loading.value = false
  }
}

async function copyLink() {
  await navigator.clipboard.writeText(generatedLink.value)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

onMounted(loadInvitations)
</script>

<template>
  <AppLayout>
    <div v-if="flash.text" :class="['alert', flash.type]" style="margin-bottom:12px">
      {{ flash.text }}
    </div>

    <div class="flex justify-between items-center mb-4">
      <p class="muted">Generate a unique invitation link and share it with students.</p>
      <button class="btn btn-primary" :disabled="loading" @click="generate">
        {{ loading ? 'Generating…' : 'Generate Link' }}
      </button>
    </div>

    <div v-if="generatedLink" class="alert alert-success" style="margin-bottom:16px">
      <p style="margin:0 0 8px 0">Invitation link generated. Copy it and share with your students.</p>
      <div class="flex items-center" style="gap:8px">
        <input
          type="text"
          :value="generatedLink"
          readonly
          style="flex:1;padding:6px 10px;border:1px solid var(--border);border-radius:6px;font-size:0.85rem;background:var(--bg)"
        />
        <button class="btn btn-secondary" @click="copyLink">
          {{ copied ? 'Copied!' : 'Copy' }}
        </button>
      </div>
    </div>

    <div class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Link ID</th><th>Generated</th><th>Status</th></tr>
          </thead>
          <tbody>
            <tr v-if="invitations.length === 0">
              <td colspan="3" style="text-align:center;color:var(--text-muted)">No invitation links yet.</td>
            </tr>
            <tr v-for="inv in invitations" :key="inv.id">
              <td><code>{{ inv.tokenShort }}…</code></td>
              <td>{{ inv.createdAt?.slice(0, 10) }}</td>
              <td>
                <span :class="['badge', STATUS_CLS[inv.status] ?? 'badge-gray']" style="text-transform:capitalize">
                  {{ inv.status?.toLowerCase() }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AppLayout>
</template>
