<script setup>
// UC-27: Manage activities in a Weekly Activity Report
import { ref, computed, onMounted, watch } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'
import { WAR_CATEGORIES, WAR_STATUSES } from '../../data/mockData'

const CURRENT_WEEK = 2  // replace with real active-week logic from backend

const entries    = ref([])
const loading    = ref(false)
const error      = ref('')
const flash      = ref({ text: '', type: '' })
const selectedWeek = ref(CURRENT_WEEK)
const showModal  = ref(false)
const editing    = ref(null)
const saving     = ref(false)
const deletingId = ref(null)

const STATUS_CLS = { 'Done': 'badge-green', 'In Progress': 'badge-orange', 'Under Testing': 'badge-blue' }

const blankForm = () => ({
  category: 'DEVELOPMENT',
  description: '',
  plannedHours: '',
  actualHours: '',
  status: 'In Progress',
})
const form      = ref(blankForm())
const formError = ref('')

// Available weeks: 1 through current week (cannot select future weeks per UC-27)
const availableWeeks = computed(() => Array.from({ length: CURRENT_WEEK }, (_, i) => i + 1))

const totalPlanned = computed(() => entries.value.reduce((s, e) => s + Number(e.plannedHours ?? 0), 0))
const totalActual  = computed(() => entries.value.reduce((s, e) => s + Number(e.actualHours  ?? 0), 0))

async function fetchEntries() {
  loading.value = true
  error.value   = ''
  try {
    const res = await api.get(`/api/v1/war?week=${selectedWeek.value}`)
    entries.value = res.data ?? []
  } catch (e) {
    error.value = e.message ?? 'Failed to load activities.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchEntries)
watch(selectedWeek, fetchEntries)

function showFlash(text, type = 'success') {
  flash.value = { text, type }
  setTimeout(() => flash.value = { text: '', type: '' }, 3500)
}

function openAdd() {
  form.value = blankForm()
  formError.value = ''
  editing.value = null
  showModal.value = true
}

function openEdit(entry) {
  form.value = {
    category:     entry.category,
    description:  entry.description,
    plannedHours: entry.plannedHours,
    actualHours:  entry.actualHours,
    status:       entry.status,
  }
  formError.value = ''
  editing.value = entry.id
  showModal.value = true
}

function validate() {
  if (!form.value.description.trim())        return 'Description is required.'
  if (!form.value.plannedHours && form.value.plannedHours !== 0)
                                             return 'Planned hours are required.'
  if (!form.value.actualHours && form.value.actualHours !== 0)
                                             return 'Actual hours are required.'
  if (Number(form.value.plannedHours) < 0)  return 'Planned hours cannot be negative.'
  if (Number(form.value.actualHours)  < 0)  return 'Actual hours cannot be negative.'
  return null
}

async function save() {
  const err = validate()
  if (err) { formError.value = err; return }

  saving.value = true
  formError.value = ''
  const payload = {
    week:         selectedWeek.value,
    category:     form.value.category,
    description:  form.value.description.trim(),
    plannedHours: Number(form.value.plannedHours),
    actualHours:  Number(form.value.actualHours),
    status:       form.value.status,
  }
  try {
    if (editing.value !== null) {
      await api.put(`/api/v1/war/${editing.value}`, payload)
      showFlash('Activity updated.')
    } else {
      await api.post('/api/v1/war', payload)
      showFlash('Activity added.')
    }
    showModal.value = false
    await fetchEntries()
  } catch (e) {
    formError.value = e.message ?? 'Failed to save. Please try again.'
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  if (deletingId.value === null) return
  try {
    await api.delete(`/api/v1/war/${deletingId.value}`)
    deletingId.value = null
    showFlash('Activity deleted.')
    await fetchEntries()
  } catch (e) {
    showFlash(e.message ?? 'Failed to delete.', 'error')
    deletingId.value = null
  }
}
</script>

<template>
  <AppLayout>
    <div v-if="flash.text" :class="['alert', flash.type === 'error' ? 'alert-error' : 'alert-success']">
      {{ flash.text }}
    </div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <!-- Controls -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <label style="margin:0">Active Week:</label>
        <select v-model.number="selectedWeek" style="width:auto;padding:6px 10px">
          <option v-for="w in availableWeeks" :key="w" :value="w">Week {{ w }}</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="openAdd">+ Add Activity</button>
    </div>

    <!-- Stats -->
    <div class="stats">
      <div class="stat">
        <div class="stat-val">{{ entries.length }}</div>
        <div class="stat-lbl">Activities</div>
      </div>
      <div class="stat">
        <div class="stat-val">{{ totalPlanned }}h</div>
        <div class="stat-lbl">Planned Hours</div>
      </div>
      <div class="stat">
        <div class="stat-val">{{ totalActual }}h</div>
        <div class="stat-lbl">Actual Hours</div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="empty"><p>Loading activities…</p></div>

    <!-- Empty -->
    <div v-else-if="entries.length === 0" class="empty">
      <div class="empty-icon">📋</div>
      <p>No activities for Week {{ selectedWeek }}. Click "Add Activity" to get started.</p>
    </div>

    <!-- Table -->
    <div v-else class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Category</th>
              <th>Description</th>
              <th>Planned (h)</th>
              <th>Actual (h)</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in entries" :key="e.id">
              <td><span class="badge badge-purple">{{ e.category }}</span></td>
              <td>{{ e.description }}</td>
              <td>{{ e.plannedHours }}</td>
              <td>{{ e.actualHours }}</td>
              <td><span :class="['badge', STATUS_CLS[e.status] ?? 'badge-gray']">{{ e.status }}</span></td>
              <td>
                <div class="flex gap-2">
                  <button class="btn btn-secondary btn-sm" @click="openEdit(e)">Edit</button>
                  <template v-if="deletingId === e.id">
                    <span class="muted" style="font-size:.8rem;align-self:center">Delete?</span>
                    <button class="btn btn-sm" style="background:var(--red);color:#fff;border:none" @click="confirmDelete">Yes</button>
                    <button class="btn btn-sm btn-secondary" @click="deletingId = null">No</button>
                  </template>
                  <button v-else class="btn btn-danger btn-sm" @click="deletingId = e.id">Delete</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Add / Edit modal -->
    <div v-if="showModal" class="overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ editing !== null ? 'Edit' : 'Add' }} Activity — Week {{ selectedWeek }}</h3>
          <button class="modal-close" @click="showModal = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="formError" class="alert alert-error">{{ formError }}</div>

          <div class="form-group">
            <label>Category</label>
            <select v-model="form.category">
              <option v-for="c in WAR_CATEGORIES" :key="c">{{ c }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea v-model="form.description" placeholder="What did you work on?" />
          </div>
          <div class="grid-2">
            <div class="form-group">
              <label>Planned Hours</label>
              <input type="number" v-model="form.plannedHours" min="0" step="0.5" placeholder="0" />
            </div>
            <div class="form-group">
              <label>Actual Hours</label>
              <input type="number" v-model="form.actualHours" min="0" step="0.5" placeholder="0" />
            </div>
          </div>
          <div class="form-group">
            <label>Status</label>
            <select v-model="form.status">
              <option v-for="s in WAR_STATUSES" :key="s">{{ s }}</option>
            </select>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
          <button class="btn btn-primary" :disabled="saving" @click="save">
            {{ saving ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
