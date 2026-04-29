<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { usePeerEvalStore } from '../../stores/peerEval'
import { teamsApi } from '../../api/teams.js'
import { sectionsApi } from '../../api/sections.js'
import { peerEvaluationsApi } from '../../api/peerEvaluations.js'

const SCORE_MIN = 1
const SCORE_MAX = 10

const auth      = useAuthStore()
const evalStore = usePeerEvalStore()

const members      = ref([])
const criteria     = ref([])   // loaded from section rubric API
const loading      = ref(true)
const loadError    = ref('')
const noTeam       = ref(false)
const activeIdx    = ref(0)
const submitting   = ref(false)
const submitError  = ref('')

function toLocalISODate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// Previous Monday — the only week the backend accepts for new submissions
function getPreviousMonday() {
  const today = new Date()
  const day   = today.getDay()
  const daysToCurrentMonday = day === 0 ? 6 : day - 1
  const currentMonday = new Date(today)
  currentMonday.setDate(today.getDate() - daysToCurrentMonday)
  const prev = new Date(currentMonday)
  prev.setDate(currentMonday.getDate() - 7)
  return toLocalISODate(prev)
}

const weekStart    = getPreviousMonday()
const activeMember = computed(() => members.value[activeIdx.value])

const memberTotal = (id) =>
  criteria.value.reduce((s, c) => s + (evalStore.scores[id]?.[c.id] ?? SCORE_MIN), 0)
const maxTotal = computed(() => criteria.value.length * SCORE_MAX)

onMounted(async () => {
  await auth.refreshTeam()

  const teamId    = auth.user?.teamId
  const section   = auth.user?.section

  if (!teamId) {
    noTeam.value  = true
    loading.value = false
    return
  }

  // Stale draft from a different team — discard it
  if (evalStore.teamId && evalStore.teamId !== teamId) {
    evalStore.reset()
  }

  try {
    // 1. Load team members
    const teamRes      = await teamsApi.getById(teamId)
    const teamStudents = teamRes.data?.students ?? []

    const self = teamStudents.find(s => s.id === auth.user.id)
    const rest = teamStudents.filter(s => s.id !== auth.user.id)

    members.value = [
      self
        ? { id: self.id, name: `${self.firstName} ${self.lastName} (You)` }
        : { id: auth.user.id, name: `${auth.user.firstName} ${auth.user.lastName} (You)` },
      ...rest.map(s => ({ id: s.id, name: `${s.firstName} ${s.lastName}` })),
    ]

    evalStore.teamId = teamId

    // 2. Load rubric criteria from the section's assigned rubric
    if (section) {
      try {
        const rubricRes = await sectionsApi.getRubricBySectionName(section)
        criteria.value  = rubricRes.data?.criteria ?? []
      } catch {
        loadError.value = 'No rubric has been assigned to this section yet.'
      }
    }

    // 3. Initialise store entries for any member not yet in the draft
    members.value.forEach(m => {
      if (!evalStore.scores[m.id]) {
        evalStore.scores[m.id] =
          Object.fromEntries(criteria.value.map(c => [c.id, SCORE_MIN]))
      }
      if (!evalStore.comments[m.id]) {
        evalStore.comments[m.id] = { pub: '', priv: '' }
      }
    })

    // 4. Sync with server — fetch any evaluations already submitted this week
    const existingRes  = await peerEvaluationsApi.getByEvaluator(auth.user.id, weekStart)
    const existingList = existingRes.data ?? []

    if (existingList.length > 0) {
      // Pre-populate store from server state (source of truth on page load)
      existingList.forEach(ev => {
        evalStore.evaluationIds[ev.evaluateeId] = ev.id
        evalStore.scores[ev.evaluateeId] =
          Object.fromEntries(ev.scores.map(s => [s.criterionId, s.score]))
        evalStore.comments[ev.evaluateeId] = {
          pub:  ev.publicComments  ?? '',
          priv: ev.privateComments ?? '',
        }
      })
      evalStore.submitted = true
    }
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
      const payload = {
        evaluatorId:    auth.user.id,
        evaluateeId:    member.id,
        weekStart,
        scores: criteria.value.map(c => ({
          criterionId: c.id,
          score:       evalStore.scores[member.id]?.[c.id] ?? SCORE_MIN,
        })),
        publicComments:  evalStore.comments[member.id]?.pub  ?? '',
        privateComments: evalStore.comments[member.id]?.priv ?? '',
      }

      const existingId = evalStore.evaluationIds[member.id]
      if (existingId) {
        // Already submitted — update existing evaluation
        await peerEvaluationsApi.update(existingId, payload)
      } else {
        // New submission
        const res = await peerEvaluationsApi.submit(payload)
        evalStore.evaluationIds[member.id] = res.data?.id
      }
    }
    evalStore.submitted = true
  } catch (e) {
    submitError.value = e.message ?? 'Submission failed. Please try again.'
  } finally {
    submitting.value = false
  }
}

