<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import studentsApi from '../../api/students'
import { peerEvaluationsApi } from '../../api/peerEvaluations.js'
import { sectionsApi } from '../../api/sections.js'
import { warsApi } from '../../api/wars.js'
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

// ── UC-33: Peer eval range report (instructor only) ───────────────────────────

function getRecentMondays(count = 20) {
  const mondays = []
  const today   = new Date()
  const day     = today.getDay()
  const daysToCurrentMonday = day === 0 ? 6 : day - 1
  const currentMonday = new Date(today)
  currentMonday.setDate(today.getDate() - daysToCurrentMonday)
  for (let i = 1; i <= count; i++) {
    const m = new Date(currentMonday)
    m.setDate(currentMonday.getDate() - 7 * i)
    mondays.push(m.toISOString().split('T')[0])
  }
  return mondays
}

const weekOptions    = getRecentMondays(20)
const rangeStartWeek = ref(weekOptions[weekOptions.length - 1])
const rangeEndWeek   = ref(weekOptions[0])

const rangeReport  = ref(null)
const rangeLoading = ref(false)
const rangeError   = ref('')
const maxGrade     = ref(0)
const expandedWeek = ref(null)

function toggleExpandWeek(w) {
  expandedWeek.value = expandedWeek.value === w ? null : w
}

async function generateRangeReport() {
  if (!student.value?.id) return
  rangeLoading.value = true
  rangeError.value   = ''
  rangeReport.value  = null
  expandedWeek.value = null
  try {
    const [reportRes, rubricRes] = await Promise.all([
      peerEvaluationsApi.getStudentRangeReport(
        student.value.id, rangeStartWeek.value, rangeEndWeek.value),
      student.value.sectionName
        ? sectionsApi.getRubricBySectionName(student.value.sectionName).catch(() => null)
        : Promise.resolve(null),
    ])
    rangeReport.value = reportRes.data
    maxGrade.value = rubricRes?.data?.criteria
      ? rubricRes.data.criteria.reduce((s, c) => s + Number(c.maxScore), 0)
      : 0
  } catch (e) {
    rangeError.value = e.message ?? 'Failed to generate report.'
  } finally {
    rangeLoading.value = false
  }
}

// ── UC-34: WAR range report (instructor only) ─────────────────────────────────

const warStartWeek  = ref(weekOptions[weekOptions.length - 1])
const warEndWeek    = ref(weekOptions[0])

const warReport     = ref(null)
const warLoading    = ref(false)
const warError      = ref('')

