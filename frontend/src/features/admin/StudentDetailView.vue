<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import studentsApi from '../../api/students'
import DeleteStudentModal from '../../components/DeleteStudentModal.vue'
import ConfirmDeletionModal from '../../components/ConfirmDeletionModal.vue'

const route   = useRoute()
const router  = useRouter()
const auth    = useAuthStore()
const isAdmin = computed(() => auth.user?.role === 'admin')

const student = ref(null)
const error   = ref('')

const showDeleteWarning = ref(false)
const showDeleteConfirm = ref(false)

const listRoute = computed(() => isAdmin.value ? '/admin/students' : '/students')

onMounted(async () => {
  try {
    const res = await studentsApi.getById(route.params.id)
    student.value = res.data
  } catch (e) {
    error.value = e.data?.message || e.message || 'Failed to load student.'
  }
})

function onDeleteWarningConfirm() {
  showDeleteWarning.value = false
  showDeleteConfirm.value = true
}

function onDeleted() {
  router.push(listRoute.value)
}
</script>

<template>
  <AppLayout>
    <!-- Header bar -->
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <button class="btn btn-secondary btn-sm" @click="router.push(listRoute)">← Back</button>
      <button
        v-if="isAdmin && student"
        class="btn btn-danger btn-sm"
        @click="showDeleteWarning = true"
      >
        Delete Student
      </button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-else-if="!student" class="empty">Loading...</div>

    <template v-else>
      <!-- Info card -->
      <div class="card">
        <div class="card-header">
          <div>
            <h2 style="font-size:1.15rem;font-weight:700;margin:0">
              {{ student.firstName }} {{ student.lastName }}
            </h2>
            <p class="muted" style="margin:2px 0 0;font-size:.875rem">{{ student.email }}</p>
          </div>
        </div>

        <div class="grid-2" style="gap:18px;margin-top:12px">
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">First Name</p>
            <p>{{ student.firstName }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Last Name</p>
            <p>{{ student.lastName }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Section</p>
            <p>{{ student.sectionName ?? '—' }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Team</p>
            <p>{{ student.teamName ?? '—' }}</p>
          </div>
        </div>
      </div>

      <!-- WARs card -->
      <div class="card" style="margin-top:16px;padding:0;overflow:hidden">
        <div class="card-header" style="padding:12px 16px">
          <h3 style="font-size:.95rem;font-weight:700">Weekly Activity Reports</h3>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Week</th>
                <th>Activities</th>
                <th>Team</th>
                <th>Section</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="student.wars.length === 0">
                <td colspan="4" class="empty">No WARs submitted.</td>
              </tr>
              <tr v-for="w in student.wars" :key="w.warId">
                <td>{{ w.weekStart }}</td>
                <td>{{ w.activityCount }}</td>
                <td>{{ w.teamName }}</td>
                <td>{{ w.sectionName }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Peer Evaluations card -->
      <div class="card" style="margin-top:16px;padding:0;overflow:hidden">
        <div class="card-header" style="padding:12px 16px">
          <h3 style="font-size:.95rem;font-weight:700">Peer Evaluations Received</h3>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Week</th>
                <th>From</th>
                <th>Score</th>
                <th>Team</th>
                <th>Section</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="student.peerEvaluationsReceived.length === 0">
                <td colspan="5" class="empty">No peer evaluations received.</td>
              </tr>
              <tr v-for="p in student.peerEvaluationsReceived" :key="p.evalId">
                <td>{{ p.weekStart }}</td>
                <td>{{ p.evaluatorName }}</td>
                <td>{{ p.totalScore }}</td>
                <td>{{ p.teamName }}</td>
                <td>{{ p.sectionName }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <DeleteStudentModal
      v-if="student"
      :student="student"
      :model-value="showDeleteWarning"
      @update:model-value="showDeleteWarning = $event"
      @confirm="onDeleteWarningConfirm"
    />

    <ConfirmDeletionModal
      v-if="student"
      :student="student"
      :model-value="showDeleteConfirm"
      @update:model-value="showDeleteConfirm = $event"
      @deleted="onDeleted"
    />
  </AppLayout>
</template>
