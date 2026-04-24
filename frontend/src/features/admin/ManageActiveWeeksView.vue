<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { sectionsApi } from '../../api/sections.js'
import { activeWeeksApi } from '../../api/activeWeeks.js'

const route  = useRoute()
const router = useRouter()

const section    = ref(null)
const loadError  = ref('')
const step       = ref(1)  // 1 = select, 2 = review

// All Mondays in the section's date range
const allWeeks   = ref([])
// Set of active week date strings "YYYY-MM-DD" (checked = active)
const activeSet  = ref(new Set())

const selectError  = ref('')
const saving       = ref(false)
const saveError    = ref('')

// ── Helpers ───────────────────────────────────────────────────────────────

function generateAllMondays(startDate, endDate) {
  const weeks = []
  let current = new Date(startDate + 'T00:00:00')
  // Advance to first Monday on or after startDate (Monday = day 1)
  while (current.getDay() !== 1) current.setDate(current.getDate() + 1)
  const end = new Date(endDate + 'T00:00:00')
  while (current <= end) {
    const y = current.getFullYear()
    const m = String(current.getMonth() + 1).padStart(2, '0')
    const d = String(current.getDate()).padStart(2, '0')
    weeks.push(`${y}-${m}-${d}`)
    current = new Date(current)
    current.setDate(current.getDate() + 7)
  }
  return weeks
}

