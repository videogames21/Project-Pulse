<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import studentsApi from '../../api/students'
import DeleteStudentModal from '../../components/DeleteStudentModal.vue'
import ConfirmDeletionModal from '../../components/ConfirmDeletionModal.vue'

const router  = useRouter()
const auth    = useAuthStore()
const isAdmin = computed(() => auth.user?.role === 'admin')

const students = ref([])
const loading  = ref(false)
const error    = ref('')

const filterFirstName  = ref('')
const filterLastName   = ref('')
const filterEmail      = ref('')
const filterTeamName   = ref('')
const filterTeamId     = ref('')
const filterSectionName = ref('')
const filterSectionId  = ref('')
const sortBy  = ref('')
const sortDir = ref('')

const showDeleteWarning  = ref(false)
const showDeleteConfirm  = ref(false)
const studentToDelete    = ref(null)

const detailRoute = (id) => isAdmin.value ? `/admin/students/${id}` : `/students/${id}`

const sortByOptions = computed(() => {
  const base = [
    { value: '',          label: 'Default' },
    { value: 'firstName', label: 'First Name' },
    { value: 'lastName',  label: 'Last Name' },
    { value: 'email',     label: 'Email' },
    { value: 'teamName',  label: 'Team Name' },
    { value: 'teamId',    label: 'Team ID' },
  ]
  if (isAdmin.value) {
    base.push({ value: 'sectionName', label: 'Section Name' })
    base.push({ value: 'sectionId',   label: 'Section ID' })
  }
  return base
})

const sortedStudents = computed(() => {
  const arr = [...students.value]
  const by  = sortBy.value
  const dir = sortDir.value

  if (!by) {
    // Default: section name DESC, then last name ASC, then first name ASC
    return arr.sort((a, b) => {
      const sA = a.sectionName ?? ''
      const sB = b.sectionName ?? ''
      const cmp = sB.localeCompare(sA)
      if (cmp !== 0) return cmp
      const lCmp = (a.lastName ?? '').localeCompare(b.lastName ?? '')
      if (lCmp !== 0) return lCmp
      return (a.firstName ?? '').localeCompare(b.firstName ?? '')
    })
  }

  return arr.sort((a, b) => {
    let va, vb
    if (by === 'teamId' || by === 'sectionId') {
      va = a[by] ?? Infinity
      vb = b[by] ?? Infinity
      return dir === 'desc' ? vb - va : va - vb
    }
    va = (a[by] ?? '').toString()
    vb = (b[by] ?? '').toString()
    return dir === 'desc' ? vb.localeCompare(va) : va.localeCompare(vb)
  })
})

async function fetchStudents() {
  loading.value = true
  error.value   = ''
  try {
    const params = {}
    if (filterFirstName.value)   params.firstName   = filterFirstName.value
    if (filterLastName.value)    params.lastName    = filterLastName.value
    if (filterEmail.value)       params.email       = filterEmail.value
    if (filterTeamName.value)    params.teamName    = filterTeamName.value
    if (filterTeamId.value)      params.teamId      = filterTeamId.value
    if (isAdmin.value) {
      if (filterSectionName.value) params.sectionName = filterSectionName.value
      if (filterSectionId.value)   params.sectionId   = filterSectionId.value
    }
    if (sortBy.value)  params.sortBy  = sortBy.value
    if (sortDir.value) params.sortDir = sortDir.value

    const res = await studentsApi.search(params)
    students.value = res.data
  } catch (e) {
    error.value = e.message || 'Failed to load students.'
  } finally {
    loading.value = false
  }
}

function clearFilters() {
  filterFirstName.value   = ''
  filterLastName.value    = ''
  filterEmail.value       = ''
  filterTeamName.value    = ''
  filterTeamId.value      = ''
  filterSectionName.value = ''
  filterSectionId.value   = ''
  sortBy.value  = ''
  sortDir.value = ''
  fetchStudents()
}

function openDelete(e, student) {
  e.stopPropagation()
  studentToDelete.value   = student
  showDeleteWarning.value = true
}

function onDeleteWarningConfirm() {
  showDeleteWarning.value = false
  showDeleteConfirm.value = true
}

