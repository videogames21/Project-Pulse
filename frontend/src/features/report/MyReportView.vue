<script setup>
import AppLayout from '../../components/AppLayout.vue'
import { MOCK_MY_REPORT } from '../../data/mockData'

const { week, overallGrade, totalMax, criteria, publicComments } = MOCK_MY_REPORT
const pct = Math.round((overallGrade / totalMax) * 100)

function barColor(p) {
  return p >= 80 ? 'var(--green)' : p >= 60 ? 'var(--orange)' : 'var(--red)'
}
</script>

<template>
  <AppLayout>
    <div class="alert alert-info mb-4">
      Showing results for <strong>Week {{ week }}</strong>.
      Evaluator identities and private comments are hidden (BR-5).
    </div>

    <div class="stats">
      <div class="stat"><div class="stat-val">{{ overallGrade }}/{{ totalMax }}</div><div class="stat-lbl">Overall Score</div></div>
      <div class="stat"><div class="stat-val" :style="`color:${barColor(pct)}`">{{ pct }}%</div><div class="stat-lbl">Percentage</div></div>
      <div class="stat"><div class="stat-val">{{ publicComments.length }}</div><div class="stat-lbl">Public Comments</div></div>
    </div>

    <div class="card mb-4">
      <div class="card-header"><h3>Score Breakdown</h3></div>
      <div v-for="c in criteria" :key="c.name" style="margin-bottom:14px">
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

    <div v-if="publicComments.length" class="card">
      <div class="card-header"><h3>Public Comments from Teammates</h3></div>
      <div v-for="(c, i) in publicComments" :key="i"
        :style="`padding:10px 12px;background:var(--surface-2);border-radius:var(--radius);border-left:3px solid var(--gold);${i < publicComments.length-1 ? 'margin-bottom:8px' : ''}`">
        "{{ c }}"
      </div>
    </div>
  </AppLayout>
</template>
