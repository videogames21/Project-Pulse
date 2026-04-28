<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'
import { teamsApi } from '../../api/teams.js'
import { usersApi } from '../../api/users.js'

const route  = useRoute()
const router = useRouter()
const team   = ref(null)
const error  = ref('')

const editing    = ref(false)
const saving     = ref(false)
const saveError  = ref('')
const deleting   = ref(false)
const form       = ref({ name: '', description: '', websiteUrl: '', sectionName: '' })

const showEditConfirm   = ref(false)
const showDeleteConfirm = ref(false)
const deletePassword    = ref('')
const deleteError       = ref('')

// Instructor assignment
const showAssignInstructor  = ref(false)
const availableInstructors  = ref([])
const selectedInstructorId  = ref(null)
const assigningInstructor   = ref(false)
const assignInstructorError = ref('')

// Student removal
const showRemoveStudent  = ref(false)
const studentToRemove    = ref(null)
const removingStudent    = ref(false)
const removeStudentError = ref('')

// Instructor removal
const showRemoveInstructor  = ref(false)
const instructorToRemove    = ref(null)
const removingInstructor    = ref(false)
const removeInstructorError = ref('')

onMounted(async () => {
  try {
    const res = await api.get(`/api/v1/teams/${route.params.id}`)
    team.value = res.data
  } catch (e) {
    error.value = e.message
  }
})

async function openAssignInstructor() {
  assignInstructorError.value = ''
  selectedInstructorId.value  = null
  try {
    const res = await usersApi.getInstructors(null, true)
    const alreadyAssignedIds = new Set((team.value.instructors ?? []).map(i => i.id))
    availableInstructors.value = res.data.filter(i =>
      i.status === 'ACTIVE' &&
      i.supervisedSectionName === team.value.sectionName &&
      !alreadyAssignedIds.has(i.id)
    )
  } catch (e) {
    assignInstructorError.value = 'Failed to load instructors.'
  }
  showAssignInstructor.value = true
}

function openRemoveStudent(student) {
  studentToRemove.value    = student
  removeStudentError.value = ''
  showRemoveStudent.value  = true
}

async function confirmRemoveStudent() {
  removingStudent.value    = true
  removeStudentError.value = ''
  try {
    await teamsApi.removeStudent(route.params.id, studentToRemove.value.id)
    const res = await api.get(`/api/v1/teams/${route.params.id}`)
    team.value = res.data
    showRemoveStudent.value = false
  } catch (e) {
    removeStudentError.value = e.message
  } finally {
    removingStudent.value = false
  }
}

function openRemoveInstructor(instructor) {
  instructorToRemove.value    = instructor
  removeInstructorError.value = ''
  showRemoveInstructor.value  = true
}

async function confirmRemoveInstructor() {
  removingInstructor.value    = true
  removeInstructorError.value = ''
  try {
    await teamsApi.removeInstructor(route.params.id, instructorToRemove.value.id)
    const res = await api.get(`/api/v1/teams/${route.params.id}`)
    team.value = res.data
    showRemoveInstructor.value = false
  } catch (e) {
    removeInstructorError.value = e.message
  } finally {
    removingInstructor.value = false
  }
}

async function confirmAssignInstructor() {
  if (!selectedInstructorId.value) {
    assignInstructorError.value = 'Please select an instructor.'
    return
  }
  assigningInstructor.value = true
  assignInstructorError.value = ''
  try {
    await teamsApi.assignInstructor(route.params.id, selectedInstructorId.value)
    const res = await api.get(`/api/v1/teams/${route.params.id}`)
    team.value = res.data
    showAssignInstructor.value = false
  } catch (e) {
    assignInstructorError.value = e.message
  } finally {
    assigningInstructor.value = false
  }
}

function startEdit() {
  form.value = { ...team.value }
  saveError.value = ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  saveError.value = ''
}

function requestSave() {
  if (!form.value.name?.trim() || !form.value.sectionName?.trim()) {
    saveError.value = 'Name and section are required.'
    return
  }
  saveError.value = ''
  showEditConfirm.value = true
}

async function confirmSave() {
  showEditConfirm.value = false
  saving.value = true
  try {
    const res = await api.put(`/api/v1/teams/${route.params.id}`, form.value)
    team.value = res.data
    editing.value = false
  } catch (e) {
    saveError.value = e.data?.message ?? e.message
  } finally {
    saving.value = false
  }
}

