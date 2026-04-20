<script setup>
import { ref, computed } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { MOCK_WAR, WAR_CATEGORIES, WAR_STATUSES } from '../../data/mockData'

const entries    = ref([...MOCK_WAR])
const filterWeek = ref('all')
const showModal  = ref(false)
const editing    = ref(null)
const flash      = ref('')

const blankForm = () => ({ week: 1, category: 'DEVELOPMENT', description: '', plannedHours: '', actualHours: '', status: 'In Progress' })
const form = ref(blankForm())

const weeks    = computed(() => [...new Set(entries.value.map(e => e.week))].sort((a,b) => a-b))
const filtered = computed(() => filterWeek.value === 'all' ? entries.value : entries.value.filter(e => e.week === Number(filterWeek.value)))

const STATUS_CLS = { 'Done': 'badge-green', 'In Progress': 'badge-orange', 'Under Testing': 'badge-blue' }

function openAdd() { form.value = blankForm(); editing.value = null; showModal.value = true }
function openEdit(e) { form.value = { ...e }; editing.value = e.id; showModal.value = true }

function save() {
  if (!form.value.description.trim()) return
  if (editing.value !== null) {
    const i = entries.value.findIndex(e => e.id === editing.value)
    entries.value[i] = { ...form.value, id: editing.value }
    notify('Activity updated.')
  } else {
    const id = Math.max(0, ...entries.value.map(e => e.id)) + 1
    entries.value.push({ ...form.value, id })
    notify('Activity added.')
  }
  showModal.value = false
}

function remove(id) { entries.value = entries.value.filter(e => e.id !== id) }

function notify(msg) { flash.value = msg; setTimeout(() => flash.value = '', 3000) }
</script>

<template>
  <AppLayout>
    <div v-if="flash" class="alert alert-success">{{ flash }}</div>

    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <label style="margin:0">Week:</label>
        <select v-model="filterWeek" style="width:auto;padding:6px 10px">
          <option value="all">All weeks</option>
          <option v-for="w in weeks" :key="w" :value="w">Week {{ w }}</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="openAdd">+ Add Activity</button>
    </div>

    <div class="stats">
      <div class="stat"><div class="stat-val">{{ filtered.length }}</div><div class="stat-lbl">Activities</div></div>
      <div class="stat"><div class="stat-val">{{ filtered.reduce((s,e) => s + Number(e.plannedHours), 0) }}h</div><div class="stat-lbl">Planned</div></div>
      <div class="stat"><div class="stat-val">{{ filtered.reduce((s,e) => s + Number(e.actualHours), 0) }}h</div><div class="stat-lbl">Actual</div></div>
    </div>

    <div v-if="filtered.length === 0" class="empty">
      <div class="empty-icon">📋</div>
      <p>No activities yet — click "Add Activity" to get started.</p>
    </div>

    <div v-else class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead><tr><th>Week</th><th>Category</th><th>Description</th><th>Planned</th><th>Actual</th><th>Status</th><th></th></tr></thead>
          <tbody>
            <tr v-for="e in filtered" :key="e.id">
              <td><strong>Wk {{ e.week }}</strong></td>
              <td><span class="badge badge-purple">{{ e.category }}</span></td>
              <td>{{ e.description }}</td>
              <td>{{ e.plannedHours }}h</td>
              <td>{{ e.actualHours }}h</td>
              <td><span :class="['badge', STATUS_CLS[e.status]]">{{ e.status }}</span></td>
              <td>
                <div class="flex gap-2">
                  <button class="btn btn-secondary btn-sm" @click="openEdit(e)">Edit</button>
                  <button class="btn btn-danger btn-sm" @click="remove(e.id)">Delete</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-head"><h3>{{ editing !== null ? 'Edit' : 'Add' }} Activity</h3><button class="modal-close" @click="showModal = false">×</button></div>
        <div class="modal-body">
          <div class="grid-2">
            <div class="form-group"><label>Week</label><input type="number" v-model.number="form.week" min="1" max="20" /></div>
            <div class="form-group"><label>Category</label><select v-model="form.category"><option v-for="c in WAR_CATEGORIES" :key="c">{{ c }}</option></select></div>
          </div>
          <div class="form-group"><label>Description</label><textarea v-model="form.description" placeholder="What did you work on?" /></div>
          <div class="grid-2">
            <div class="form-group"><label>Planned Hours</label><input type="number" v-model="form.plannedHours" min="0" step="0.5" /></div>
            <div class="form-group"><label>Actual Hours</label><input type="number" v-model="form.actualHours" min="0" step="0.5" /></div>
          </div>
          <div class="form-group"><label>Status</label><select v-model="form.status"><option v-for="s in WAR_STATUSES" :key="s">{{ s }}</option></select></div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
          <button class="btn btn-primary" @click="save">Save</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
