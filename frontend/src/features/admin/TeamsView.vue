<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const router     = useRouter()
const teams      = ref([])
const flash      = ref('')
const showModal  = ref(false)
const form       = ref({ name: '', description: '', websiteUrl: '', sectionName: '' })
const createError = ref('')

const filterName    = ref('')
const filterSection = ref('')

async function load() {
  const params = {}
  if (filterName.value.trim())    params.teamName    = filterName.value.trim()
  if (filterSection.value.trim()) params.sectionName = filterSection.value.trim()
  const res = await api.get('/api/v1/teams', params)
  teams.value = res.data
}

onMounted(load)

function clearFilters() {
  filterName.value    = ''
  filterSection.value = ''
  load()
}

async function create() {
  if (!form.value.name.trim() || !form.value.sectionName.trim()) {
    createError.value = 'Name and section are required.'
    return
  }
  createError.value = ''
  try {
    await api.post('/api/v1/teams', form.value)
    form.value = { name: '', description: '', websiteUrl: '', sectionName: '' }
    showModal.value = false
    flash.value = 'Team created.'
    setTimeout(() => flash.value = '', 3000)
    load()
  } catch (e) {
    createError.value = e.response?.data?.message ?? e.message
  }
}
</script>

<template>
  <AppLayout>
    <div v-if="flash" class="alert alert-success">{{ flash }}</div>

    <div class="flex justify-between items-center mb-4">
      <p class="muted">Create and manage Senior Design project teams.</p>
      <button class="btn btn-primary" @click="showModal = true">+ New Team</button>
    </div>

    <div class="card" style="padding:12px;margin-bottom:16px">
      <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:flex-end">
        <div class="form-group" style="margin:0;flex:1;min-width:140px">
          <label class="form-label">Team Name</label>
          <input v-model="filterName" class="form-input" placeholder="Search by name" @keyup.enter="load" />
        </div>
        <div class="form-group" style="margin:0;flex:1;min-width:140px">
          <label class="form-label">Section</label>
          <input v-model="filterSection" class="form-input" placeholder="Search by section" @keyup.enter="load" />
        </div>
        <button class="btn btn-primary btn-sm" @click="load">Search</button>
        <button class="btn btn-secondary btn-sm" @click="clearFilters">Clear</button>
      </div>
    </div>

    <div class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Description</th><th>Section</th><th></th></tr></thead>
          <tbody>
            <tr v-if="teams.length === 0">
              <td colspan="4" class="empty">No teams found.</td>
            </tr>
            <tr v-for="t in teams" :key="t.id">
              <td><strong>{{ t.name }}</strong></td>
              <td class="muted">{{ t.description ?? '—' }}</td>
              <td>{{ t.sectionName }}</td>
              <td>
                <button class="btn btn-secondary btn-sm" @click="router.push(`/admin/teams/${t.id}`)">View</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showModal" class="overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-head"><h3>Create Team</h3><button class="modal-close" @click="showModal = false">×</button></div>
        <div class="modal-body">
          <div v-if="createError" class="alert alert-error">{{ createError }}</div>
          <div class="form-group"><label class="form-label">Team Name *</label><input v-model="form.name" class="form-input" placeholder="e.g. Team Delta" /></div>
          <div class="form-group"><label class="form-label">Section *</label><input v-model="form.sectionName" class="form-input" placeholder="e.g. CS4903-001" /></div>
          <div class="form-group"><label class="form-label">Description</label><textarea v-model="form.description" class="form-input" placeholder="Brief project description" /></div>
          <div class="form-group"><label class="form-label">Website URL</label><input v-model="form.websiteUrl" class="form-input" placeholder="https://" /></div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
          <button class="btn btn-primary" @click="create">Create</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
