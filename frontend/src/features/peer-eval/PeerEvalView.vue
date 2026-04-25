<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { teamsApi } from '../../api/teams.js'
import { RUBRIC_CRITERIA } from '../../data/mockData'

const auth      = useAuthStore()
const members   = ref([])
const loading   = ref(true)
const noTeam    = ref(false)
const activeIdx = ref(0)
const submitted = ref(false)
const scores    = ref({})
const comments  = ref({})

const activeMember = computed(() => members.value[activeIdx.value])
const memberTotal  = (id) => RUBRIC_CRITERIA.reduce((s, c) => s + (scores.value[id]?.[c.id] ?? 0), 0)
const maxTotal     = RUBRIC_CRITERIA.reduce((s, c) => s + c.maxScore, 0)

onMounted(async () => {
  await auth.refreshTeam()

  const teamId = auth.user?.teamId
  if (!teamId) {
    noTeam.value  = true
    loading.value = false
    return
  }

  try {
    const res          = await teamsApi.getById(teamId)
    const teamStudents = res.data?.students ?? []

    // Put self first
    const self = teamStudents.find(s => s.id === auth.user.id)
    const rest = teamStudents.filter(s => s.id !== auth.user.id)

    members.value = [
      self
        ? { id: self.id, name: `${self.firstName} ${self.lastName} (You)` }
        : { id: auth.user.id, name: `${auth.user.firstName} ${auth.user.lastName} (You)` },
      ...rest.map(s => ({ id: s.id, name: `${s.firstName} ${s.lastName}` })),
    ]

    scores.value   = Object.fromEntries(
      members.value.map(m => [m.id, Object.fromEntries(RUBRIC_CRITERIA.map(c => [c.id, 0]))])
    )
    comments.value = Object.fromEntries(
      members.value.map(m => [m.id, { pub: '', priv: '' }])
    )
  } catch {
    noTeam.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AppLayout>
    <!-- Loading -->
    <div v-if="loading" style="text-align:center;padding:60px;color:var(--text-muted)">Loading…</div>

    <!-- Not assigned -->
    <div v-else-if="noTeam" class="empty">
      <div class="empty-icon">👥</div>
      <h3>No Team Assigned</h3>
      <p class="muted mt-4">You haven't been assigned to a senior design team yet. Contact your admin.</p>
    </div>

    <!-- Submitted -->
    <div v-else-if="submitted" class="empty">
      <div class="empty-icon">✅</div>
      <h3>Peer evaluation submitted!</h3>
      <p class="muted mt-4">Responses are locked — evaluations cannot be edited after submission (BR-3).</p>
    </div>

    <!-- Eval form -->
    <template v-else>
      <div class="alert alert-info mb-4">
        Evaluate each teammate for <strong>Week 2</strong>. Submissions are final once submitted.
      </div>

      <!-- Member tabs -->
      <div style="display:flex;gap:4px;border-bottom:2px solid var(--border);margin-bottom:20px">
        <button
          v-for="(m, i) in members"
          :key="m.id"
          :style="`padding:9px 16px;border:none;background:none;font-size:.875rem;font-weight:500;cursor:pointer;
                   border-bottom:2px solid transparent;margin-bottom:-2px;
                   color:${activeIdx === i ? 'var(--purple)' : 'var(--muted)'};
                   border-bottom-color:${activeIdx === i ? 'var(--purple)' : 'transparent'}`"
          @click="activeIdx = i"
        >
          {{ m.name.replace(' (You)', '') }}
          <span style="margin-left:5px;font-size:.72rem;color:var(--gold);font-weight:700">
            {{ memberTotal(m.id) }}/{{ maxTotal }}
          </span>
        </button>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>Evaluating: {{ activeMember.name }}</h3>
          <span class="badge badge-purple">{{ memberTotal(activeMember.id) }} / {{ maxTotal }} pts</span>
        </div>

        <div v-for="c in RUBRIC_CRITERIA" :key="c.id" style="margin-bottom:18px">
          <div class="flex items-center justify-between" style="margin-bottom:3px">
            <h4>{{ c.name }}</h4>
            <span class="muted" style="font-size:.8rem">max {{ c.maxScore }} pts</span>
          </div>
          <p class="muted" style="font-size:.8rem;margin-bottom:7px">{{ c.description }}</p>
          <div class="score-row">
            <input
              type="range"
              :min="0"
              :max="c.maxScore"
              step="1"
              v-model.number="scores[activeMember.id][c.id]"
            />
            <span class="score-val">{{ scores[activeMember.id][c.id] }}/{{ c.maxScore }}</span>
          </div>
        </div>

        <div class="form-group">
          <label>Public Comment (visible to evaluatee)</label>
          <textarea v-model="comments[activeMember.id].pub" placeholder="Strengths, contributions…" />
        </div>
        <div class="form-group">
          <label>Private Comment (instructor only)</label>
          <textarea v-model="comments[activeMember.id].priv" placeholder="Confidential feedback…" />
        </div>
      </div>

      <div class="flex justify-between items-center mt-4">
        <div class="flex gap-2">
          <button class="btn btn-secondary" :disabled="activeIdx === 0" @click="activeIdx--">← Previous</button>
          <button class="btn btn-secondary" :disabled="activeIdx === members.length - 1" @click="activeIdx++">Next →</button>
        </div>
        <button
          v-if="activeIdx === members.length - 1"
          class="btn btn-primary"
          @click="submitted = true"
        >
          Submit Peer Evaluation
        </button>
      </div>
    </template>
  </AppLayout>
</template>