async function generateWARReport() {
  if (!student.value?.id) return
  warLoading.value = true
  warError.value   = ''
  warReport.value  = null
  try {
    const res = await warsApi.getStudentRangeReport(
      student.value.id, warStartWeek.value, warEndWeek.value)
    warReport.value = res.data
  } catch (e) {
    warError.value = e.message ?? 'Failed to generate WAR report.'
  } finally {
    warLoading.value = false
  }
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

    <!-- UC-33: Peer Eval Range Report — instructor only -->
    <div v-if="!isAdmin && student" class="card" style="margin-top:16px">
      <div class="card-header">
        <h3 style="font-size:.95rem;font-weight:700">Peer Eval Range Report</h3>
      </div>

      <!-- Date range selectors -->
      <div class="flex items-center gap-3 mb-4" style="flex-wrap:wrap">
        <div class="flex items-center gap-2">
          <label style="margin:0;font-weight:600;font-size:.875rem">From:</label>
          <select v-model="rangeStartWeek" style="padding:5px 9px;font-size:.875rem">
            <option v-for="w in weekOptions" :key="w" :value="w">{{ w }}</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label style="margin:0;font-weight:600;font-size:.875rem">To:</label>
          <select v-model="rangeEndWeek" style="padding:5px 9px;font-size:.875rem">
            <option v-for="w in weekOptions" :key="w" :value="w">{{ w }}</option>
          </select>
        </div>
        <button class="btn btn-primary btn-sm" :disabled="rangeLoading" @click="generateRangeReport">
          {{ rangeLoading ? 'Generating…' : 'Generate Report' }}
        </button>
      </div>

      <div v-if="rangeError" class="alert alert-error">{{ rangeError }}</div>

      <div v-else-if="rangeReport">
        <!-- No data -->
        <div v-if="rangeReport.weeks.length === 0" class="alert alert-info">
          No peer evaluations found for {{ student.firstName }} {{ student.lastName }}
          between {{ rangeStartWeek }} and {{ rangeEndWeek }}.
        </div>

        <!-- Results table -->
        <div v-else class="table-wrap" style="margin-top:0">
          <table>
            <thead>
              <tr>
                <th>Week</th>
                <th>Avg Grade</th>
                <th>Evaluations</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="w in rangeReport.weeks" :key="w.weekStart">
                <tr>
                  <td>{{ w.weekStart }}</td>
                  <td>
                    <strong>{{ Number(w.averageGrade).toFixed(1) }}{{ maxGrade ? '/' + maxGrade : '' }}</strong>
                  </td>
                  <td>{{ w.evaluationCount }}</td>
                  <td>
                    <button class="btn btn-secondary btn-sm" @click="toggleExpandWeek(w.weekStart)">
                      {{ expandedWeek === w.weekStart ? 'Hide' : 'View' }}
                    </button>
                  </td>
                </tr>

                <!-- Expanded: per-evaluator details -->
                <tr v-if="expandedWeek === w.weekStart">
                  <td colspan="4" style="background:var(--surface-2);padding:12px 16px">
                    <div v-if="w.receivedEvaluations.length === 0" class="muted">No evaluations.</div>
                    <div
                      v-for="(ev, idx) in w.receivedEvaluations"
                      :key="idx"
                      style="border:1px solid var(--border);border-radius:6px;padding:10px 14px;margin-bottom:8px;background:var(--surface)"
                    >
                      <div class="flex justify-between items-center" style="margin-bottom:6px">
                        <strong style="font-size:.9rem">{{ ev.evaluatorName }}</strong>
                        <span class="badge badge-purple">
                          Score: {{ Number(ev.totalScore).toFixed(1) }}{{ maxGrade ? '/' + maxGrade : '' }}
                        </span>
                      </div>
                      <div v-if="ev.publicComments" style="margin-bottom:4px;font-size:.85rem">
                        <span class="muted" style="font-weight:600">Public: </span>{{ ev.publicComments }}
                      </div>
                      <div
                        v-if="ev.privateComments"
                        style="font-size:.85rem;padding:6px 10px;background:#fef3c7;border-radius:4px;border-left:3px solid var(--orange)"
                      >
                        <span style="font-weight:600;color:#92400e">Private: </span>
                        <span style="color:#78350f">{{ ev.privateComments }}</span>
                      </div>
                      <div v-else class="muted" style="font-size:.8rem;font-style:italic">No private comment.</div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- UC-34: WAR Range Report — instructor only -->
    <div v-if="!isAdmin && student" class="card" style="margin-top:16px">
      <div class="card-header">
        <h3 style="font-size:.95rem;font-weight:700">WAR Range Report</h3>
      </div>

      <!-- Date range selectors -->
      <div class="flex items-center gap-3 mb-4" style="flex-wrap:wrap">
        <div class="flex items-center gap-2">
          <label style="margin:0;font-weight:600;font-size:.875rem">From:</label>
          <select v-model="warStartWeek" style="padding:5px 9px;font-size:.875rem">
            <option v-for="w in weekOptions" :key="w" :value="w">{{ w }}</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <label style="margin:0;font-weight:600;font-size:.875rem">To:</label>
          <select v-model="warEndWeek" style="padding:5px 9px;font-size:.875rem">
            <option v-for="w in weekOptions" :key="w" :value="w">{{ w }}</option>
          </select>
        </div>
        <button class="btn btn-primary btn-sm" :disabled="warLoading" @click="generateWARReport">
          {{ warLoading ? 'Generating…' : 'Generate Report' }}
        </button>
      </div>

      <div v-if="warError" class="alert alert-error">{{ warError }}</div>

      <div v-else-if="warReport">
        <!-- No data -->
        <div v-if="warReport.weeks.length === 0" class="alert alert-info">
          No WAR activities found for {{ student.firstName }} {{ student.lastName }}
          between {{ warStartWeek }} and {{ warEndWeek }}.
        </div>

        <!-- Results: one section per week -->
        <template v-else>
          <div
            v-for="w in warReport.weeks"
            :key="w.weekStart"
            style="margin-bottom:20px"
          >
            <div style="font-weight:700;font-size:.875rem;margin-bottom:8px;color:var(--purple)">
              Active week: {{ w.weekStart }}
            </div>
            <div v-if="w.activities.length === 0" class="muted" style="font-size:.85rem;font-style:italic">
              No activities submitted this week.
            </div>
            <div v-else class="table-wrap" style="margin-top:0">
              <table>
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Description</th>
                    <th>Planned</th>
                    <th>Actual</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="a in w.activities" :key="a.id">
                    <td>{{ a.category }}</td>
                    <td>{{ a.description }}</td>
                    <td>{{ a.plannedHours != null ? Number(a.plannedHours).toFixed(1) + 'h' : '—' }}</td>
                    <td>{{ a.actualHours  != null ? Number(a.actualHours).toFixed(1)  + 'h' : '—' }}</td>
                    <td>
                      <span
                        :class="['badge', a.status === 'DONE' ? 'badge-green' : a.status === 'IN_PROGRESS' ? 'badge-purple' : 'badge-orange']"
                      >{{ a.status }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>
      </div>
    </div>

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