function openDeleteConfirm() {
  deletePassword.value = ''
  deleteError.value = ''
  showDeleteConfirm.value = true
}

async function confirmDelete() {
  if (deletePassword.value !== 'DELETE') {
    deleteError.value = 'Type DELETE to confirm.'
    return
  }
  showDeleteConfirm.value = false
  deleting.value = true
  try {
    await api.delete(`/api/v1/teams/${route.params.id}`)
    router.push('/admin/teams')
  } catch (e) {
    error.value = e.message
  } finally {
    deleting.value = false
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
          <div style="display:flex;gap:8px;margin-left:auto">
            <button class="btn btn-primary btn-sm" @click="startEdit">Edit</button>
            <button class="btn btn-secondary btn-sm" style="color:#dc2626" :disabled="deleting" @click="openDeleteConfirm">
              {{ deleting ? 'Deleting…' : 'Delete' }}
            </button>
          </div>
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

      <!-- Students -->
      <div v-if="!editing" class="card" style="margin-top:16px;padding:0;overflow:hidden">
        <div class="card-header" style="padding:12px 16px">
          <h3 style="font-size:.95rem;font-weight:700">Students</h3>
        </div>
        <div v-if="(team.students ?? []).length === 0" style="padding:16px">
          <p class="muted">No students assigned to this team.</p>
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
              <tr v-for="s in team.students" :key="s.id">
                <td><strong>{{ s.firstName }} {{ s.lastName }}</strong></td>
                <td class="muted">{{ s.email }}</td>
                <td>
                  <button class="btn btn-secondary btn-sm" style="color:#dc2626" @click="openRemoveStudent(s)">Remove</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Instructors -->
      <div v-if="!editing" class="card" style="margin-top:16px;padding:0;overflow:hidden">
        <div class="card-header" style="padding:12px 16px;display:flex;align-items:center;justify-content:space-between">
          <h3 style="font-size:.95rem;font-weight:700">Instructors</h3>
          <button class="btn btn-primary btn-sm" @click="openAssignInstructor">+ Assign Instructor</button>
        </div>
        <div v-if="(team.instructors ?? []).length === 0" style="padding:16px">
          <p class="muted">No instructors assigned to this team.</p>
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
              <tr v-for="i in team.instructors" :key="i.id">
                <td><strong>{{ i.firstName }} {{ i.lastName }}</strong></td>
                <td class="muted">{{ i.email }}</td>
                <td>
                  <button class="btn btn-secondary btn-sm" style="color:#dc2626" @click="openRemoveInstructor(i)">Remove</button>
                </td>
              </tr>
            </tbody>
          </table>
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
          <button class="btn btn-primary" :disabled="saving" @click="requestSave">
            {{ saving ? 'Saving…' : 'Save' }}
          </button>
          <button class="btn btn-secondary" :disabled="saving" @click="cancelEdit">Cancel</button>
        </div>
      </div>
    </template>

    <!-- Edit Confirm Modal -->
    <div v-if="showEditConfirm" class="overlay" @click.self="showEditConfirm = false">
      <div class="modal">
        <div class="modal-head">
          <h3>Confirm Changes</h3>
          <button class="modal-close" @click="showEditConfirm = false">×</button>
        </div>
        <div class="modal-body">
          <p>Are you sure you want to save changes to <strong>{{ team.name }}</strong>?</p>
          <ul style="margin-top:10px;padding-left:20px;color:var(--text-muted);font-size:0.9rem">
            <li>The team name and section will be updated immediately.</li>
            <li>Any linked data (WARs, evaluations) will remain unchanged.</li>
            <li v-if="form.sectionName !== team.sectionName" style="color:#b45309;font-weight:600">
              The section is changing — all students will be unassigned from this team.
            </li>
          </ul>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showEditConfirm = false">Cancel</button>
          <button class="btn btn-primary" @click="confirmSave">Confirm Save</button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Modal -->
    <div v-if="showDeleteConfirm" class="overlay" @click.self="showDeleteConfirm = false">
      <div class="modal">
        <div class="modal-head" style="border-bottom:2px solid #dc2626">
          <h3 style="color:#dc2626">Delete Team</h3>
          <button class="modal-close" @click="showDeleteConfirm = false">×</button>
        </div>
        <div class="modal-body">
          <p>You are about to permanently delete <strong>{{ team?.name }}</strong>. Please read the following before proceeding:</p>

          <ul style="margin:12px 0;padding-left:20px;font-size:0.9rem;line-height:1.8">
            <li>All team progress and history will be <strong>permanently deleted</strong>.</li>
            <li>WAR submissions associated with this team will be deleted.</li>
            <li>Individual student accounts will <strong>not</strong> be deleted.</li>
            <li>All assigned students will be <strong>unassigned</strong> and returned to the unassigned pool.</li>
          </ul>

          <p style="margin-top:12px;font-size:0.9rem;color:var(--text-muted)">
            This action <strong>cannot be undone</strong>. Type <strong>DELETE</strong> below to confirm.
          </p>

          <div class="form-group" style="margin-top:12px;margin-bottom:0">
            <input
              v-model="deletePassword"
              placeholder="Type DELETE to confirm"
              style="border-color:#dc2626"
              @keyup.enter="confirmDelete"
            />
            <p v-if="deleteError" style="color:#dc2626;font-size:0.85rem;margin-top:4px">{{ deleteError }}</p>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showDeleteConfirm = false">Cancel</button>
          <button
            class="btn"
            style="background:#dc2626;color:#fff"
            @click="confirmDelete"
          >
            Delete Team
          </button>
        </div>
      </div>
    </div>
    <!-- Remove Student Modal -->
    <div v-if="showRemoveStudent" class="overlay" @click.self="showRemoveStudent = false">
      <div class="modal">
        <div class="modal-head">
          <h3>Remove Student</h3>
          <button class="modal-close" @click="showRemoveStudent = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="removeStudentError" class="alert alert-error" style="margin-bottom:12px">
            {{ removeStudentError }}
          </div>
          <p>Remove <strong>{{ studentToRemove?.firstName }} {{ studentToRemove?.lastName }}</strong> from <strong>{{ team?.name }}</strong>?</p>
          <p class="muted" style="margin-top:8px;font-size:.875rem">They will be returned to the unassigned pool and can be reassigned to another team.</p>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showRemoveStudent = false">Cancel</button>
          <button class="btn" style="background:#dc2626;color:#fff" :disabled="removingStudent" @click="confirmRemoveStudent">
            {{ removingStudent ? 'Removing…' : 'Remove' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Remove Instructor Modal -->
    <div v-if="showRemoveInstructor" class="overlay" @click.self="showRemoveInstructor = false">
      <div class="modal">
        <div class="modal-head">
          <h3>Remove Instructor</h3>
          <button class="modal-close" @click="showRemoveInstructor = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="removeInstructorError" class="alert alert-error" style="margin-bottom:12px">
            {{ removeInstructorError }}
          </div>
          <p>Remove <strong>{{ instructorToRemove?.firstName }} {{ instructorToRemove?.lastName }}</strong> from <strong>{{ team?.name }}</strong>?</p>
          <p class="muted" style="margin-top:8px;font-size:.875rem">They will be returned to the unassigned pool.</p>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showRemoveInstructor = false">Cancel</button>
          <button class="btn" style="background:#dc2626;color:#fff" :disabled="removingInstructor" @click="confirmRemoveInstructor">
            {{ removingInstructor ? 'Removing…' : 'Remove' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Assign Instructor Modal -->
    <div v-if="showAssignInstructor" class="overlay" @click.self="showAssignInstructor = false">
      <div class="modal">
        <div class="modal-head">
          <h3>Assign Instructor</h3>
          <button class="modal-close" @click="showAssignInstructor = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="assignInstructorError" class="alert alert-error" style="margin-bottom:12px">
            {{ assignInstructorError }}
          </div>
          <div v-if="availableInstructors.length === 0" class="muted">
            No unassigned instructors available. Please assign instructors to section first.
          </div>
          <div v-else class="form-group" style="margin-bottom:0">
            <label>Select Instructor</label>
            <select v-model="selectedInstructorId" style="width:100%;padding:8px;border:1px solid var(--border);border-radius:6px;background:var(--surface);color:var(--text)">
              <option :value="null" disabled>— Choose an instructor —</option>
              <option v-for="i in availableInstructors" :key="i.id" :value="i.id">
                {{ i.firstName }} {{ i.lastName }} ({{ i.email }})
              </option>
            </select>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showAssignInstructor = false">Cancel</button>
          <button
            class="btn btn-primary"
            :disabled="assigningInstructor || availableInstructors.length === 0"
            @click="confirmAssignInstructor"
          >
            {{ assigningInstructor ? 'Assigning…' : 'Assign' }}
          </button>
        </div>
      </div>
    </div>

  </AppLayout>
</template>
