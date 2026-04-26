<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const router  = useRouter()
const teams   = ref([])
const loading = ref(false)
const error   = ref('')
const flash   = ref('')
const filterName    = ref('')
const filterSection = ref('')
const showModal = ref(false)
const saving    = ref(false)
const formError = ref('')
const form = ref({ name: '', description: '', websiteUrl: '', sectionName: '' })

async function fetchTeams() {
  loading.value = true
  error.value   = ''
  try {
    const params = new URLSearchParams()
    if (filterName.value.trim())    params.set('teamName',    filterName.value.trim())
    if (filterSection.value.trim()) params.set('sectionName', filterSection.value.trim())
    const query = params.toString() ? `?${params}` : ''
    const res = await api.get(`/api/v1/teams${query}`)
    teams.value = res.data
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function clearFilters() {
  filterName.value    = ''
  filterSection.value = ''
  fetchTeams()
}

function openModal() {
  form.value      = { name: '', description: '', websiteUrl: '', sectionName: '' }
  formError.value = ''
  showModal.value = true
}

async function create() {
  formError.value = ''
  saving.value    = true
  try {
    await api.post('/api/v1/teams', form.value)
    showModal.value = false
    flash.value = 'Team created successfully.'
    setTimeout(() => flash.value = '', 3000)
    fetchTeams()
  } catch (e) {
    formError.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(fetchTeams)
</script>

<template>
  <AppLayout>
    <div v-if="flash" class="alert alert-success">{{ flash }}</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div class="flex justify-between items-center mb-4">
      <p class="muted">Create and manage Senior Design project teams.</p>
      <button class="btn btn-primary" @click="openModal">+ New Team</button>
    </div>

    <!-- Filters -->
    <div class="card mb-4" style="margin-bottom:16px">
      <div class="flex gap-3 items-center" style="flex-wrap:wrap">
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:160px">
          <label>Team Name</label>
          <input v-model="filterName" placeholder="Search by name..." @keyup.enter="fetchTeams" />
        </div>
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:160px">
          <label>Section</label>
          <input v-model="filterSection" placeholder="Search by section..." @keyup.enter="fetchTeams" />
        </div>
        <div style="display:flex;gap:8px;align-self:flex-end;padding-bottom:1px">
          <button class="btn btn-primary" @click="fetchTeams" :disabled="loading">Search</button>
          <button class="btn btn-secondary" @click="clearFilters">Clear</button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Description</th>
              <th>Section</th>
              <th>Website</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="empty">Loading...</td>
            </tr>
            <tr v-else-if="teams.length === 0">
              <td colspan="6" class="empty">No teams found.</td>
            </tr>
            <tr v-for="t in teams" :key="t.id">
              <td class="muted">{{ t.id }}</td>
              <td><strong>{{ t.name }}</strong></td>
              <td class="muted">{{ t.description ?? '—' }}</td>
              <td>{{ t.sectionName }}</td>
              <td>
                <a v-if="t.websiteUrl" :href="t.websiteUrl" target="_blank" style="color:var(--purple)">{{ t.websiteUrl }}</a>
                <span v-else class="muted">—</span>
              </td>
              <td>
                <button class="btn btn-secondary btn-sm" @click="router.push(`/admin/teams/${t.id}`)">View</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create Modal -->
    <div v-if="showModal" class="overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-head">
          <h3>Create Team</h3>
          <button class="modal-close" @click="showModal = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="formError" class="alert alert-error">{{ formError }}</div>
          <div class="form-group">
            <label>Team Name *</label>
            <input v-model="form.name" placeholder="e.g. Team Delta" />
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea v-model="form.description" placeholder="Brief project description" />
          </div>
          <div class="form-group">
            <label>Website URL</label>
            <input v-model="form.websiteUrl" placeholder="https://" />
          </div>
          <div class="form-group">
            <label>Section Name *</label>
            <input v-model="form.sectionName" placeholder="e.g. CS4910" />
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
          <button class="btn btn-primary" @click="create" :disabled="saving">
            {{ saving ? 'Creating...' : 'Create' }}
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
