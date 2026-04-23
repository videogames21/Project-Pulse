<script setup>
// UC-31: Section peer evaluation report (instructor)
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const MAX_WEEK = 5  // replace with active-week logic from backend
const router = useRouter()

const expandedComments = ref(new Set())
function toggleComments(studentId) {
  const next = new Set(expandedComments.value)
  if (next.has(studentId)) {
    next.delete(studentId)
  } else {
    next.add(studentId)
  }
  expandedComments.value = next
}

const selectedWeek = ref(1)
const loading = ref(false)
const error   = ref('')
const rows    = ref([])

const availableWeeks = computed(() => Array.from({ length: MAX_WEEK }, (_, i) => i + 1))

const submitted     = computed(() => rows.value.filter(r => r.submitted))
const nonSubmitters = computed(() => rows.value.filter(r => !r.submitted))
const avgScore      = computed(() => {
  const s = submitted.value.filter(r => r.maxScore)
  if (!s.length) return '—'
  return (s.reduce((acc, r) => acc + (r.totalScore / r.maxScore * 100), 0) / s.length).toFixed(1) + '%'
})

function pct(r) {
  return r.maxScore ? Math.round(r.totalScore / r.maxScore * 100) : null
}
function barColor(p) {
  return p >= 80 ? 'var(--green)' : p >= 60 ? 'var(--orange)' : 'var(--red)'
}
function studentName(r) {
  return r.firstName && r.lastName ? `${r.firstName} ${r.lastName}` : r.studentName ?? '—'
}

async function fetchReport() {
  loading.value = true
  error.value   = ''
  rows.value    = []
  try {
    const res = await api.get(`/api/v1/reports/peer-eval/section?week=${selectedWeek.value}`)
    rows.value = res.data ?? []
  } catch (e) {
    error.value = e.message ?? 'Failed to load section report.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)
watch(selectedWeek, fetchReport)
</script>

<template>
  <AppLayout>
    <!-- Controls -->
    <div class="flex items-center gap-3 mb-4">
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
        <div class="stat-lbl">Students</div>
      </div>
      <div class="stat">
        <div class="stat-val" style="color:var(--green)">{{ submitted.length }}</div>
        <div class="stat-lbl">Submitted</div>
      </div>
      <div class="stat">
        <div class="stat-val" :style="`color:${nonSubmitters.length > 0 ? 'var(--red)' : 'var(--green)'}`">{{ nonSubmitters.length }}</div>
        <div class="stat-lbl">Missing</div>
      </div>
      <div class="stat">
        <div class="stat-val">{{ avgScore }}</div>
        <div class="stat-lbl">Section Avg</div>
      </div>
    </div>

    <div v-if="nonSubmitters.length" class="alert alert-warning" style="margin-bottom:16px">
      <strong>Missing submissions:</strong>
      {{ nonSubmitters.map(r => studentName(r)).join(', ') }} did not submit for Week {{ selectedWeek }}.
    </div>

    <div v-if="loading" class="empty"><p>Loading report…</p></div>

    <div v-else-if="rows.length === 0 && !error" class="empty">
      <p>No data for Week {{ selectedWeek }}.</p>
    </div>

    <div v-else class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Student</th>
              <th>Submitted</th>
              <th>Score</th>
              <th>Percentage</th>
              <th>Evaluations Received</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <template v-for="r in rows" :key="r.studentId">
              <!-- Main student row -->
              <tr>
                <td><strong>{{ studentName(r) }}</strong></td>
                <td><span :class="['badge', r.submitted ? 'badge-green' : 'badge-orange']">{{ r.submitted ? 'Yes' : 'No' }}</span></td>
                <td>{{ r.submitted ? `${r.totalScore}/${r.maxScore}` : '—' }}</td>
                <td>
                  <template v-if="r.submitted && pct(r) !== null">
                    <div class="flex items-center gap-2">
                      <div class="progress-wrap" style="flex:1;min-width:80px">
                        <div class="progress-bar" :style="`width:${pct(r)}%;background:${barColor(pct(r))}`" />
                      </div>
                      <span style="font-size:.8rem;font-weight:600">{{ pct(r) }}%</span>
                    </div>
                  </template>
                  <template v-else>—</template>
                </td>
                <td>{{ r.evaluationsReceived ?? '—' }}/{{ (r.teamSize ?? 1) - 1 }} teammates</td>
                <td>
                  <div class="flex gap-2">
                    <button
                      v-if="r.comments && r.comments.length"
                      class="btn btn-secondary btn-sm"
                      @click="toggleComments(r.studentId)"
                    >
                      {{ expandedComments.has(r.studentId) ? 'Hide Comments' : `Comments (${r.comments.length})` }}
                    </button>
                    <button class="btn btn-secondary btn-sm" @click="router.push(`/instructor/students/${r.studentId}/peer-eval-report`)">
                      View Report
                    </button>
                  </div>
                </td>
              </tr>
              <!-- Expandable comments sub-rows -->
              <template v-if="expandedComments.has(r.studentId) && r.comments?.length">
                <tr
                  v-for="(c, i) in r.comments"
                  :key="i"
                  style="background:var(--surface-2,#f8fafc)"
                >
                  <td style="padding-left:28px;font-size:.8rem;color:var(--muted)">{{ c.evaluatorName }}</td>
                  <td colspan="2" style="font-size:.8rem">
                    <span v-if="c.publicComment" style="color:var(--text)">{{ c.publicComment }}</span>
                    <span v-else class="muted">—</span>
                  </td>
                  <td colspan="3" style="font-size:.8rem;background:rgba(77,25,121,.04)">
                    <span style="font-size:.7rem;font-weight:700;text-transform:uppercase;color:var(--muted);margin-right:4px">Private:</span>
                    <span v-if="c.privateComment">{{ c.privateComment }}</span>
                    <span v-else class="muted">—</span>
                  </td>
                </tr>
              </template>
            </template>
          </tbody>
        </table>
      </div>
    </div>
  </AppLayout>
</template>