function startEditing() {
  evalStore.submitted = false
  submitError.value   = ''
  activeIdx.value     = 0
}
</script>

<template>
  <AppLayout>
    <!-- Loading -->
    <div v-if="loading" style="text-align:center;padding:60px;color:var(--text-muted)">Loading…</div>

    <!-- Not assigned to a team -->
    <div v-else-if="noTeam" class="empty">
      <div class="empty-icon">👥</div>
      <h3>No Team Assigned</h3>
      <p class="muted mt-4">You haven't been assigned to a senior design team yet. Contact your admin.</p>
    </div>

    <!-- Rubric load error -->
    <div v-else-if="loadError" class="alert alert-info">{{ loadError }}</div>

    <!-- Submitted banner -->
    <div v-else-if="evalStore.submitted">
      <div class="alert alert-success mb-4" style="display:flex;justify-content:space-between;align-items:center">
        <span>Peer evaluation submitted for week of <strong>{{ weekStart }}</strong>.</span>
        <button class="btn btn-secondary btn-sm" @click="startEditing">Edit Evaluation</button>
      </div>

      <!-- Read-only summary of submitted scores -->
      <div class="card">
        <div class="card-header"><h3>Submitted Scores</h3></div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Member</th>
                <th v-for="c in criteria" :key="c.id">{{ c.name }}</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in members" :key="m.id">
                <td><strong>{{ m.name }}</strong></td>
                <td v-for="c in criteria" :key="c.id">
                  {{ evalStore.scores[m.id]?.[c.id] ?? '—' }}
                </td>
                <td><strong>{{ memberTotal(m.id) }}/{{ maxTotal }}</strong></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Eval form -->
    <template v-else>
      <div class="alert alert-info mb-4">
        Evaluate each teammate for the week of <strong>{{ weekStart }}</strong>.
        Progress is saved automatically.
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

        <div v-for="c in criteria" :key="c.id" style="margin-bottom:18px">
          <div class="flex items-center justify-between" style="margin-bottom:3px">
            <h4>{{ c.name }}</h4>
            <span class="muted" style="font-size:.8rem">{{ SCORE_MIN }}–{{ c.maxScore }} pts</span>
          </div>
          <p class="muted" style="font-size:.8rem;margin-bottom:7px">{{ c.description }}</p>
          <div class="score-row">
            <input
              type="range"
              :min="SCORE_MIN"
              :max="c.maxScore"
              step="1"
              v-model.number="evalStore.scores[activeMember.id][c.id]"
            />
            <span class="score-val">{{ evalStore.scores[activeMember.id][c.id] }}/{{ c.maxScore }}</span>
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
          {{ submitting ? 'Submitting…' : (Object.keys(evalStore.evaluationIds).length ? 'Update Evaluation' : 'Submit Peer Evaluation') }}
        </button>
      </div>
    </template>
  </AppLayout>
</template>
