<script setup>
// UC-32: WAR report by team (instructor)
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const MAX_WEEK = 5  // replace with active-week logic from backend
const router = useRouter()

const teams        = ref([])
const selectedTeam = ref(null)
const selectedWeek = ref(1)
const loading      = ref(false)
const error        = ref('')
const rows         = ref([])

const availableWeeks = computed(() => Array.from({ length: MAX_WEEK }, (_, i) => i + 1))

const submitted    = computed(() => rows.value.filter(r => r.activityCount > 0))
const nonSubmitters = computed(() => rows.value.filter(r => r.activityCount === 0))
const totalPlanned = computed(() => rows.value.reduce((s, r) => s + Number(r.plannedHours ?? 0), 0))
const totalActual  = computed(() => rows.value.reduce((s, r) => s + Number(r.actualHours  ?? 0), 0))

const STATUS_CLS = { 'Done': 'badge-green', 'In Progress': 'badge-orange', 'Under Testing': 'badge-blue' }

function studentName(r) {
  return r.firstName && r.lastName ? `${r.firstName} ${r.lastName}` : r.studentName ?? '—'
}

async function fetchTeams() {
  try {
    const res = await api.get('/api/v1/teams')
    teams.value = res.data ?? []
    if (teams.value.length) selectedTeam.value = teams.value[0].id
  } catch (e) {
    error.value = e.message ?? 'Failed to load teams.'
  }
}

async function fetchReport() {
  if (!selectedTeam.value) return
  loading.value = true
  error.value   = ''
  rows.value    = []
  try {
    const res = await api.get(`/api/v1/reports/war/team/${selectedTeam.value}?week=${selectedWeek.value}`)
    rows.value = res.data ?? []
  } catch (e) {
    error.value = e.message ?? 'Failed to load WAR report.'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchTeams()
  await fetchReport()
})

watch([selectedTeam, selectedWeek], fetchReport)
</script>

<template>
  <AppLayout>
    <!-- Controls -->
    <div class="flex items-center gap-3 mb-4" style="flex-wrap:wrap">
      <label style="margin:0">Team:</label>
      <select v-model.number="selectedTeam" style="width:auto;padding:6px 10px">
        <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.name }}</option>
      </select>
      <label style="margin:0">Week:</label>
      <select v-model.number="selectedWeek" style="width:auto;padding:6px 10px">
        <option v-for="w in availableWeeks" :key="w" :value="w">Week {{ w }}</option>
      </select>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <!-- Stats -->
    <div class="stats">
      <div class="stat">
        <div class="stat-val">{{ rows.length }}</div>
        <div class="stat-lbl">Members</div>
      </div>
      <div class="stat">
        <div class="stat-val" style="color:var(--green)">{{ submitted.length }}</div>
        <div class="stat-lbl">WARs Submitted</div>
      </div>
      <div class="stat">
        <div class="stat-val">{{ totalPlanned }}h</div>
        <div class="stat-lbl">Planned</div>
      </div>
      <div class="stat">
        <div class="stat-val">{{ totalActual }}h</div>
        <div class="stat-lbl">Actual</div>
      </div>
    </div>

    <div v-if="nonSubmitters.length" class="alert alert-warning" style="margin-bottom:16px">
      <strong>Missing submissions:</strong>
      {{ nonSubmitters.map(r => studentName(r)).join(', ') }} did not submit a WAR for Week {{ selectedWeek }}.
    </div>

    <div v-if="loading" class="empty"><p>Loading report…</p></div>

    <div v-else-if="rows.length === 0 && !error" class="empty">
      <p>No WAR data for this team and week.</p>
    </div>

    <template v-else>
      <div v-for="r in rows" :key="r.studentId" class="card mb-4" style="margin-bottom:20px">
        <div class="card-header">
          <div class="flex items-center gap-2">
            <h3>{{ studentName(r) }}</h3>
            <span v-if="r.activityCount === 0" class="badge badge-orange">No WAR submitted</span>
          </div>
          <div class="flex gap-2">
            <span class="badge badge-purple">{{ r.plannedHours ?? 0 }}h planned</span>
            <span class="badge badge-green">{{ r.actualHours ?? 0 }}h actual</span>
            <button class="btn btn-secondary btn-sm" @click="router.push(`/instructor/students/${r.studentId}/war-report`)">
              Full Report
            </button>
          </div>
        </div>

        <div v-if="r.activities && r.activities.length" class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Category</th>
                <th>Description</th>
                <th>Planned (h)</th>
                <th>Actual (h)</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in r.activities" :key="a.id">
                <td><span class="badge badge-purple">{{ a.category }}</span></td>
                <td>{{ a.description }}</td>
                <td>{{ a.plannedHours }}</td>
                <td>{{ a.actualHours }}</td>
                <td><span :class="['badge', STATUS_CLS[a.status] ?? 'badge-gray']">{{ a.status }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="muted" style="font-size:.875rem;padding:8px 0">No activities reported this week.</div>
      </div>
    </template>
  </AppLayout>
</template>
