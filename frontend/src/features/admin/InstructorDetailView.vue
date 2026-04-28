<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { usersApi } from '../../api/users.js'

const route      = useRoute()
const router     = useRouter()
const instructor = ref(null)
const error      = ref('')
const actionError   = ref('')
const showConfirm      = ref(false)
const pendingAction    = ref(null) // 'deactivate' | 'reactivate'
const deactivateReason = ref('')
const loading          = ref(false)

onMounted(async () => {
  try {
    const res = await usersApi.getInstructorById(route.params.id)
    instructor.value = res.data
  } catch (e) {
    error.value = e.message
  }
})

function promptAction(action) {
  pendingAction.value    = action
  deactivateReason.value = ''
  actionError.value      = ''
  showConfirm.value      = true
}

async function confirmAction() {
  if (pendingAction.value === 'deactivate' && !deactivateReason.value.trim()) {
    actionError.value = 'A reason for deactivation is required.'
    return
  }
  loading.value = true
  actionError.value = ''
  try {
    let res
    if (pendingAction.value === 'deactivate') {
      res = await usersApi.deactivateInstructor(route.params.id, deactivateReason.value.trim())
    } else {
      res = await usersApi.reactivateInstructor(route.params.id)
    }
    instructor.value  = res.data
    showConfirm.value = false
  } catch (e) {
    // Validation errors return field-level details under e.data
    const fieldErrors = e.data?.data
    if (fieldErrors && typeof fieldErrors === 'object') {
      actionError.value = Object.values(fieldErrors).join(', ')
    } else {
      actionError.value = e.data?.message || e.message
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AppLayout>
    <div class="flex items-center gap-2 mb-4" style="margin-bottom:16px;display:flex;justify-content:space-between;align-items:center">
      <button class="btn btn-secondary btn-sm" @click="router.back()">← Back</button>
      <template v-if="instructor">
        <button
          v-if="instructor.status === 'ACTIVE'"
          class="btn btn-sm"
          style="background:#dc2626;color:#fff;border:none"
          :disabled="loading"
          @click="promptAction('deactivate')"
        >
          Deactivate Instructor
        </button>
        <button
          v-else
          class="btn btn-sm"
          style="background:var(--green,#16a34a);color:#fff;border:none"
          :disabled="loading"
          @click="promptAction('reactivate')"
        >
          Reactivate Instructor
        </button>
      </template>
    </div>

    <!-- Confirmation dialog -->
    <div v-if="showConfirm" class="card" style="margin-bottom:16px;border:2px solid #f59e0b;background:#fffbeb">
      <div style="padding:16px">
        <template v-if="pendingAction === 'deactivate'">
          <h3 style="font-size:.95rem;font-weight:700;margin-bottom:8px">Deactivate Instructor?</h3>
          <p style="font-size:.875rem;margin-bottom:8px">
            This will remove the instructor's access to the system and unassign them from any team.
            Their information will remain stored and the account can be reactivated later.
          </p>
          <div style="margin-bottom:12px">
            <label style="display:block;font-size:.8rem;font-weight:600;margin-bottom:4px">
              Reason for deactivation <span style="color:#dc2626">*</span>
            </label>
            <textarea
              v-model="deactivateReason"
              rows="3"
              placeholder="Enter a reason..."
              style="width:100%;padding:8px;border:1px solid #d1d5db;border-radius:6px;font-size:.875rem;resize:vertical;box-sizing:border-box"
            />
          </div>
        </template>
        <template v-else>
          <h3 style="font-size:.95rem;font-weight:700;margin-bottom:8px">Reactivate Instructor?</h3>
          <p style="font-size:.875rem;margin-bottom:8px">
            This will restore the instructor's access to the system.
            They will need to be manually reassigned to a team.
          </p>
        </template>
        <div v-if="actionError" class="alert alert-error" style="margin-bottom:8px">{{ actionError }}</div>
        <div style="display:flex;gap:8px">
          <button class="btn btn-sm btn-secondary" :disabled="loading" @click="showConfirm = false">Cancel</button>
          <button
            class="btn btn-sm"
            :style="pendingAction === 'deactivate' ? 'background:#dc2626;color:#fff;border:none' : 'background:var(--green,#16a34a);color:#fff;border:none'"
            :disabled="loading"
            @click="confirmAction"
          >
            {{ loading ? 'Processing...' : (pendingAction === 'deactivate' ? 'Confirm Deactivation' : 'Confirm Reactivation') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="!instructor" class="empty">Loading...</div>

    <template v-else>
      <!-- Header card -->
      <div class="card">
        <div class="card-header">
          <div>
            <h2 style="font-size:1.15rem;font-weight:700;margin:0">
              {{ instructor.firstName }} {{ instructor.lastName }}
            </h2>
            <p class="muted" style="margin:2px 0 0 0;font-size:.875rem">{{ instructor.email }}</p>
          </div>
          <span
            class="badge"
            style="margin-left:auto"
            :style="instructor.status === 'ACTIVE'
              ? 'background:var(--green,#16a34a);color:#fff'
              : 'background:#6b7280;color:#fff'"
          >
            {{ instructor.status === 'ACTIVE' ? 'Active' : 'Deactivated' }}
          </span>
        </div>

        <div class="grid-2" style="gap:18px;margin-top:12px">
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">First Name</p>
            <p>{{ instructor.firstName }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Last Name</p>
            <p>{{ instructor.lastName }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Email</p>
            <p>{{ instructor.email }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Status</p>
            <p>{{ instructor.status === 'ACTIVE' ? 'Active' : 'Deactivated' }}</p>
          </div>
        </div>
      </div>

      <!-- Section Assignment -->
      <div class="card" style="margin-top:16px">
        <div class="card-header" style="padding:12px 16px">
          <h3 style="font-size:.95rem;font-weight:700">Assigned Section</h3>
        </div>
        <div style="padding:16px">
          <p v-if="!instructor.supervisedSectionName" class="muted">This instructor is not assigned to any section.</p>
          <p v-else style="font-weight:600">{{ instructor.supervisedSectionName }}</p>
        </div>
      </div>

      <!-- Supervised Teams -->
      <div class="card" style="margin-top:16px;padding:0;overflow:hidden">
        <div class="card-header" style="padding:12px 16px">
          <h3 style="font-size:.95rem;font-weight:700">Supervised Teams</h3>
        </div>

        <div v-if="!instructor.supervisedTeams || instructor.supervisedTeams.length === 0" style="padding:16px">
          <p class="muted">This instructor is not currently assigned to any teams.</p>
        </div>

        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Team Name</th>
                <th>Section</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="team in instructor.supervisedTeams" :key="team.teamId">
                <td>
                  <strong
                    style="color:var(--purple);cursor:pointer"
                    @click="router.push(`/admin/teams/${team.teamId}`)"
                  >
                    {{ team.teamName }}
                  </strong>
                </td>
                <td class="muted">{{ team.sectionName }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </AppLayout>
</template>
