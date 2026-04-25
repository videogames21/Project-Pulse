<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { usersApi } from '../../api/users.js'
import { MOCK_TEAM_WAR, MOCK_TEAMS } from '../../data/mockData'

const profile       = ref(null)
const profileLoaded = ref(false)

const selectedTeam = ref(1)
const selectedWeek = ref(2)
const rows = MOCK_TEAM_WAR

const submitted     = computed(() => rows.filter(r => r.submitted))
const nonSubmitters = computed(() => rows.filter(r => !r.submitted))

onMounted(async () => {
  try {
    const res = await usersApi.getProfile()
    profile.value = res.data
  } catch { /* ignore */ } finally {
    profileLoaded.value = true
  }
})
</script>

<template>
  <AppLayout>
    <div v-if="!profileLoaded" style="color:#888;padding:16px 0">Loading…</div>

    <div v-else-if="!profile?.supervisedSectionName" class="empty-state">
      <p style="font-size:1rem;color:#6b6480">You haven't been assigned to a section yet. Please contact your admin.</p>
    </div>

    <template v-else>
      <div class="flex items-center gap-3 mb-4">
        <label style="margin:0">Team:</label>
        <select v-model.number="selectedTeam" style="width:auto;padding:6px 10px">
          <option v-for="t in MOCK_TEAMS" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
        <label style="margin:0">Week:</label>
        <select v-model.number="selectedWeek" style="width:auto;padding:6px 10px">
          <option v-for="w in [1,2,3,4,5]" :key="w" :value="w">Week {{ w }}</option>
        </select>
        <button class="btn btn-secondary btn-sm">Export CSV</button>
      </div>

      <div class="stats">
        <div class="stat"><div class="stat-val">{{ rows.length }}</div><div class="stat-lbl">Members</div></div>
        <div class="stat"><div class="stat-val" style="color:var(--green)">{{ submitted.length }}</div><div class="stat-lbl">WARs Submitted</div></div>
        <div class="stat"><div class="stat-val">{{ submitted.reduce((s,r) => s+r.planned,0) }}h</div><div class="stat-lbl">Planned</div></div>
        <div class="stat"><div class="stat-val">{{ submitted.reduce((s,r) => s+r.actual,0) }}h</div><div class="stat-lbl">Actual</div></div>
      </div>

      <div v-if="nonSubmitters.length" class="alert alert-warning mb-4">
        <strong>Missing WARs:</strong> {{ nonSubmitters.map(r => r.student).join(', ') }} did not submit for Week {{ selectedWeek }}.
      </div>

      <div class="card" style="padding:0;overflow:hidden">
        <div class="table-wrap">
          <table>
            <thead><tr><th>Student</th><th>Submitted</th><th>Activities</th><th>Planned</th><th>Actual</th><th>Variance</th></tr></thead>
            <tbody>
              <tr v-for="r in rows" :key="r.student">
                <td><strong>{{ r.student }}</strong></td>
                <td><span :class="['badge', r.submitted ? 'badge-green' : 'badge-orange']">{{ r.submitted ? 'Yes' : 'No' }}</span></td>
                <td>{{ r.entries }}</td>
                <td>{{ r.planned }}h</td>
                <td>{{ r.actual }}h</td>
                <td :style="`font-weight:600;color:${r.actual-r.planned > 0 ? 'var(--red)' : r.actual-r.planned < 0 ? 'var(--blue)' : 'var(--green)'}`">
                  {{ r.actual - r.planned > 0 ? '+' : '' }}{{ r.actual - r.planned }}h
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </AppLayout>
</template>
