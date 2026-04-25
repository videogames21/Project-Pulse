<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { usePeerEvalStore } from '../../stores/peerEval'
import { teamsApi } from '../../api/teams.js'
import { peerEvaluationsApi } from '../../api/peerEvaluations.js'
import { RUBRIC_CRITERIA } from '../../data/mockData'

const SCORE_MIN = 1
const SCORE_MAX = 10

const auth      = useAuthStore()
const evalStore = usePeerEvalStore()

const members      = ref([])
const loading      = ref(true)
const noTeam       = ref(false)
const activeIdx    = ref(0)
const submitting   = ref(false)
const submitError  = ref('')

// Previous Monday — the only week the backend accepts for new submissions
function getPreviousMonday() {
  const today = new Date()
  const day   = today.getDay()
  const daysToCurrentMonday = day === 0 ? 6 : day - 1
  const currentMonday = new Date(today)
  currentMonday.setDate(today.getDate() - daysToCurrentMonday)
  const prev = new Date(currentMonday)
  prev.setDate(currentMonday.getDate() - 7)
  return prev.toISOString().split('T')[0]
}

const weekStart    = getPreviousMonday()
const activeMember = computed(() => members.value[activeIdx.value])
const memberTotal  = (id) =>
  RUBRIC_CRITERIA.reduce((s, c) => s + (evalStore.scores[id]?.[c.id] ?? SCORE_MIN), 0)
const maxTotal     = RUBRIC_CRITERIA.length * SCORE_MAX

onMounted(async () => {
  await auth.refreshTeam()

  const teamId = auth.user?.teamId
  if (!teamId) {
    noTeam.value  = true
    loading.value = false
    return
  }

  // Draft from a previous team is stale — discard it
  if (evalStore.teamId && evalStore.teamId !== teamId) {
    evalStore.reset()
  }

  try {
    const res          = await teamsApi.getById(teamId)
    const teamStudents = res.data?.students ?? []

    const self = teamStudents.find(s => s.id === auth.user.id)
    const rest = teamStudents.filter(s => s.id !== auth.user.id)

    members.value = [
      self
        ? { id: self.id, name: `${self.firstName} ${self.lastName} (You)` }
        : { id: auth.user.id, name: `${auth.user.firstName} ${auth.user.lastName} (You)` },
      ...rest.map(s => ({ id: s.id, name: `${s.firstName} ${s.lastName}` })),
    ]

    // Set teamId in store so future visits detect stale drafts
    evalStore.teamId = teamId

    // Initialise any members not yet in the draft
    members.value.forEach(m => {
      if (!evalStore.scores[m.id]) {
        evalStore.scores[m.id] =
          Object.fromEntries(RUBRIC_CRITERIA.map(c => [c.id, SCORE_MIN]))
      }
      if (!evalStore.comments[m.id]) {
        evalStore.comments[m.id] = { pub: '', priv: '' }
      }
    })
  } catch {
    noTeam.value = true
  } finally {
    loading.value = false
  }
})

async function submitEvaluation() {
  submitting.value  = true
  submitError.value = ''
  try {
    for (const member of members.value) {
      await peerEvaluationsApi.submit({
        evaluatorId:    auth.user.id,
        evaluateeId:    member.id,
        weekStart,
        scores: RUBRIC_CRITERIA.map(c => ({
          criterionId: c.id,
          score:       evalStore.scores[member.id]?.[c.id] ?? SCORE_MIN,
        })),
        publicComments:  evalStore.comments[member.id]?.pub  ?? '',
        privateComments: evalStore.comments[member.id]?.priv ?? '',
      })
    }
    evalStore.submitted = true
  } catch (e) {
    // Conflict (409) means already submitted for this week — treat as success
    if (e.status === 409) {
      evalStore.submitted = true
    } else {
      submitError.value = e.message ?? 'Submission failed. Please try again.'
    }
  } finally {
    submitting.value = false
  }
}
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
    <div v-else-if="evalStore.submitted" class="empty">
      <div class="empty-icon">✅</div>
      <h3>Peer evaluation submitted!</h3>
      <p class="muted mt-4">
        Responses are locked — evaluations cannot be edited after submission (BR-3).
      </p>
    </div>

    <!-- Eval form -->
    <template v-else>
      <div class="alert alert-info mb-4">
        Evaluate each teammate for the week of <strong>{{ weekStart }}</strong>.
        Progress is saved automatically. Submissions are final once submitted.
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

      <div v-if="activeMember" class="card">
        <div class="card-header">
          <h3>Evaluating: {{ activeMember.name }}</h3>
          <span class="badge badge-purple">{{ memberTotal(activeMember.id) }} / {{ maxTotal }} pts</span>
        </div>

        <div v-for="c in RUBRIC_CRITERIA" :key="c.id" style="margin-bottom:18px">
          <div class="flex items-center justify-between" style="margin-bottom:3px">
            <h4>{{ c.name }}</h4>
            <span class="muted" style="font-size:.8rem">{{ SCORE_MIN }}–{{ SCORE_MAX }} pts</span>
          </div>
          <p class="muted" style="font-size:.8rem;margin-bottom:7px">{{ c.description }}</p>
          <div class="score-row">
            <input
              type="range"
              :min="SCORE_MIN"
              :max="SCORE_MAX"
              step="1"
              v-model.number="evalStore.scores[activeMember.id][c.id]"
            />
            <span class="score-val">{{ evalStore.scores[activeMember.id][c.id] }}/{{ SCORE_MAX }}</span>
          </div>
        </div>

        <div class="form-group">
          <label>Public Comment (visible to evaluatee)</label>
          <textarea v-model="evalStore.comments[activeMember.id].pub" placeholder="Strengths, contributions…" />
        </div>
        <div class="form-group">
          <label>Private Comment (instructor only)</label>
          <textarea v-model="evalStore.comments[activeMember.id].priv" placeholder="Confidential feedback…" />
        </div>
      </div>

      <div v-if="submitError" class="alert alert-error mt-4">{{ submitError }}</div>

      <div class="flex justify-between items-center mt-4">
        <div class="flex gap-2">
          <button class="btn btn-secondary" :disabled="activeIdx === 0" @click="activeIdx--">← Previous</button>
          <button class="btn btn-secondary" :disabled="activeIdx === members.length - 1" @click="activeIdx++">Next →</button>
        </div>
        <button
          v-if="activeIdx === members.length - 1"
          class="btn btn-primary"
          :disabled="submitting"
          @click="submitEvaluation"
        >
          {{ submitting ? 'Submitting…' : 'Submit Peer Evaluation' }}
        </button>
      </div>
    </template>
  </AppLayout>
</template>
