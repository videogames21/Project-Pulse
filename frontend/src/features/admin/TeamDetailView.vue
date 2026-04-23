<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const route  = useRoute()
const router = useRouter()
const team   = ref(null)
const error  = ref('')

const editing    = ref(false)
const saving     = ref(false)
const saveError  = ref('')
const form       = ref({ name: '', description: '', websiteUrl: '', sectionName: '' })

onMounted(async () => {
  try {
    const res = await api.get(`/api/v1/teams/${route.params.id}`)
    team.value = res.data
  } catch (e) {
    error.value = e.message
  }
})

function startEdit() {
  form.value = { ...team.value }
  saveError.value = ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  saveError.value = ''
}

async function saveEdit() {
  if (!form.value.name?.trim() || !form.value.sectionName?.trim()) {
    saveError.value = 'Name and section are required.'
    return
  }
  saving.value = true
  saveError.value = ''
  try {
    const res = await api.put(`/api/v1/teams/${route.params.id}`, form.value)
    team.value = res.data
    editing.value = false
  } catch (e) {
    saveError.value = e.response?.data?.message ?? e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AppLayout>
    <div class="flex items-center gap-2 mb-4" style="margin-bottom:16px">
      <button class="btn btn-secondary btn-sm" @click="router.back()">← Back</button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="!team" class="empty">Loading...</div>

    <template v-else>
      <!-- View mode -->
      <div v-if="!editing" class="card">
        <div class="card-header">
          <h2 style="font-size:1.1rem;font-weight:700">{{ team.name }}</h2>
          <span class="badge badge-purple">{{ team.sectionName }}</span>
          <button class="btn btn-primary btn-sm" style="margin-left:auto" @click="startEdit">Edit</button>
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

      <!-- Edit mode -->
      <div v-else class="card">
        <div class="card-header">
          <h2 style="font-size:1.1rem;font-weight:700">Edit Team</h2>
        </div>

        <div v-if="saveError" class="alert alert-error" style="margin-bottom:12px">{{ saveError }}</div>

        <div class="form-group">
          <label class="form-label">Team Name *</label>
          <input v-model="form.name" class="form-input" placeholder="Team name" />
        </div>

        <div class="form-group">
          <label class="form-label">Section *</label>
          <input v-model="form.sectionName" class="form-input" placeholder="Section name" />
        </div>

        <div class="form-group">
          <label class="form-label">Description</label>
          <input v-model="form.description" class="form-input" placeholder="Optional description" />
        </div>

        <div class="form-group">
          <label class="form-label">Website URL</label>
          <input v-model="form.websiteUrl" class="form-input" placeholder="https://..." />
        </div>

        <div style="display:flex;gap:8px;margin-top:16px">
          <button class="btn btn-primary" :disabled="saving" @click="saveEdit">
            {{ saving ? 'Saving…' : 'Save' }}
          </button>
          <button class="btn btn-secondary" :disabled="saving" @click="cancelEdit">Cancel</button>
        </div>
      </div>
    </template>
  </AppLayout>
</template>
