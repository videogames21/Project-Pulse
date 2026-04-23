<script setup>
// UC-29: View own peer evaluation report
// BR-5: No evaluator identities or private comments shown
import { ref, computed, onMounted, watch } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const CURRENT_WEEK = 2  // replace with active-week logic from backend
const MAX_WEEK = CURRENT_WEEK

const selectedWeek = ref(Math.max(1, CURRENT_WEEK - 1))
const loading = ref(false)
const error   = ref('')
const report  = ref(null)

const availableWeeks = computed(() => Array.from({ length: MAX_WEEK }, (_, i) => i + 1))

const pct = computed(() => {
  if (!report.value || !report.value.maxScore) return 0
  return Math.round((report.value.overallScore / report.value.maxScore) * 100)
})

function barColor(p) {
  return p >= 80 ? 'var(--green)' : p >= 60 ? 'var(--orange)' : 'var(--red)'
}

function criterionPct(c) {
  return c.maxScore ? Math.round((c.avgScore / c.maxScore) * 100) : 0
}

async function fetchReport() {
  loading.value = true
  error.value   = ''
  report.value  = null
  try {
    const res = await api.get(`/api/v1/reports/peer-eval/student/me?week=${selectedWeek.value}`)
    report.value = res.data
  } catch (e) {
    error.value = e.message ?? 'Failed to load report.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)
watch(selectedWeek, fetchReport)
</script>

<template>
  <AppLayout>
    <!-- Week selector -->
    <div class="flex items-center gap-3 mb-4">
      <label style="margin:0">Week:</label>
      <select v-model.number="selectedWeek" style="width:auto;padding:6px 10px">
        <option v-for="w in availableWeeks" :key="w" :value="w">Week {{ w }}</option>
      </select>
    </div>

    <div class="alert alert-info" style="margin-bottom:20px">
      Showing peer evaluation results for <strong>Week {{ selectedWeek }}</strong>.
      Evaluator identities and private comments are hidden (BR-5).
    </div>

    <div v-if="loading" class="empty"><p>Loading report…</p></div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <template v-else-if="report">
      <!-- Summary stats -->
      <div class="stats">
        <div class="stat">
          <div class="stat-val">{{ report.overallScore }}/{{ report.maxScore }}</div>
          <div class="stat-lbl">Overall Score</div>
        </div>
        <div class="stat">
          <div class="stat-val" :style="`color:${barColor(pct)}`">{{ pct }}%</div>
          <div class="stat-lbl">Percentage</div>
        </div>
        <div class="stat">
          <div class="stat-val">{{ report.publicComments?.length ?? 0 }}</div>
          <div class="stat-lbl">Public Comments</div>
        </div>
      </div>

      <!-- Criterion breakdown -->
      <div class="card mb-4" style="margin-bottom:20px">
        <div class="card-header"><h3>Score Breakdown by Criterion</h3></div>
        <div v-for="c in report.criteria" :key="c.criterionId" style="margin-bottom:16px">
          <div class="flex justify-between items-center" style="margin-bottom:4px">
            <span style="font-weight:600">{{ c.name }}</span>
            <span style="font-weight:700;color:var(--purple)">{{ c.avgScore }}/{{ c.maxScore }}</span>
          </div>
          <p v-if="c.description" class="muted" style="font-size:.78rem;margin-bottom:5px">{{ c.description }}</p>
          <div class="progress-wrap">
            <div
              class="progress-bar"
              :style="`width:${criterionPct(c)}%;background:${barColor(criterionPct(c))}`"
            />
          </div>
          <span class="muted" style="font-size:.75rem">{{ criterionPct(c) }}%</span>
        </div>
      </div>

      <!-- Public comments -->
      <div v-if="report.publicComments?.length" class="card">
        <div class="card-header"><h3>Public Comments from Teammates</h3></div>
        <div
          v-for="(c, i) in report.publicComments"
          :key="i"
          :style="`padding:10px 14px;background:var(--surface-2);border-radius:var(--radius);border-left:3px solid var(--gold);${i < report.publicComments.length - 1 ? 'margin-bottom:8px' : ''}`"
        >
          "{{ c }}"
        </div>
      </div>

      <div v-else class="empty">
        <p>No public comments received for Week {{ selectedWeek }}.</p>
      </div>
    </template>

    <div v-else class="empty">
      <p>No peer evaluation data found for Week {{ selectedWeek }}.</p>
    </div>
  </AppLayout>
</template>
