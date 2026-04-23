<script setup>
// UC-33: Per-student peer evaluation report (instructor view)
// Instructor can see evaluator names and private comments
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const MAX_WEEK = 5  // replace with active-week logic from backend
const route  = useRoute()
const router = useRouter()

const studentId   = computed(() => route.params.id)
const startWeek   = ref(1)
const endWeek     = ref(MAX_WEEK)
const loading     = ref(false)
const error       = ref('')
const report      = ref(null)

const availableWeeks = computed(() => Array.from({ length: MAX_WEEK }, (_, i) => i + 1))
const studentName = computed(() => {
  if (!report.value) return ''
  const { firstName, lastName } = report.value
  return firstName && lastName ? `${firstName} ${lastName}` : report.value.studentName ?? ''
})

function pct(score, max) {
  return max ? Math.round(score / max * 100) : 0
}
function barColor(p) {
  return p >= 80 ? 'var(--green)' : p >= 60 ? 'var(--orange)' : 'var(--red)'
}

async function fetchReport() {
  loading.value = true
  error.value   = ''
  report.value  = null
  try {
    const res = await api.get(
      `/api/v1/reports/peer-eval/student/${studentId.value}?startWeek=${startWeek.value}&endWeek=${endWeek.value}`
    )
    report.value = res.data
  } catch (e) {
    error.value = e.message ?? 'Failed to load student report.'
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
      <button class="btn btn-secondary btn-sm" @click="router.push('/section-report')">← Back to Section Report</button>
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
        <p class="muted">Peer Evaluation Report — Week {{ startWeek }} to {{ endWeek }}</p>
      </div>

      <!-- Per-week breakdown -->
      <div v-for="wk in report.weeks" :key="wk.week" class="card mb-4" style="margin-bottom:20px">
        <div class="card-header">
          <h3>Week {{ wk.week }}</h3>
          <span class="badge badge-purple">{{ wk.overallScore }}/{{ wk.maxScore }} pts</span>
        </div>

        <!-- Criterion averages -->
        <div v-if="wk.criteria?.length" style="margin-bottom:16px">
          <p style="font-size:.78rem;font-weight:700;text-transform:uppercase;color:var(--muted);margin-bottom:8px">Criterion Averages</p>
          <div v-for="c in wk.criteria" :key="c.criterionId" style="margin-bottom:10px">
            <div class="flex justify-between items-center" style="margin-bottom:3px">
              <span style="font-weight:600;font-size:.875rem">{{ c.name }}</span>
              <span style="font-weight:700;color:var(--purple);font-size:.875rem">{{ c.avgScore }}/{{ c.maxScore }}</span>
            </div>
            <div class="progress-wrap">
              <div class="progress-bar" :style="`width:${pct(c.avgScore, c.maxScore)}%;background:${barColor(pct(c.avgScore, c.maxScore))}`" />
            </div>
          </div>
        </div>

        <!-- Individual evaluations (instructor only — full details) -->
        <div v-if="wk.evaluations?.length">
          <p style="font-size:.78rem;font-weight:700;text-transform:uppercase;color:var(--muted);margin-bottom:8px">Individual Evaluations</p>
          <div v-for="ev in wk.evaluations" :key="ev.evaluatorId ?? ev.evaluatorName"
            style="border:1px solid var(--border);border-radius:var(--radius);padding:12px;margin-bottom:10px">
            <div class="flex justify-between items-center" style="margin-bottom:8px">
              <strong style="font-size:.875rem">{{ ev.evaluatorName }}</strong>
              <span class="badge badge-purple">{{ ev.totalScore }}/{{ wk.maxScore }} pts</span>
            </div>
            <div class="table-wrap" style="margin-bottom:8px">
              <table>
                <thead>
                  <tr><th>Criterion</th><th>Score</th><th>Max</th></tr>
                </thead>
                <tbody>
                  <tr v-for="cs in ev.criterionScores" :key="cs.criterionId">
                    <td>{{ cs.name }}</td>
                    <td><strong>{{ cs.score }}</strong></td>
                    <td class="muted">{{ cs.maxScore }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="ev.publicComment" style="margin-bottom:6px">
              <span class="muted" style="font-size:.72rem;text-transform:uppercase;font-weight:700">Public: </span>
              <span style="font-size:.875rem">{{ ev.publicComment }}</span>
            </div>
            <div v-if="ev.privateComment"
              style="background:rgba(77,25,121,.06);border-radius:var(--radius);padding:6px 10px">
              <span class="muted" style="font-size:.72rem;text-transform:uppercase;font-weight:700">Private: </span>
              <span style="font-size:.875rem">{{ ev.privateComment }}</span>
            </div>
          </div>
        </div>

        <div v-else class="muted" style="font-size:.875rem">No evaluations received this week.</div>
      </div>

      <div v-if="!report.weeks?.length" class="empty">
        <p>No peer evaluation data for Week {{ startWeek }} – {{ endWeek }}.</p>
      </div>
    </template>

    <div v-else class="empty">
      <p>No report found for this student.</p>
    </div>
  </AppLayout>
</template>
