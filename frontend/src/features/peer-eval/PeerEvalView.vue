<script setup>
// UC-28: Submit a peer evaluation for the previous week
// BR-3: Cannot be edited once submitted
// BR-4: Only previous week; one week window
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const PREVIOUS_WEEK = 1  // replace with active-week logic from backend

// ── State ──────────────────────────────────────────────────────────────────
const loading       = ref(true)
const error         = ref('')
const alreadySubmitted = ref(false)

const teammates     = ref([])   // fetched from backend: { id, firstName, lastName }
const rubricCriteria = ref([])  // fetched from backend: { id, name, description, maxScore }

// scores[memberId][criterionId] = integer
const scores   = ref({})
const comments = ref({})  // { [memberId]: { pub: '', priv: '' } }

const activeIdx = ref(0)
const step      = ref(1)  // 1 = fill in, 2 = review, 3 = submitted
const submitting = ref(false)
const submitError = ref('')

// ── Derived ────────────────────────────────────────────────────────────────
const allMembers = computed(() => teammates.value)

const activeMember = computed(() => allMembers.value[activeIdx.value] ?? null)

const maxTotal = computed(() =>
  rubricCriteria.value.reduce((s, c) => s + c.maxScore, 0)
)

function memberTotal(memberId) {
  return rubricCriteria.value.reduce(
    (s, c) => s + (scores.value[memberId]?.[c.id] ?? 0), 0
  )
}

const allFilled = computed(() =>
  allMembers.value.every(m =>
    rubricCriteria.value.every(c => scores.value[m.id]?.[c.id] != null)
  )
)

// ── Load teammates + rubric + check if already submitted ───────────────────
onMounted(async () => {
  try {
    const [membersRes, rubricRes, existingRes] = await Promise.all([
      api.get('/api/v1/teams/my-team/members'),
      api.get('/api/v1/rubrics/active'),
      api.get(`/api/v1/peer-evaluations/my-submission?week=${PREVIOUS_WEEK}`),
    ])

    teammates.value     = membersRes.data ?? []
    rubricCriteria.value = rubricRes.data?.criteria ?? []

    // Initialise score/comment maps
    for (const m of teammates.value) {
      scores.value[m.id]   = Object.fromEntries(rubricCriteria.value.map(c => [c.id, 0]))
      comments.value[m.id] = { pub: '', priv: '' }
    }

    if (existingRes.data) {
      alreadySubmitted.value = true
    }
  } catch (e) {
    error.value = e.message ?? 'Failed to load peer evaluation data.'
  } finally {
    loading.value = false
  }
})

// ── Validation before review step ─────────────────────────────────────────
function goToReview() {
  // Scores must be integers — clamp any non-integer input
  for (const m of allMembers.value) {
    for (const c of rubricCriteria.value) {
      scores.value[m.id][c.id] = Math.round(Number(scores.value[m.id][c.id] ?? 0))
    }
  }
  step.value = 2
}

// ── Submit ─────────────────────────────────────────────────────────────────
async function submit() {
  submitting.value = true
  submitError.value = ''
  const payload = {
    week: PREVIOUS_WEEK,
    evaluations: allMembers.value.map(m => ({
      evaluateeId:   m.id,
      criterionScores: rubricCriteria.value.map(c => ({
        criterionId: c.id,
        score:       scores.value[m.id][c.id],
      })),
      publicComment:  comments.value[m.id].pub.trim(),
      privateComment: comments.value[m.id].priv.trim(),
    })),
  }
  try {
    await api.post('/api/v1/peer-evaluations', payload)
    step.value = 3
  } catch (e) {
    submitError.value = e.message ?? 'Submission failed. Please try again.'
  } finally {
    submitting.value = false
  }
}

const memberName = (m) => `${m.firstName} ${m.lastName}`
</script>

