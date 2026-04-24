<script setup>
import { ref, watch } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { teamsApi } from '../../api/teams'

const auth = useAuthStore()

const loading     = ref(false)
const teamMembers = ref([])
const report      = ref(null)  // populated when peer eval backend ships

function barColor(p) {
  return p >= 80 ? 'var(--green)' : p >= 60 ? 'var(--orange)' : 'var(--red)'
}

async function loadTeam(teamId) {
  loading.value = true
  teamMembers.value = []
  report.value = null
  try {
    const res = await teamsApi.getById(teamId)
    teamMembers.value = res.data?.students ?? []
  } catch {
    teamMembers.value = []
  } finally {
    loading.value = false
  }
}

// Re-run whenever the student's teamId changes (e.g., admin reassigns them)
watch(
  () => auth.user?.teamId,
  (teamId) => { if (teamId) loadTeam(teamId) },
  { immediate: true }
)
</script>

<template>
  <AppLayout>
    <!-- Not on a team -->
    <div v-if="!auth.user?.teamId" class="empty">
      <div class="empty-icon">📊</div>
      <p class="empty-title">Not on a Team</p>
      <p class="empty-sub">You haven't been assigned to a team yet. Your peer evaluation report will appear here once you're on a team.</p>
    </div>

    <!-- Loading team data -->
    <div v-else-if="loading" class="empty">
      <p style="color:#888">Loading…</p>
    </div>

    <!-- Only member on the team -->
    <div v-else-if="teamMembers.length <= 1" class="empty">
      <div class="empty-icon">🌟</div>
      <p class="empty-title">You're the only person on the team — you're doing great!</p>
      <p class="empty-sub">Your report will appear here once teammates join and submit peer evaluations.</p>
    </div>

    <!-- On a team with others, but no peer reviews submitted yet -->
    <div v-else-if="!report" class="empty">
      <div class="empty-icon">⏳</div>
      <p class="empty-title">No critiques yet</p>
      <p class="empty-sub">No other teammates have submitted a peer review yet — please wait for your critique!</p>
    </div>

    <!-- Has peer evaluation report data -->
    <template v-else>
      <div class="alert alert-info mb-4">
        Showing results for <strong>Week {{ report.week }}</strong>.
        Evaluator identities and private comments are hidden (BR-5).
      </div>

      <div class="stats">
        <div class="stat"><div class="stat-val">{{ report.overallGrade }}/{{ report.totalMax }}</div><div class="stat-lbl">Overall Score</div></div>
        <div class="stat"><div class="stat-val" :style="`color:${barColor(Math.round(report.overallGrade/report.totalMax*100))}`">{{ Math.round(report.overallGrade/report.totalMax*100) }}%</div><div class="stat-lbl">Percentage</div></div>
        <div class="stat"><div class="stat-val">{{ report.publicComments.length }}</div><div class="stat-lbl">Public Comments</div></div>
      </div>

      <div class="card mb-4">
        <div class="card-header"><h3>Score Breakdown</h3></div>
        <div v-for="c in report.criteria" :key="c.name" style="margin-bottom:14px">
          <div class="flex justify-between items-center" style="margin-bottom:4px">
            <span style="font-weight:600">{{ c.name }}</span>
            <span style="font-weight:700;color:var(--purple)">{{ c.score }}/{{ c.max }}</span>
          </div>
          <div class="progress-wrap">
            <div class="progress-bar" :style="`width:${Math.round(c.score/c.max*100)}%;background:${barColor(Math.round(c.score/c.max*100))}`" />
          </div>
          <span class="muted" style="font-size:.75rem">{{ Math.round(c.score/c.max*100) }}%</span>
        </div>
      </div>

      <div v-if="report.publicComments.length" class="card">
        <div class="card-header"><h3>Public Comments from Teammates</h3></div>
        <div v-for="(c, i) in report.publicComments" :key="i"
          :style="`padding:10px 12px;background:var(--surface-2);border-radius:var(--radius);border-left:3px solid var(--gold);${i < report.publicComments.length-1 ? 'margin-bottom:8px' : ''}`">
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
