import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const STORAGE_KEY = 'pp_peer_eval_draft'

function loadDraft() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const usePeerEvalStore = defineStore('peerEval', () => {
  const saved = loadDraft()

  const scores        = ref(saved?.scores        ?? {})
  const comments      = ref(saved?.comments      ?? {})
  const submitted     = ref(saved?.submitted     ?? false)
  const weekStart     = ref(saved?.weekStart     ?? null)
  const teamId        = ref(saved?.teamId        ?? null)
  // evaluateeId → existing evaluation ID (used to switch POST → PUT on re-submit)
  const evaluationIds = ref(saved?.evaluationIds ?? {})

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      scores:        scores.value,
      comments:      comments.value,
      submitted:     submitted.value,
      weekStart:     weekStart.value,
      teamId:        teamId.value,
      evaluationIds: evaluationIds.value,
    }))
  }

  function reset() {
    scores.value        = {}
    comments.value      = {}
    submitted.value     = false
    weekStart.value     = null
    teamId.value        = null
    evaluationIds.value = {}
    localStorage.removeItem(STORAGE_KEY)
  }

  watch([scores, comments, submitted, weekStart, teamId, evaluationIds], persist, { deep: true })

  return { scores, comments, submitted, weekStart, teamId, evaluationIds, reset }
})
