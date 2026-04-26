<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { peerEvaluationsApi } from '../../api/peerEvaluations.js'

const auth = useAuthStore()

// ── Section & week selection ──────────────────────────────────────────────────

// Use the instructor's section from their session, with a fallback text input
const sectionName = ref(auth.user?.section ?? '')

function getRecentMondays(count = 10) {
  const mondays = []
  const today   = new Date()
  const day     = today.getDay()
  const daysToCurrentMonday = day === 0 ? 6 : day - 1
  const currentMonday = new Date(today)
  currentMonday.setDate(today.getDate() - daysToCurrentMonday)
  // Include current week too for instructor viewing
  for (let i = 0; i <= count; i++) {
    const m = new Date(currentMonday)
    m.setDate(currentMonday.getDate() - 7 * i)
    mondays.push(m.toISOString().split('T')[0])
  }
  return mondays
}

const weekOptions   = getRecentMondays(10)
const selectedWeek  = ref(weekOptions[1])    // default: most recent previous Monday

// ── Data ──────────────────────────────────────────────────────────────────────

const report     = ref(null)
const loading    = ref(false)
const error      = ref('')

// ── Filtering ─────────────────────────────────────────────────────────────────

const searchName = ref('')

const filteredStudents = computed(() => {
  if (!report.value) return []
  const q = searchName.value.trim().toLowerCase()
  if (!q) return report.value.students
  return report.value.students.filter(s =>
    s.studentName.toLowerCase().includes(q)
  )
})

// ── Computed stats (based on full, unfiltered list) ───────────────────────────

const submitted    = computed(() => report.value?.students.filter(s => s.didSubmit)         ?? [])
const nonSubmitters = computed(() => report.value?.students.filter(s => !s.didSubmit)       ?? [])
const maxGrade  = computed(() => report.value?.maxGrade ?? 0)
const avgGrade  = computed(() => {
  const withEvals = report.value?.students.filter(s => s.evaluationCount > 0) ?? []
  if (!withEvals.length) return '—'
  const sum = withEvals.reduce((acc, s) => acc + Number(s.averageGrade), 0)
  return (sum / withEvals.length).toFixed(1)
})

// ── Expanded row for private details ─────────────────────────────────────────

const expandedStudentId = ref(null)

function toggleExpand(id) {
  expandedStudentId.value = expandedStudentId.value === id ? null : id
}

// ── Fetch ─────────────────────────────────────────────────────────────────────

async function fetchReport() {
  if (!sectionName.value.trim() || !selectedWeek.value) return
  loading.value = true
  error.value   = ''
  report.value  = null
  expandedStudentId.value = null
  try {
    const res = await peerEvaluationsApi.getSectionReport(sectionName.value.trim(), selectedWeek.value)
    report.value = res.data
  } catch (e) {
    error.value = e.message ?? 'Failed to load report.'
  } finally {
    loading.value = false
  }
}

watch([selectedWeek, sectionName], fetchReport)
onMounted(fetchReport)

</script>

