<script setup>
import { ref, computed } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { MOCK_SECTION_REPORT, MOCK_TEAMS } from '../../data/mockData'

const selectedWeek = ref(2)
const rows = MOCK_SECTION_REPORT

const submitted    = computed(() => rows.filter(r => r.submitted))
const nonSubmitters = computed(() => rows.filter(r => !r.submitted))
const avgScore     = computed(() => submitted.value.length ? (submitted.value.reduce((s,r) => s + r.total, 0) / submitted.value.length).toFixed(1) : 0)

function pct(row) { return row.total !== null ? Math.round(row.total / row.max * 100) : null }
function barColor(p) { return p >= 80 ? 'var(--green)' : p >= 60 ? 'var(--orange)' : 'var(--red)' }
</script>

<template>
  <AppLayout>
    <div class="flex items-center gap-3 mb-4">
      <label style="margin:0">Week:</label>
      <select v-model.number="selectedWeek" style="width:auto;padding:6px 10px">
        <option v-for="w in [1,2,3,4,5]" :key="w" :value="w">Week {{ w }}</option>
      </select>
      <button class="btn btn-secondary btn-sm">Export CSV</button>
    </div>

    <div class="stats">
      <div class="stat"><div class="stat-val">{{ rows.length }}</div><div class="stat-lbl">Students</div></div>
      <div class="stat"><div class="stat-val" style="color:var(--green)">{{ submitted.length }}</div><div class="stat-lbl">Submitted</div></div>
      <div class="stat"><div class="stat-val" :style="`color:${nonSubmitters.length > 0 ? 'var(--red)' : 'var(--green)'}`">{{ nonSubmitters.length }}</div><div class="stat-lbl">Missing</div></div>
      <div class="stat"><div class="stat-val">{{ avgScore }}</div><div class="stat-lbl">Section Avg</div></div>
    </div>

    <div v-if="nonSubmitters.length" class="alert alert-warning mb-4">
      <strong>Missing submissions:</strong> {{ nonSubmitters.map(r => r.student).join(', ') }} did not submit for Week {{ selectedWeek }}.
    </div>

    <div class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead><tr><th>Student</th><th>Submitted</th><th>Score</th><th>Percentage</th><th>Evaluations Received</th></tr></thead>
          <tbody>
            <tr v-for="r in rows" :key="r.student">
              <td><strong>{{ r.student }}</strong></td>
              <td><span :class="['badge', r.submitted ? 'badge-green' : 'badge-orange']">{{ r.submitted ? 'Yes' : 'No' }}</span></td>
              <td>{{ r.total ?? '—' }}/{{ r.max }}</td>
              <td>
                <template v-if="pct(r) !== null">
                  <div class="flex items-center gap-2">
                    <div class="progress-wrap" style="flex:1;min-width:80px"><div class="progress-bar" :style="`width:${pct(r)}%;background:${barColor(pct(r))}`" /></div>
                    <span style="font-size:.8rem;font-weight:600">{{ pct(r) }}%</span>
                  </div>
                </template>
                <template v-else>—</template>
              </td>
              <td>{{ r.evaluatedBy }}/{{ rows.length - 1 }} teammates</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AppLayout>
</template>
