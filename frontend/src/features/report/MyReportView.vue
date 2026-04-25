<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { peerEvaluationsApi } from '../../api/peerEvaluations.js'

const auth    = useAuthStore()
const report  = ref(null)
const loading = ref(false)
const error   = ref('')

// Generate the last N past Mondays as selectable options
function getRecentMondays(count = 10) {
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

const weekOptions = getRecentMondays(10)
const selectedWeek = ref(weekOptions[0])   // default: most recent previous Monday

const SCORE_MAX_PER_CRITERION = 10

const totalMax = computed(() =>
  report.value ? report.value.averageCriterionScores.length * SCORE_MAX_PER_CRITERION : 0
)

const pct = computed(() =>
  totalMax.value ? Math.round((Number(report.value.averageGrade) / totalMax.value) * 100) : 0
)

function barColor(p) {
  return p >= 80 ? 'var(--green)' : p >= 60 ? 'var(--orange)' : 'var(--red)'
}

function criterionPct(score) {
  return Math.round((Number(score) / SCORE_MAX_PER_CRITERION) * 100)
}

async function fetchReport() {
  if (!auth.user?.id || !selectedWeek.value) return
  loading.value = true
  error.value   = ''
  report.value  = null
  try {
    const res = await peerEvaluationsApi.getStudentReport(auth.user.id, selectedWeek.value)
    report.value = res.data
  } catch (e) {
    error.value = e.message ?? 'Failed to load report.'
  } finally {
    loading.value = false
  }
}

watch(selectedWeek, fetchReport)
onMounted(fetchReport)
</script>

<template>
  <AppLayout>
    <!-- Week selector -->
    <div class="flex items-center gap-3 mb-4">
      <label style="margin:0;font-weight:600">Week:</label>
      <select v-model="selectedWeek" style="width:auto;padding:6px 10px">
        <option v-for="w in weekOptions" :key="w" :value="w">{{ w }}</option>
      </select>
    </div>

    <div class="alert alert-info mb-4">
      Showing peer evaluation results for <strong>{{ selectedWeek }}</strong>.
      Evaluator identities and private comments are hidden (BR-5).
    </div>

    <!-- Loading -->
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-muted)">Loading…</div>

    <!-- Error -->
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <!-- No evaluations yet -->
    <div v-else-if="!report || report.evaluationCount === 0" class="empty">
      <div class="empty-icon">📊</div>
      <h3>No Evaluations Yet</h3>
      <p class="muted mt-4">No peer evaluations have been submitted for you for this week.</p>
    </div>

    <!-- Report -->
    <template v-else>
      <div class="stats">
        <div class="stat">
          <div class="stat-val">{{ Number(report.averageGrade).toFixed(1) }}/{{ totalMax }}</div>
          <div class="stat-lbl">Average Score</div>
        </div>
        <div class="stat">
          <div class="stat-val" :style="`color:${barColor(pct)}`">{{ pct }}%</div>
          <div class="stat-lbl">Percentage</div>
        </div>
        <div class="stat">
          <div class="stat-val">{{ report.evaluationCount }}</div>
          <div class="stat-lbl">Evaluations Received</div>
        </div>
        <div class="stat">
          <div class="stat-val">{{ report.publicComments.length }}</div>
          <div class="stat-lbl">Public Comments</div>
        </div>
      </div>

      <div class="card mb-4">
        <div class="card-header"><h3>Score Breakdown by Criterion</h3></div>
        <div
          v-for="c in report.averageCriterionScores"
          :key="c.criterionId"
          style="margin-bottom:14px"
        >
          <div class="flex justify-between items-center" style="margin-bottom:4px">
            <span style="font-weight:600">Criterion {{ c.criterionId }}</span>
            <span style="font-weight:700;color:var(--purple)">
              {{ Number(c.averageScore).toFixed(2) }}/{{ SCORE_MAX_PER_CRITERION }}
            </span>
          </div>
          <div class="progress-wrap">
            <div
              class="progress-bar"
              :style="`width:${criterionPct(c.averageScore)}%;background:${barColor(criterionPct(c.averageScore))}`"
            />
          </div>
          <span class="muted" style="font-size:.75rem">{{ criterionPct(c.averageScore) }}%</span>
        </div>
      </div>

      <div v-if="report.publicComments.length" class="card">
        <div class="card-header"><h3>Public Comments from Teammates</h3></div>
        <div
          v-for="(c, i) in report.publicComments"
          :key="i"
          :style="`padding:10px 12px;background:var(--surface-2);border-radius:var(--radius);border-left:3px solid var(--gold);${i < report.publicComments.length-1 ? 'margin-bottom:8px' : ''}`"
        >
          "{{ c }}"
        </div>
      </div>
    </template>
  </AppLayout>
</template>

<style scoped>
.empty       { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; text-align: center; gap: 10px; }
.empty-icon  { font-size: 2.8rem; }
.empty-title { font-size: 1.05rem; font-weight: 700; color: #4D1979; max-width: 400px; line-height: 1.4; }
.empty-sub   { font-size: .88rem; color: #7a7090; max-width: 380px; line-height: 1.5; }
</style>
