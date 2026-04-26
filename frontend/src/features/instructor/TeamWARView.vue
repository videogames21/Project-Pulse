<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { teamsApi } from '../../api/teams.js'
import { warsApi } from '../../api/wars.js'

const auth      = useAuthStore()
const isStudent = computed(() => auth.user?.role === 'student')

// ── Week selection ─────────────────────────────────────────────────────────────

function getRecentMondays(count = 10) {
  const mondays = []
  const today   = new Date()
  const day     = today.getDay()
  const daysToCurrentMonday = day === 0 ? 6 : day - 1
  const currentMonday = new Date(today)
  currentMonday.setDate(today.getDate() - daysToCurrentMonday)
  for (let i = 0; i <= count; i++) {
    const m = new Date(currentMonday)
    m.setDate(currentMonday.getDate() - 7 * i)
    mondays.push(m.toISOString().split('T')[0])
  }
  return mondays
}

const weekOptions    = getRecentMondays(10)
const selectedWeek   = ref(weekOptions[1])   // default: most recent previous Monday

// ── Teams ──────────────────────────────────────────────────────────────────────

const teams          = ref([])
const selectedTeamId = ref(null)
const teamsLoading   = ref(false)

async function loadTeams() {
  teamsLoading.value = true
  try {
    const params = auth.user?.section ? { sectionName: auth.user.section } : {}
    const res = await teamsApi.getAll(params)
    teams.value = res.data ?? []
    if (teams.value.length > 0 && !selectedTeamId.value) {
      selectedTeamId.value = teams.value[0].id
    }
  } catch {
    teams.value = []
  } finally {
    teamsLoading.value = false
  }
}

// ── Report ─────────────────────────────────────────────────────────────────────

const report  = ref(null)
const loading = ref(false)
const error   = ref('')

async function fetchReport() {
  if (!selectedTeamId.value || !selectedWeek.value) return
  loading.value = true
  error.value   = ''
  report.value  = null
  try {
    const res = await warsApi.getTeamReport(selectedTeamId.value, selectedWeek.value)
    report.value = res.data
  } catch (e) {
    error.value = e.message ?? 'Failed to load report.'
  } finally {
    loading.value = false
  }
}

watch([selectedTeamId, selectedWeek], fetchReport)

onMounted(async () => {
  if (isStudent.value) {
    // Students only see their own team — skip the full teams list
    selectedTeamId.value = auth.user?.teamId ?? null
    if (auth.user?.teamId) {
      teams.value = [{ id: auth.user.teamId, name: auth.user.team ?? 'My Team' }]
    }
  } else {
    await loadTeams()
  }
  await fetchReport()
})

// ── Derived data ───────────────────────────────────────────────────────────────

const studentSummaries = computed(() => report.value?.students ?? [])
const submitted        = computed(() => studentSummaries.value.filter(s => s.didSubmit))
const nonSubmitters    = computed(() => studentSummaries.value.filter(s => !s.didSubmit))

const totalPlanned = computed(() =>
  studentSummaries.value
    .flatMap(s => s.activities)
    .reduce((acc, a) => acc + Number(a.plannedHours ?? 0), 0)
    .toFixed(1)
)
const totalActual = computed(() =>
  studentSummaries.value
    .flatMap(s => s.activities)
    .reduce((acc, a) => acc + Number(a.actualHours ?? 0), 0)
    .toFixed(1)
)

// Flatten to per-activity rows; show student name only on their first row
const activityRows = computed(() => {
  const result = []
  for (const s of studentSummaries.value) {
    if (s.activities.length === 0) {
      result.push({
        key: `${s.studentId}-empty`,
        studentName: s.studentName,
        isFirst: true,
        noActivities: true,
      })
    } else {
      s.activities.forEach((a, i) => {
        result.push({
          key: `${s.studentId}-${a.id}`,
          studentName: s.studentName,
          isFirst: i === 0,
          noActivities: false,
          category: a.category,
          description: a.description,
          plannedHours: a.plannedHours,
          actualHours: a.actualHours,
          status: a.status,
        })
      })
    }
  }
  return result
})
</script>

