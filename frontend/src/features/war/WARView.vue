<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { warsApi } from '../../api/wars.js'
import { WAR_CATEGORIES } from '../../data/mockData'

const auth = useAuthStore()

// ── Status options ─────────────────────────────────────────────────────────────
const WAR_STATUSES = [
  { value: 'IN_PROGRESS',   label: 'In Progress'   },
  { value: 'UNDER_TESTING', label: 'Under Testing' },
  { value: 'DONE',          label: 'Done'          },
]

const STATUS_CLS = {
  IN_PROGRESS:   'badge-orange',
  UNDER_TESTING: 'badge-blue',
  DONE:          'badge-green',
}

function statusLabel(v) {
  return WAR_STATUSES.find(s => s.value === v)?.label ?? v
}

// ── Week selection ─────────────────────────────────────────────────────────────

function getRecentMondays(count = 12) {
  const mondays = []
  const today   = new Date()
  const day     = today.getDay()
  const daysToCurrentMonday = day === 0 ? 6 : day - 1
  const currentMonday = new Date(today)
  currentMonday.setDate(today.getDate() - daysToCurrentMonday)
  // Include current week (students can add activities for the current week)
  for (let i = 0; i < count; i++) {
    const m = new Date(currentMonday)
    m.setDate(currentMonday.getDate() - 7 * i)
    mondays.push(m.toISOString().split('T')[0])
  }
  return mondays
}

const weekOptions   = getRecentMondays(12)
const selectedWeek  = ref(weekOptions[0])   // default: current week

// ── Data ───────────────────────────────────────────────────────────────────────

const activities = ref([])
const loading    = ref(false)
const error      = ref('')
const flash      = ref('')

async function loadActivities() {
  if (!auth.user?.id || !selectedWeek.value) return
  loading.value = true
  error.value   = ''
  try {
    const res = await warsApi.getWAR(auth.user.id, selectedWeek.value)
    activities.value = res.data?.activities ?? []
  } catch (e) {
    error.value = e.message ?? 'Failed to load activities.'
    activities.value = []
  } finally {
    loading.value = false
  }
}

watch(selectedWeek, loadActivities)
onMounted(loadActivities)

// ── Stats ──────────────────────────────────────────────────────────────────────

const totalPlanned = computed(() => activities.value.reduce((s, a) => s + Number(a.plannedHours ?? 0), 0).toFixed(1))
const totalActual  = computed(() => activities.value.reduce((s, a) => s + Number(a.actualHours  ?? 0), 0).toFixed(1))

// ── Modal ──────────────────────────────────────────────────────────────────────

const showModal  = ref(false)
const editingId  = ref(null)
const saving     = ref(false)
const saveError  = ref('')

const blankForm = () => ({
  category:     'DEVELOPMENT',
  description:  '',
  plannedHours: '',
  actualHours:  '',
  status:       'IN_PROGRESS',
})

const form = ref(blankForm())

function openAdd() {
  form.value    = blankForm()
  editingId.value = null
  saveError.value = ''
  showModal.value = true
}

function openEdit(a) {
  form.value = {
    category:     a.category,
    description:  a.description,
    plannedHours: String(a.plannedHours),
    actualHours:  String(a.actualHours),
    status:       a.status,
  }
  editingId.value = a.id
  saveError.value = ''
  showModal.value = true
}