<template>
  <AppLayout>

    <!-- Loading -->
    <div v-if="loading" class="empty"><p>Loading…</p></div>

    <!-- Error -->
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <!-- Already submitted (BR-3) -->
    <div v-else-if="alreadySubmitted" class="empty">
      <div class="empty-icon">🔒</div>
      <h3>Already submitted</h3>
      <p class="muted mt-4" style="margin-top:10px">
        You already submitted a peer evaluation for Week {{ PREVIOUS_WEEK }}.
        Evaluations cannot be edited after submission (BR-3).
      </p>
    </div>

    <!-- Submitted this session -->
    <div v-else-if="step === 3" class="empty">
      <div class="empty-icon">✅</div>
      <h3>Peer evaluation submitted!</h3>
      <p class="muted" style="margin-top:10px">
        Your responses for Week {{ PREVIOUS_WEEK }} have been recorded and are now locked (BR-3).
      </p>
    </div>

    <!-- Step 1: Fill in evaluations -->
    <template v-else-if="step === 1">
      <div class="alert alert-info" style="margin-bottom:20px">
        Evaluating teammates for <strong>Week {{ PREVIOUS_WEEK }}</strong>.
        All scores must be integers. Submissions are final once confirmed (BR-3).
      </div>

      <!-- Member tabs -->
      <div style="display:flex;gap:0;border-bottom:2px solid var(--border);margin-bottom:20px;flex-wrap:wrap">
        <button
          v-for="(m, i) in allMembers"
          :key="m.id"
          :style="`padding:9px 16px;border:none;background:none;font-size:.875rem;font-weight:${activeIdx===i?'600':'400'};cursor:pointer;border-bottom:2px solid ${activeIdx===i?'var(--purple)':'transparent'};margin-bottom:-2px;color:${activeIdx===i?'var(--purple)':'var(--muted)'}`"
          @click="activeIdx = i"
        >
          {{ memberName(m) }}
          <span style="margin-left:5px;font-size:.72rem;color:var(--gold);font-weight:700">
            {{ memberTotal(m.id) }}/{{ maxTotal }}
          </span>
        </button>
      </div>

      <div v-if="activeMember" class="card">
        <div class="card-header">
          <h3>Evaluating: {{ memberName(activeMember) }}</h3>
          <span class="badge badge-purple">{{ memberTotal(activeMember.id) }} / {{ maxTotal }} pts</span>
        </div>

        <!-- Criterion scores — integer inputs per spec -->
        <div v-for="c in rubricCriteria" :key="c.id" style="margin-bottom:18px">
          <div class="flex items-center justify-between" style="margin-bottom:3px">
            <strong>{{ c.name }}</strong>
            <span class="muted" style="font-size:.8rem">0 – {{ c.maxScore }} pts</span>
          </div>
          <p class="muted" style="font-size:.8rem;margin-bottom:6px">{{ c.description }}</p>
          <input
            type="number"
            :min="0"
            :max="c.maxScore"
            step="1"
            v-model.number="scores[activeMember.id][c.id]"
            style="width:120px"
          />
        </div>

        <div class="form-group">
          <label>Public Comment <span style="font-weight:400;text-transform:none">(visible to evaluatee)</span></label>
          <textarea v-model="comments[activeMember.id].pub" placeholder="Strengths, contributions…" />
        </div>
        <div class="form-group">
          <label>Private Comment <span style="font-weight:400;text-transform:none">(instructor only)</span></label>
          <textarea v-model="comments[activeMember.id].priv" placeholder="Confidential feedback…" />
        </div>
      </div>

      <!-- Navigation -->
      <div class="flex justify-between items-center mt-4">
        <div class="flex gap-2">
          <button class="btn btn-secondary" :disabled="activeIdx === 0" @click="activeIdx--">← Previous</button>
          <button class="btn btn-secondary" :disabled="activeIdx === allMembers.length - 1" @click="activeIdx++">Next →</button>
        </div>
        <button
          v-if="activeIdx === allMembers.length - 1"
          class="btn btn-primary"
          :disabled="!allFilled"
          @click="goToReview"
        >
          Review & Submit →
        </button>
      </div>
    </template>

    <!-- Step 2: Review before confirming (UC-28 step 5-6) -->
    <template v-else-if="step === 2">
      <div class="alert alert-warning" style="margin-bottom:20px">
        Please review your evaluations carefully. <strong>Submissions cannot be edited after confirmation (BR-3).</strong>
      </div>

      <div v-if="submitError" class="alert alert-error">{{ submitError }}</div>

      <div v-for="m in allMembers" :key="m.id" class="card mb-4">
        <div class="card-header">
          <h3>{{ memberName(m) }}</h3>
          <span class="badge badge-purple">{{ memberTotal(m.id) }} / {{ maxTotal }} pts</span>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Criterion</th><th>Score</th><th>Max</th></tr>
            </thead>
            <tbody>
              <tr v-for="c in rubricCriteria" :key="c.id">
                <td>{{ c.name }}</td>
                <td><strong>{{ scores[m.id][c.id] }}</strong></td>
                <td class="muted">{{ c.maxScore }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="comments[m.id].pub" style="margin-top:10px">
          <span class="muted" style="font-size:.78rem">PUBLIC:</span>
          <p style="margin-top:3px;font-size:.875rem">{{ comments[m.id].pub }}</p>
        </div>
      </div>

      <div class="flex justify-between items-center mt-4">
        <button class="btn btn-secondary" @click="step = 1">← Back to Edit</button>
        <button class="btn btn-primary" :disabled="submitting" @click="submit">
          {{ submitting ? 'Submitting…' : 'Confirm & Submit' }}
        </button>
      </div>
    </template>

  </AppLayout>
</template>