<template>
  <AppLayout>
    <!-- Controls -->
    <div class="flex items-center gap-3 mb-4" style="flex-wrap:wrap">
      <!-- Team picker — instructors only; students are locked to their own team -->
      <div v-if="!isStudent" class="flex items-center gap-2">
        <label style="margin:0;font-weight:600">Team:</label>
        <select v-model="selectedTeamId" style="width:auto;padding:6px 10px" :disabled="teamsLoading">
          <option v-if="teamsLoading" :value="null">Loading…</option>
          <option v-else-if="!teams.length" :value="null">No teams found</option>
          <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
      </div>
      <div class="flex items-center gap-2">
        <label style="margin:0;font-weight:600">Week:</label>
        <select v-model="selectedWeek" style="width:auto;padding:6px 10px">
          <option v-for="w in weekOptions" :key="w" :value="w">{{ w }}</option>
        </select>
      </div>
      <button class="btn btn-secondary btn-sm" @click="fetchReport">Refresh</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-muted)">Loading…</div>

    <!-- Error -->
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <!-- No team available -->
    <div v-else-if="!teams.length && !teamsLoading" class="empty">
      <div class="empty-icon">👥</div>
      <h3>No Teams Found</h3>
      <p class="muted mt-4">No teams are assigned to your section yet.</p>
    </div>

    <!-- Waiting for team selection / report -->
    <div v-else-if="!report" class="empty">
      <div class="empty-icon">📋</div>
      <h3>No Data</h3>
      <p class="muted mt-4">Select a team and week to view WAR submissions.</p>
    </div>

    <template v-else>
      <!-- Summary stats -->
      <div class="stats">
        <div class="stat">
          <div class="stat-val">{{ studentSummaries.length }}</div>
          <div class="stat-lbl">Members</div>
        </div>
        <div class="stat">
          <div class="stat-val" style="color:var(--green)">{{ submitted.length }}</div>
          <div class="stat-lbl">WARs Submitted</div>
        </div>
        <div class="stat">
          <div class="stat-val">{{ totalPlanned }}h</div>
          <div class="stat-lbl">Total Planned</div>
        </div>
        <div class="stat">
          <div class="stat-val">{{ totalActual }}h</div>
          <div class="stat-lbl">Total Actual</div>
        </div>
      </div>

      <!-- Missing WARs alert -->
      <div v-if="nonSubmitters.length" class="alert alert-warning mb-4">
        <strong>Missing WARs:</strong>
        {{ nonSubmitters.map(r => r.studentName).join(', ') }}
        did not submit for {{ selectedWeek }}.
      </div>

      <!-- No members in team -->
      <div v-if="studentSummaries.length === 0" class="alert alert-info">
        No students are assigned to this team.
      </div>

      <!-- Per-activity table (UC-32) -->
      <div v-else class="card" style="padding:0;overflow:hidden">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Student</th>
                <th>Category</th>
                <th>Description</th>
                <th>Planned</th>
                <th>Actual</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in activityRows" :key="row.key">
                <td>
                  <strong v-if="row.isFirst">{{ row.studentName }}</strong>
                </td>
                <template v-if="row.noActivities">
                  <td colspan="5" class="muted" style="font-style:italic">No activities submitted.</td>
                </template>
                <template v-else>
                  <td>{{ row.category }}</td>
                  <td>{{ row.description }}</td>
                  <td>{{ row.plannedHours != null ? Number(row.plannedHours).toFixed(1) + 'h' : '—' }}</td>
                  <td>{{ row.actualHours  != null ? Number(row.actualHours).toFixed(1)  + 'h' : '—' }}</td>
                  <td>
                    <span
                      :class="['badge', row.status === 'DONE' ? 'badge-green' : row.status === 'IN_PROGRESS' ? 'badge-purple' : 'badge-orange']"
                    >{{ row.status }}</span>
                  </td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </AppLayout>
</template>
