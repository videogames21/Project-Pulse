<script setup>
// UC-34: Per-student WAR report (instructor view)
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const MAX_WEEK = 5  // replace with active-week logic from backend
const route  = useRoute()
const router = useRouter()

const studentId  = computed(() => route.params.id)
const startWeek  = ref(1)
const endWeek    = ref(MAX_WEEK)
const loading    = ref(false)
const error      = ref('')
const report     = ref(null)

const availableWeeks = computed(() => Array.from({ length: MAX_WEEK }, (_, i) => i + 1))
const studentName = computed(() => {
  if (!report.value) return ''
  const { firstName, lastName } = report.value
  return firstName && lastName ? `${firstName} ${lastName}` : report.value.studentName ?? ''
})

const STATUS_CLS = { 'Done': 'badge-green', 'In Progress': 'badge-orange', 'Under Testing': 'badge-blue' }

async function fetchReport() {
  loading.value = true
  error.value   = ''
  report.value  = null
  try {
    const res = await api.get(
      `/api/v1/reports/war/student/${studentId.value}?startWeek=${startWeek.value}&endWeek=${endWeek.value}`
    )
    report.value = res.data
  } catch (e) {
    error.value = e.message ?? 'Failed to load student WAR report.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)
watch([startWeek, endWeek], fetchReport)
</script>

<template>
  <AppLayout>
    <!-- Back navigation -->
    <div class="flex items-center gap-2 mb-4">
      <button class="btn btn-secondary btn-sm" @click="router.push('/team-war')">← Back to Team WAR</button>
    </div>

    <!-- Week range selector -->
    <div class="flex items-center gap-3 mb-4" style="flex-wrap:wrap">
      <label style="margin:0">From Week:</label>
      <select v-model.number="startWeek" style="width:auto;padding:6px 10px">
        <option v-for="w in availableWeeks" :key="w" :value="w">Week {{ w }}</option>
      </select>
      <label style="margin:0">To Week:</label>
      <select v-model.number="endWeek" style="width:auto;padding:6px 10px">
        <option v-for="w in availableWeeks.filter(w => w >= startWeek)" :key="w" :value="w">Week {{ w }}</option>
      </select>
    </div>

    <div v-if="loading" class="empty"><p>Loading report…</p></div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <template v-else-if="report">
      <div class="page-header" style="margin-bottom:20px">
        <h2>{{ studentName }}</h2>
        <p class="muted">Weekly Activity Report — Week {{ startWeek }} to {{ endWeek }}</p>
      </div>

      <!-- Summary stats across all weeks -->
      <div class="stats">
        <div class="stat">
          <div class="stat-val">{{ report.weeks?.length ?? 0 }}</div>
          <div class="stat-lbl">Weeks Reported</div>
        </div>
        <div class="stat">
          <div class="stat-val">{{ report.weeks?.reduce((s, w) => s + (w.activities?.length ?? 0), 0) ?? 0 }}</div>
          <div class="stat-lbl">Total Activities</div>
        </div>
        <div class="stat">
          <div class="stat-val">{{ report.weeks?.reduce((s, w) => s + (w.totalPlanned ?? 0), 0) ?? 0 }}h</div>
          <div class="stat-lbl">Total Planned</div>
        </div>
        <div class="stat">
          <div class="stat-val">{{ report.weeks?.reduce((s, w) => s + (w.totalActual ?? 0), 0) ?? 0 }}h</div>
          <div class="stat-lbl">Total Actual</div>
        </div>
      </div>

      <!-- Per-week activity tables -->
      <div v-for="wk in report.weeks" :key="wk.week" class="card mb-4" style="margin-bottom:20px">
        <div class="card-header">
          <h3>Week {{ wk.week }}</h3>
          <div class="flex gap-2">
            <span class="badge badge-purple">{{ wk.totalPlanned ?? 0 }}h planned</span>
            <span class="badge badge-green">{{ wk.totalActual ?? 0 }}h actual</span>
          </div>
        </div>

        <div v-if="wk.activities?.length" class="table-wrap">
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
              <tr v-for="a in wk.activities" :key="a.id">
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

      <div v-if="!report.weeks?.length" class="empty">
        <p>No WAR data for Week {{ startWeek }} – {{ endWeek }}.</p>
      </div>
    </template>

    <div v-else class="empty">
      <p>No report found for this student.</p>
    </div>
  </AppLayout>
</template>
