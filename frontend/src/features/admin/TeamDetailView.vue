<script setup>
// UC-13: Remove student from team
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const route  = useRoute()
const router = useRouter()

const team    = ref(null)
const members = ref([])
const loading = ref(true)
const error   = ref('')
const flash   = ref({ text: '', type: '' })

const removingId   = ref(null)
const removingName = ref('')

function showFlash(text, type = 'success') {
  flash.value = { text, type }
  setTimeout(() => flash.value = { text: '', type: '' }, 3500)
}

function memberName(m) {
  return m.firstName && m.lastName ? `${m.firstName} ${m.lastName}` : m.username ?? '—'
}

async function fetchTeam() {
  loading.value = true
  error.value   = ''
  try {
    const [teamRes, membersRes] = await Promise.all([
      api.get(`/api/v1/teams/${route.params.id}`),
      api.get(`/api/v1/teams/${route.params.id}/members`),
    ])
    team.value    = teamRes.data
    members.value = membersRes.data ?? []
  } catch (e) {
    error.value = e.message ?? 'Failed to load team.'
  } finally {
    loading.value = false
  }
}

function confirmRemove(member) {
  removingId.value   = member.id
  removingName.value = memberName(member)
}

async function doRemove() {
  const memberId = removingId.value
  removingId.value   = null
  removingName.value = ''
  try {
    await api.delete(`/api/v1/teams/${route.params.id}/members/${memberId}`)
    showFlash('Student removed from team.')
    members.value = members.value.filter(m => m.id !== memberId)
  } catch (e) {
    showFlash(e.message ?? 'Failed to remove student.', 'error')
  }
}

onMounted(fetchTeam)
</script>

<template>
  <AppLayout>
    <!-- Flash -->
    <div v-if="flash.text" :class="['alert', flash.type === 'error' ? 'alert-error' : 'alert-success']">
      {{ flash.text }}
    </div>

    <!-- Back -->
    <div class="flex items-center gap-2 mb-4">
      <button class="btn btn-secondary btn-sm" @click="router.push('/admin/teams')">← Back to Teams</button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-else-if="loading" class="empty"><p>Loading…</p></div>

    <template v-else-if="team">
      <!-- Team info card -->
      <div class="card mb-4" style="margin-bottom:20px">
        <div class="card-header">
          <h2 style="font-size:1.1rem;font-weight:700">{{ team.name }}</h2>
          <span class="badge badge-purple">{{ team.sectionName }}</span>
        </div>
        <div class="grid-2" style="gap:18px">
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Description</p>
            <p>{{ team.description ?? '—' }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Website</p>
            <a v-if="team.websiteUrl" :href="team.websiteUrl" target="_blank" style="color:var(--purple)">{{ team.websiteUrl }}</a>
            <p v-else class="muted">—</p>
          </div>
        </div>
      </div>

      <!-- Members table -->
      <div class="card" style="padding:0;overflow:hidden">
        <div class="card-header" style="padding:14px 18px">
          <h3>Members ({{ members.length }})</h3>
        </div>
        <div v-if="members.length === 0" class="empty">
          <p>No members on this team.</p>
        </div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in members" :key="m.id">
                <td><strong>{{ memberName(m) }}</strong></td>
                <td class="muted">{{ m.email ?? '—' }}</td>
                <td>
                  <template v-if="removingId === m.id">
                    <div class="flex gap-2 items-center">
                      <span class="muted" style="font-size:.8rem">Remove {{ removingName }}?</span>
                      <button
                        class="btn btn-sm"
                        style="background:var(--red);color:#fff;border:none"
                        @click="doRemove"
                      >Yes, Remove</button>
                      <button class="btn btn-sm btn-secondary" @click="removingId = null">Cancel</button>
                    </div>
                  </template>
                  <button
                    v-else
                    class="btn btn-danger btn-sm"
                    @click="confirmRemove(m)"
                  >Remove</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </AppLayout>
</template>