function formatDate(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  return d.toLocaleDateString('en-US', { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' })
}

function weekLabel(dateStr, index) {
  return `Week ${index + 1} — ${formatDate(dateStr)}`
}

// ── Lifecycle ─────────────────────────────────────────────────────────────

onMounted(async () => {
  try {
    const [sectionRes, weeksRes] = await Promise.all([
      sectionsApi.getById(route.params.id),
      activeWeeksApi.get(route.params.id),
    ])

    section.value = sectionRes.data

    if (!section.value.startDate || !section.value.endDate) {
      loadError.value = 'This section does not have a start and end date set. Please edit the section first.'
      return
    }

    allWeeks.value = generateAllMondays(section.value.startDate, section.value.endDate)

    const saved = new Set(weeksRes.data.activeWeeks ?? [])

    if (saved.size === 0) {
      // No prior setup — default all weeks active
      activeSet.value = new Set(allWeeks.value)
    } else {
      // Prior setup exists. Start with all current weeks active, then re-apply the
      // admin's previous choices only for weeks that were in the original saved range.
      // Weeks outside that range are new (date was expanded) and default to active.
      const sortedSaved = [...saved].sort()
      const firstSaved  = sortedSaved[0]
      const lastSaved   = sortedSaved[sortedSaved.length - 1]

      const active = new Set(allWeeks.value)
      for (const w of allWeeks.value) {
        // Week was inside the previously configured range but NOT saved → explicitly inactive
        if (w >= firstSaved && w <= lastSaved && !saved.has(w)) {
          active.delete(w)
        }
      }
      activeSet.value = active
    }
  } catch (e) {
    loadError.value = 'Failed to load section data. Please try again.'
  }
})

// ── Step 1 actions ────────────────────────────────────────────────────────

function toggleWeek(dateStr) {
  const next = new Set(activeSet.value)
  if (next.has(dateStr)) {
    next.delete(dateStr)
  } else {
    next.add(dateStr)
  }
  activeSet.value = next
}

function selectAll() {
  activeSet.value = new Set(allWeeks.value)
}

function clearAll() {
  activeSet.value = new Set()
}

function goToReview() {
  selectError.value = ''
  if (activeSet.value.size === 0) {
    selectError.value = 'Please select at least one active week.'
    return
  }
  step.value = 2
}

// ── Step 2 actions ────────────────────────────────────────────────────────

function selectedWeeks() {
  return allWeeks.value.filter(d => activeSet.value.has(d))
}

async function confirmSave() {
  saving.value  = true
  saveError.value = ''
  try {
    await activeWeeksApi.save(route.params.id, selectedWeeks())
    router.push(`/admin/sections/${route.params.id}`)
  } catch (e) {
    saveError.value = e.response?.data?.message ?? e.message ?? 'Failed to save active weeks.'
    saving.value = false
  }
}
</script>

<template>
  <AppLayout>
    <!-- Back button -->
    <div style="margin-bottom:16px">
      <button class="btn btn-secondary btn-sm" @click="router.push(`/admin/sections/${route.params.id}`)">← Back to Section</button>
    </div>

    <div v-if="loadError" class="alert alert-error">{{ loadError }}</div>

    <div v-else-if="!section" class="muted">Loading…</div>

    <template v-else>
      <!-- Step indicators -->
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px">
        <div style="display:flex;align-items:center;gap:8px">
          <span
            style="width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:.85rem;font-weight:700"
            :style="step === 1
              ? 'background:var(--purple);color:#fff'
              : 'background:var(--surface-raised);color:var(--text-muted);border:1px solid var(--border)'"
          >1</span>
          <span :style="step === 1 ? 'font-weight:600' : 'color:var(--text-muted)'">Select Active Weeks</span>
        </div>
        <span style="color:var(--border)">→</span>
        <div style="display:flex;align-items:center;gap:8px">
          <span
            style="width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:.85rem;font-weight:700"
            :style="step === 2
              ? 'background:var(--purple);color:#fff'
              : 'background:var(--surface-raised);color:var(--text-muted);border:1px solid var(--border)'"
          >2</span>
          <span :style="step === 2 ? 'font-weight:600' : 'color:var(--text-muted)'">Review &amp; Confirm</span>
        </div>
      </div>

      <!-- ── Step 1: Select weeks ── -->
      <template v-if="step === 1">
        <div class="card">
          <div class="card-header">
            <div>
              <h2 style="font-size:1.05rem;font-weight:700">Active Weeks — {{ section.sectionName }}</h2>
              <p class="muted" style="font-size:.85rem;margin-top:4px">
                Check the weeks students must submit WARs and peer evaluations. Uncheck holiday or inactive weeks.
              </p>
            </div>
          </div>

          <div style="display:flex;gap:8px;margin-bottom:16px">
            <button class="btn btn-secondary btn-sm" @click="selectAll">Select All</button>
            <button class="btn btn-secondary btn-sm" @click="clearAll">Clear All</button>
            <span class="muted" style="margin-left:auto;font-size:.85rem;align-self:center">
              {{ activeSet.size }} of {{ allWeeks.length }} weeks active
            </span>
          </div>

          <div v-if="selectError" class="alert alert-error" style="margin-bottom:12px">{{ selectError }}</div>

          <div style="max-height:480px;overflow-y:auto;border:1px solid var(--border);border-radius:6px">
            <div
              v-for="(dateStr, index) in allWeeks"
              :key="dateStr"
              style="display:flex;align-items:center;gap:12px;padding:10px 14px;cursor:pointer;border-bottom:1px solid var(--border)"
              :style="index === allWeeks.length - 1 ? 'border-bottom:none' : ''"
              @click="toggleWeek(dateStr)"
            >
              <input
                type="checkbox"
                :checked="activeSet.has(dateStr)"
                style="width:16px;height:16px;cursor:pointer;accent-color:var(--purple)"
                @click.stop="toggleWeek(dateStr)"
              />
              <span :style="activeSet.has(dateStr) ? '' : 'color:var(--text-muted);text-decoration:line-through'">
                {{ weekLabel(dateStr, index) }}
              </span>
              <span
                v-if="!activeSet.has(dateStr)"
                style="margin-left:auto;font-size:.75rem;color:#dc2626;font-weight:600"
              >Inactive</span>
            </div>
          </div>

          <div style="margin-top:16px;display:flex;justify-content:flex-end">
            <button class="btn btn-primary" @click="goToReview">Review →</button>
          </div>
        </div>
      </template>

      <!-- ── Step 2: Review & Confirm ── -->
      <template v-if="step === 2">
        <div class="card">
          <div class="card-header">
            <h2 style="font-size:1.05rem;font-weight:700">Review Active Weeks — {{ section.sectionName }}</h2>
          </div>

          <p class="muted" style="margin-bottom:16px;font-size:.875rem">
            The following <strong>{{ selectedWeeks().length }}</strong> weeks will be marked as active.
            Students must submit WARs and peer evaluations during these weeks.
          </p>

          <div v-if="saveError" class="alert alert-error" style="margin-bottom:12px">{{ saveError }}</div>

          <div style="border:1px solid var(--border);border-radius:6px;max-height:480px;overflow-y:auto">
            <div
              v-for="(dateStr, index) in selectedWeeks()"
              :key="dateStr"
              style="display:flex;align-items:center;gap:12px;padding:10px 14px;border-bottom:1px solid var(--border)"
              :style="index === selectedWeeks().length - 1 ? 'border-bottom:none' : ''"
            >
              <span style="color:var(--purple);font-weight:700;font-size:.85rem;width:24px">{{ index + 1 }}</span>
              <span>{{ formatDate(dateStr) }}</span>
            </div>
          </div>

          <div style="margin-top:16px;display:flex;gap:8px;justify-content:flex-end">
            <button class="btn btn-secondary" :disabled="saving" @click="step = 1">← Back</button>
            <button class="btn btn-primary" :disabled="saving" @click="confirmSave">
              {{ saving ? 'Saving…' : 'Confirm & Save' }}
            </button>
          </div>
        </div>
      </template>
    </template>
  </AppLayout>
</template>