function onDeleted() {
  showDeleteConfirm.value = false
  studentToDelete.value   = null
  fetchStudents()
}

onMounted(fetchStudents)
</script>

<template>
  <AppLayout>
    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div style="margin-bottom:16px">
      <p class="muted">
        {{ isAdmin ? 'View and manage all students in the system.' : 'View students in your section.' }}
      </p>
    </div>

    <!-- Filters -->
    <div class="card mb-4" style="margin-bottom:16px">
      <div class="flex gap-3" style="flex-wrap:wrap;gap:12px">
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:140px">
          <label>First Name</label>
          <input v-model="filterFirstName" placeholder="Filter..." @keyup.enter="fetchStudents" />
        </div>
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:140px">
          <label>Last Name</label>
          <input v-model="filterLastName" placeholder="Filter..." @keyup.enter="fetchStudents" />
        </div>
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:160px">
          <label>Email</label>
          <input v-model="filterEmail" placeholder="Filter..." @keyup.enter="fetchStudents" />
        </div>
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:140px">
          <label>Team Name</label>
          <input v-model="filterTeamName" placeholder="Filter..." @keyup.enter="fetchStudents" />
        </div>
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:100px">
          <label>Team ID</label>
          <input v-model="filterTeamId" type="number" placeholder="ID..." @keyup.enter="fetchStudents" />
        </div>
        <template v-if="isAdmin">
          <div class="form-group" style="margin-bottom:0;flex:1;min-width:140px">
            <label>Section Name</label>
            <input v-model="filterSectionName" placeholder="Filter..." @keyup.enter="fetchStudents" />
          </div>
          <div class="form-group" style="margin-bottom:0;flex:1;min-width:100px">
            <label>Section ID</label>
            <input v-model="filterSectionId" type="number" placeholder="ID..." @keyup.enter="fetchStudents" />
          </div>
        </template>
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:140px">
          <label>Sort By</label>
          <select v-model="sortBy" @change="fetchStudents">
            <option v-for="opt in sortByOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:120px">
          <label>Direction</label>
          <select v-model="sortDir" @change="fetchStudents">
            <option value="">Default</option>
            <option value="asc">Ascending</option>
            <option value="desc">Descending</option>
          </select>
        </div>
        <div style="display:flex;gap:8px;align-self:flex-end;padding-bottom:1px">
          <button class="btn btn-primary" @click="fetchStudents" :disabled="loading">Search</button>
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
              <th>First Name</th>
              <th>Last Name</th>
              <th>Email</th>
              <th>Team</th>
              <th>Section</th>
              <th v-if="isAdmin"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td :colspan="isAdmin ? 6 : 5" class="empty">Loading...</td>
            </tr>
            <tr v-else-if="sortedStudents.length === 0">
              <td :colspan="isAdmin ? 6 : 5" class="empty">No students found.</td>
            </tr>
            <tr
              v-for="s in sortedStudents"
              :key="s.id"
              style="cursor:pointer"
              @click="router.push(detailRoute(s.id))"
            >
              <td><strong>{{ s.firstName }}</strong></td>
              <td>{{ s.lastName }}</td>
              <td class="muted">{{ s.email }}</td>
              <td>
                <span v-if="s.teamName">{{ s.teamName }}</span>
                <span v-else class="muted">—</span>
              </td>
              <td>
                <span v-if="s.sectionName">{{ s.sectionName }}</span>
                <span v-else class="muted">—</span>
              </td>
              <td v-if="isAdmin" style="text-align:right" @click.stop>
                <button class="btn btn-danger btn-sm" @click="openDelete($event, s)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <DeleteStudentModal
      v-if="studentToDelete"
      :student="studentToDelete"
      :model-value="showDeleteWarning"
      @update:model-value="showDeleteWarning = $event"
      @confirm="onDeleteWarningConfirm"
    />

    <ConfirmDeletionModal
      v-if="studentToDelete"
      :student="studentToDelete"
      :model-value="showDeleteConfirm"
      @update:model-value="showDeleteConfirm = $event"
      @deleted="onDeleted"
    />
  </AppLayout>
</template>