<template>
  <AppLayout>
    <!-- Controls -->
    <div class="flex items-center gap-3 mb-4" style="flex-wrap:wrap">
      <div class="flex items-center gap-2">
        <label style="margin:0;font-weight:600">Section:</label>
        <input
          v-model="sectionName"
          placeholder="e.g. 2024-2025"
          style="padding:6px 10px;border:1px solid var(--border);border-radius:6px;background:var(--surface);color:var(--text);width:160px"
          @keyup.enter="fetchReport"
        />
      </div>
      <div class="flex items-center gap-2">
        <label style="margin:0;font-weight:600">Week:</label>
        <select v-model="selectedWeek" style="width:auto;padding:6px 10px">
          <option v-for="w in weekOptions" :key="w" :value="w">{{ w }}</option>
        </select>
      </div>
      <div class="flex items-center gap-2">
        <label style="margin:0;font-weight:600">Filter by name:</label>
        <input
          v-model="searchName"
          placeholder="Last or first name…"
          style="padding:6px 10px;border:1px solid var(--border);border-radius:6px;background:var(--surface);color:var(--text);width:180px"
        />
      </div>
      <button class="btn btn-secondary btn-sm" @click="fetchReport">Refresh</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-muted)">Loading…</div>

    <!-- Error -->
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <!-- Empty section -->
    <div v-else-if="!report" class="empty">
      <div class="empty-icon">📊</div>
      <h3>No Data</h3>
      <p class="muted mt-4">Enter a section name and select a week to load the report.</p>
    </div>

    <template v-else>
      <!-- Summary stats -->
      <div class="stats">
        <div class="stat"><div class="stat-val">{{ report.students.length }}</div><div class="stat-lbl">Students</div></div>
        <div class="stat"><div class="stat-val" style="color:var(--green)">{{ submitted.length }}</div><div class="stat-lbl">Submitted</div></div>
        <div class="stat">
          <div class="stat-val" :style="`color:${nonSubmitters.length > 0 ? 'var(--red)' : 'var(--green)'}`">{{ nonSubmitters.length }}</div>
          <div class="stat-lbl">Missing</div>
        </div>
        <div class="stat"><div class="stat-val">{{ avgGrade !== '—' ? `${avgGrade}/${maxGrade}` : '—' }}</div><div class="stat-lbl">Section Avg</div></div>
      </div>

      <!-- Missing submissions alert -->
      <div v-if="nonSubmitters.length" class="alert alert-warning mb-4">
        <strong>Missing submissions:</strong>
        {{ nonSubmitters.map(s => s.studentName).join(', ') }}
        did not submit for {{ selectedWeek }}.
      </div>

      <!-- No results -->
      <div v-if="filteredStudents.length === 0" class="alert alert-info">
        <span v-if="searchName.trim()">No students match "{{ searchName }}".</span>
        <span v-else>No students are enrolled in this section for the selected week.</span>
      </div>

      <!-- Student table -->
      <div v-else class="card" style="padding:0;overflow:hidden">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Student</th>
                <th>Submitted</th>
                <th>Evaluations Received</th>
                <th>Avg Grade</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="s in filteredStudents" :key="s.studentId">
                <tr>
                  <td><strong>{{ s.studentName }}</strong></td>
                  <td>
                    <span :class="['badge', s.didSubmit ? 'badge-green' : 'badge-orange']">
                      {{ s.didSubmit ? 'Yes' : 'No' }}
                    </span>
                  </td>
                  <td>{{ s.evaluationCount }}</td>
                  <td>{{ s.evaluationCount > 0 ? `${Number(s.averageGrade).toFixed(1)}/${maxGrade}` : '—' }}</td>
                  <td>
                    <button
                      class="btn btn-secondary btn-sm"
                      @click="toggleExpand(s.studentId)"
                    >
                      {{ expandedStudentId === s.studentId ? 'Hide' : 'View' }}
                    </button>
                  </td>
                </tr>

                <!-- Expanded: received evaluations with private data -->
                <tr v-if="expandedStudentId === s.studentId">
                  <td colspan="5" style="background:var(--surface-2);padding:12px 16px">
                    <div v-if="s.receivedEvaluations.length === 0" class="muted">
                      No evaluations received yet.
                    </div>
                    <div
                      v-for="(ev, idx) in s.receivedEvaluations"
                      :key="idx"
                      style="border:1px solid var(--border);border-radius:6px;padding:10px 14px;margin-bottom:8px;background:var(--surface)"
                    >
                      <div class="flex justify-between items-center" style="margin-bottom:6px">
                        <strong style="font-size:.9rem">{{ ev.evaluatorName }}</strong>
                        <span class="badge badge-purple">Score: {{ Number(ev.totalScore).toFixed(1) }}/{{ maxGrade }}</span>
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
    </template>
  </AppLayout>
</template>