async function save() {
  if (!form.value.description.trim()) {
    saveError.value = 'Description is required.'
    return
  }
  saving.value    = true
  saveError.value = ''
  try {
    const payload = {
      category:     form.value.category,
      description:  form.value.description.trim(),
      plannedHours: Number(form.value.plannedHours),
      actualHours:  Number(form.value.actualHours),
      status:       form.value.status,
    }
    if (editingId.value !== null) {
      await warsApi.updateActivity(auth.user.id, selectedWeek.value, editingId.value, payload)
      notify('Activity updated.')
    } else {
      await warsApi.addActivity(auth.user.id, selectedWeek.value, payload)
      notify('Activity added.')
    }
    showModal.value = false
    await loadActivities()
  } catch (e) {
    saveError.value = e.message ?? 'Failed to save activity.'
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  try {
    await warsApi.deleteActivity(auth.user.id, selectedWeek.value, id)
    notify('Activity deleted.')
    await loadActivities()
  } catch (e) {
    error.value = e.message ?? 'Failed to delete activity.'
  }
}

function notify(msg) {
  flash.value = msg
  setTimeout(() => { flash.value = '' }, 3000)
}
</script>

<template>
  <AppLayout>
    <!-- Flash notification -->
    <div v-if="flash" class="alert alert-success mb-4">{{ flash }}</div>

    <!-- Controls -->
    <div class="flex items-center justify-between mb-4" style="flex-wrap:wrap;gap:12px">
      <div class="flex items-center gap-3">
        <label style="margin:0;font-weight:600">Week:</label>
        <select v-model="selectedWeek" style="width:auto;padding:6px 10px">
          <option v-for="w in weekOptions" :key="w" :value="w">{{ w }}</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="openAdd">+ Add Activity</button>
    </div>

    <!-- Error -->
    <div v-if="error" class="alert alert-error mb-4">{{ error }}</div>

    <!-- Loading -->
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-muted)">Loading…</div>

    <template v-else>
      <!-- Stats -->
      <div class="stats mb-4">
        <div class="stat"><div class="stat-val">{{ activities.length }}</div><div class="stat-lbl">Activities</div></div>
        <div class="stat"><div class="stat-val">{{ totalPlanned }}h</div><div class="stat-lbl">Planned</div></div>
        <div class="stat"><div class="stat-val">{{ totalActual }}h</div><div class="stat-lbl">Actual</div></div>
      </div>

      <!-- Empty state -->
      <div v-if="activities.length === 0" class="empty">
        <div class="empty-icon">📋</div>
        <h3>No Activities Yet</h3>
        <p class="muted mt-4">Click "Add Activity" to log your work for this week.</p>
      </div>

      <!-- Activity table -->
      <div v-else class="card" style="padding:0;overflow:hidden">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Category</th>
                <th>Description</th>
                <th>Planned</th>
                <th>Actual</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in activities" :key="a.id">
                <td><span class="badge badge-purple">{{ a.category }}</span></td>
                <td>{{ a.description }}</td>
                <td>{{ Number(a.plannedHours).toFixed(1) }}h</td>
                <td>{{ Number(a.actualHours).toFixed(1) }}h</td>
                <td><span :class="['badge', STATUS_CLS[a.status]]">{{ statusLabel(a.status) }}</span></td>
                <td>
                  <div class="flex gap-2">
                    <button class="btn btn-secondary btn-sm" @click="openEdit(a)">Edit</button>
                    <button class="btn btn-danger btn-sm" @click="remove(a.id)">Delete</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- Add/Edit Modal -->
    <div v-if="showModal" class="overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ editingId !== null ? 'Edit' : 'Add' }} Activity</h3>
          <button class="modal-close" @click="showModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="grid-2">
            <div class="form-group">
              <label>Category</label>
              <select v-model="form.category">
                <option v-for="c in WAR_CATEGORIES" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>Status</label>
              <select v-model="form.status">
                <option v-for="s in WAR_STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea v-model="form.description" placeholder="What did you work on?" />
          </div>
          <div class="grid-2">
            <div class="form-group">
              <label>Planned Hours</label>
              <input type="number" v-model="form.plannedHours" min="0.1" step="0.5" />
            </div>
            <div class="form-group">
              <label>Actual Hours</label>
              <input type="number" v-model="form.actualHours" min="0.1" step="0.5" />
            </div>
          </div>
          <div v-if="saveError" class="alert alert-error">{{ saveError }}</div>
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
