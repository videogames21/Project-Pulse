<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { sectionsApi } from '../../api/sections.js'
import { rubricsApi } from '../../api/rubrics.js'
import { usersApi } from '../../api/users.js'

const route  = useRoute()
const router = useRouter()

// ── Wizard state ──────────────────────────────────────────────────────────────
const step = ref(1)
const loading = ref(true)
const loadError = ref('')

// Step 1 — section details
const form = ref({ name: '', startDate: '', endDate: '' })
const step1Error = ref('')
const instructors = ref([])
const selectedInstructorIds = ref([])

// Step 2 — rubric selection
const rubrics = ref([])
const selectedRubricId = ref(null)

// Step 3 — criteria editing
const editableCriteria = ref([])
const criteriaEdited = ref(false)
const step3Error = ref('')

// Step 4 — submit
const submitting = ref(false)
const submitError = ref('')

// ── Derived ───────────────────────────────────────────────────────────────────
const selectedRubric = computed(() =>
  rubrics.value.find(r => r.id === selectedRubricId.value) ?? null
)

const criteriaTotal = computed(() =>
  editableCriteria.value.reduce((sum, c) => sum + Number(c.maxScore ?? 0), 0)
)

// ── Lifecycle — load existing section + rubrics ───────────────────────────────
onMounted(async () => {
  try {
    const [sectionRes, rubricsRes, instructorsRes] = await Promise.allSettled([
      sectionsApi.getById(route.params.id),
      rubricsApi.getAll(),
      usersApi.getInstructors(undefined, true),
    ])

    if (sectionRes.status === 'rejected') {
      const status = sectionRes.reason?.status ?? sectionRes.reason?.response?.status
      loadError.value = status === 404 ? 'Section not found.' : 'Failed to load section.'
      return
    }

    const s = sectionRes.value.data
    form.value.name      = s.sectionName ?? ''
    form.value.startDate = s.startDate   ?? ''
    form.value.endDate   = s.endDate     ?? ''

    if (rubricsRes.status === 'rejected') {
      if (s.rubricName) {
        loadError.value = 'Failed to load rubrics. Cannot edit a section with a linked rubric — please try again.'
        return
      }
    } else {
      rubrics.value = rubricsRes.value.data ?? []
    }

    if (instructorsRes.status === 'fulfilled') instructors.value = instructorsRes.value.data ?? []

    // Pre-select the rubric and instructors that were previously linked
    if (s.rubricId) selectedRubricId.value = s.rubricId
    selectedInstructorIds.value = (s.instructors ?? []).map(i => i.id)
  } finally {
    loading.value = false
  }
})

// ── Step 1 ────────────────────────────────────────────────────────────────────
function validateStep1() {
  step1Error.value = ''
  if (!form.value.name.trim()) {
    step1Error.value = 'Section name is required.'
    return false
  }
  if (form.value.startDate && form.value.endDate && form.value.startDate > form.value.endDate) {
    step1Error.value = 'Start date must be before end date.'
    return false
  }
  return true
}

function goToStep2() {
  if (validateStep1()) {
    submitError.value = ''
    step.value = 2
  }
}

// ── Step 2 ────────────────────────────────────────────────────────────────────
function selectRubric(id) {
  selectedRubricId.value = id
}

function goToStep3() {
  if (selectedRubricId.value === null) {
    step.value = 4
    return
  }
  editableCriteria.value = selectedRubric.value.criteria.map(c => ({
    name: c.name,
    description: c.description ?? '',
    maxScore: Number(c.maxScore),
  }))
  criteriaEdited.value = false
  step3Error.value = ''
  step.value = 3
}

// ── Step 3 ────────────────────────────────────────────────────────────────────
function markEdited() {
  criteriaEdited.value = true
}

function removeCriterion(index) {
  if (editableCriteria.value.length === 1) return
  editableCriteria.value.splice(index, 1)
  criteriaEdited.value = true
}

function addCriterion() {
  editableCriteria.value.push({ name: '', description: '', maxScore: 10 })
  criteriaEdited.value = true
}

function goToStep4() {
  step3Error.value = ''
  for (const c of editableCriteria.value) {
    if (!c.name.trim()) {
      step3Error.value = 'All criterion names are required.'
      return
    }
    if (!c.maxScore || Number(c.maxScore) <= 0) {
      step3Error.value = 'All max scores must be greater than 0.'
      return
    }
  }
  step.value = 4
}

// ── Step 4 — submit ───────────────────────────────────────────────────────────
async function confirm() {
  submitting.value = true
  submitError.value = ''
  try {
    const payload = {
      name: form.value.name.trim(),
      startDate: form.value.startDate || null,
      endDate:   form.value.endDate   || null,
    }

    payload.instructorIds = selectedInstructorIds.value

    if (selectedRubricId.value !== null) {
      payload.rubricId = selectedRubricId.value
      if (criteriaEdited.value) {
        payload.criteria = editableCriteria.value.map(c => ({
          name:        c.name.trim(),
          description: c.description.trim(),
          maxScore:    Number(c.maxScore),
        }))
      }
    }

    await sectionsApi.update(route.params.id, payload)
    router.push('/admin/sections')
  } catch (err) {
    const status = err.status ?? err.response?.status
    if (status === 409) {
      submitError.value = 'A section with that name already exists.'
      step.value = 1
      step1Error.value = submitError.value
    } else {
      submitError.value = 'Something went wrong. Please try again.'
    }
  } finally {
    submitting.value = false
  }
}

function cancel() {
  router.push(`/admin/sections/${route.params.id}`)
}

// ── Step labels ───────────────────────────────────────────────────────────────
const stepLabels = ['Details', 'Rubric', 'Criteria', 'Confirm']
</script>

<template>
  <AppLayout>

    <div v-if="loading" class="muted" style="padding:24px">Loading…</div>
    <div v-else-if="loadError" class="alert alert-error">{{ loadError }}</div>

    <template v-else>

      <!-- Breadcrumb / cancel bar -->
      <div class="flex items-center gap-2" style="margin-bottom:20px">
        <button class="btn btn-secondary btn-sm" @click="cancel">← Cancel</button>
        <span class="muted" style="font-size:.85rem">Edit Senior Design Section</span>
      </div>

      <!-- Step indicators -->
      <div class="flex gap-2 items-center" style="margin-bottom:24px">
        <template v-for="(label, i) in stepLabels" :key="i">
          <div
            class="flex items-center gap-1"
            :style="step === i + 1 ? 'font-weight:700;color:var(--primary)' : step > i + 1 ? 'color:var(--green)' : 'color:var(--muted)'"
            style="font-size:.85rem"
          >
            <span
              style="display:inline-flex;align-items:center;justify-content:center;width:22px;height:22px;border-radius:50%;font-size:.75rem;font-weight:700"
              :style="step === i + 1 ? 'background:var(--primary);color:#fff' : step > i + 1 ? 'background:var(--green);color:#fff' : 'background:var(--surface-2);color:var(--muted)'"
            >{{ step > i + 1 ? '✓' : i + 1 }}</span>
            {{ label }}
          </div>
          <span v-if="i < stepLabels.length - 1" class="muted">›</span>
        </template>
      </div>

      <!-- ── Step 1: Section Details ─────────────────────────────────────────── -->
      <div v-if="step === 1" class="card" style="max-width:520px">
        <div class="card-header">
          <h3 style="font-size:1rem;font-weight:700">Section Details</h3>
        </div>
        <div style="padding:20px">
          <div v-if="step1Error" class="alert alert-error" style="margin-bottom:16px">
            {{ step1Error }}
          </div>

          <div class="form-group" style="margin-bottom:16px">
            <label>Section Name <span style="color:var(--red)">*</span></label>
            <input
              v-model="form.name"
              placeholder="e.g. Section 2025-2026"
              @input="step1Error = ''"
              @keyup.enter="goToStep2"
            />
          </div>

          <div class="form-group" style="margin-bottom:16px">
            <label>Start Date</label>
            <input type="date" v-model="form.startDate" />
          </div>

          <div class="form-group" style="margin-bottom:16px">
            <label>End Date</label>
            <input type="date" v-model="form.endDate" />
          </div>

          <div class="form-group" style="margin-bottom:24px">
            <label>Section Instructors</label>
            <p class="muted" style="font-size:.78rem;margin-bottom:6px">Select all instructors assigned to this section.</p>
            <div v-if="instructors.length === 0" class="muted" style="font-size:.85rem">No instructors available.</div>
            <div
              v-for="i in instructors"
              :key="i.id"
              style="display:flex;align-items:center;gap:8px;padding:6px 0;border-bottom:1px solid var(--surface-2)"
            >
              <input
                type="checkbox"
                :id="'instr-' + i.id"
                :value="i.id"
                v-model="selectedInstructorIds"
                style="accent-color:var(--primary)"
              />
              <label :for="'instr-' + i.id" style="cursor:pointer;margin:0">
                {{ i.firstName }} {{ i.lastName }}
              </label>
            </div>
          </div>

          <div class="flex gap-2 justify-end">
            <button class="btn btn-secondary" @click="cancel">Cancel</button>
            <button class="btn btn-primary" @click="goToStep2">Next: Choose Rubric →</button>
          </div>
        </div>
      </div>

      <!-- ── Step 2: Rubric Selection ───────────────────────────────────────── -->
      <div v-else-if="step === 2" class="card" style="max-width:620px">
        <div class="card-header">
          <h3 style="font-size:1rem;font-weight:700">Choose a Rubric for Peer Evaluation</h3>
        </div>
        <div style="padding:20px">

          <p class="muted" style="font-size:.85rem;margin-bottom:12px">Select an existing rubric, or skip to leave it unset.</p>

          <div v-if="rubrics.length === 0" class="muted" style="margin-bottom:8px;font-size:.85rem">
            No rubrics exist yet.
            <a href="/admin/rubrics" style="color:var(--primary);margin-left:4px">Create one first →</a>
          </div>

          <div
            v-for="r in rubrics"
            :key="r.id"
            class="card"
            style="padding:12px 16px;margin-bottom:8px;cursor:pointer;border:2px solid transparent;transition:border .15s"
            :style="selectedRubricId === r.id ? 'border-color:var(--primary);background:var(--surface-2)' : ''"
            @click="selectRubric(r.id)"
          >
            <div class="flex items-center gap-3">
              <input
                type="radio"
                :value="r.id"
                v-model="selectedRubricId"
                style="width:16px;height:16px;accent-color:var(--primary)"
              />
              <div>
                <p style="font-weight:600;margin:0">{{ r.name }}</p>
                <p class="muted" style="font-size:.78rem;margin:2px 0 0">
                  {{ r.criteria.length }} criteria ·
                  {{ r.criteria.reduce((s, c) => s + Number(c.maxScore ?? 0), 0) }} pts total
                </p>
              </div>
            </div>
          </div>

          <!-- No rubric option -->
          <div
            class="card"
            style="padding:12px 16px;margin-bottom:8px;cursor:pointer;border:2px solid transparent;transition:border .15s"
            :style="selectedRubricId === null ? 'border-color:var(--primary);background:var(--surface-2)' : ''"
            @click="selectedRubricId = null"
          >
            <div class="flex items-center gap-3">
              <input
                type="radio"
                :value="null"
                v-model="selectedRubricId"
                style="width:16px;height:16px;accent-color:var(--primary)"
              />
              <div>
                <p style="font-weight:600;margin:0">No rubric</p>
                <p class="muted" style="font-size:.78rem;margin:2px 0 0">Remove the rubric from this section.</p>
              </div>
            </div>
          </div>

          <div v-if="rubrics.length > 0" style="margin-top:4px">
            <a href="/admin/rubrics" style="color:var(--primary);font-size:.82rem">+ Create a new rubric first →</a>
          </div>

          <div class="flex gap-2 justify-end" style="margin-top:24px">
            <button class="btn btn-secondary" @click="step = 1">← Back</button>
            <button class="btn btn-secondary" @click="cancel">Cancel</button>
            <button class="btn btn-primary" @click="goToStep3">
              {{ selectedRubricId !== null ? 'Next: Review Criteria →' : 'Next: Confirm →' }}
            </button>
          </div>
        </div>
      </div>

      <!-- ── Step 3: Criteria Review / Edit ─────────────────────────────────── -->
      <div v-else-if="step === 3" class="card" style="max-width:700px">
        <div class="card-header">
          <h3 style="font-size:1rem;font-weight:700">
            Review Rubric Criteria
            <span v-if="criteriaEdited" class="badge badge-purple" style="font-size:.72rem;margin-left:8px">Edited — will duplicate rubric</span>
          </h3>
        </div>
        <div style="padding:20px">

          <div v-if="step3Error" class="alert alert-error" style="margin-bottom:16px">
            {{ step3Error }}
          </div>

          <p class="muted" style="font-size:.85rem;margin-bottom:16px">
            Review the criteria below. Any changes will create a private copy of
            <strong>{{ selectedRubric?.name }}</strong> linked to this section only.
          </p>

          <div
            v-for="(c, i) in editableCriteria"
            :key="i"
            class="card"
            style="padding:12px;margin-bottom:8px;background:var(--surface-2)"
          >
            <div style="display:flex;gap:8px;align-items:flex-start">
              <div style="flex:1">
                <div class="form-group" style="margin-bottom:8px">
                  <label style="font-size:.78rem">Name <span style="color:var(--red)">*</span></label>
                  <input v-model="c.name" @input="markEdited" />
                </div>
                <div class="form-group" style="margin-bottom:8px">
                  <label style="font-size:.78rem">Description</label>
                  <input v-model="c.description" @input="markEdited" />
                </div>
                <div class="form-group">
                  <label style="font-size:.78rem">Max Score <span style="color:var(--red)">*</span></label>
                  <input
                    type="number"
                    v-model.number="c.maxScore"
                    min="0.01"
                    step="0.5"
                    style="max-width:100px"
                    @input="markEdited"
                  />
                </div>
              </div>
              <button
                class="btn btn-sm"
                style="padding:4px 8px"
                :style="editableCriteria.length === 1
                  ? 'color:var(--muted);background:transparent;border:1px solid var(--muted);cursor:not-allowed'
                  : 'color:var(--red);background:transparent;border:1px solid var(--red)'"
                :disabled="editableCriteria.length === 1"
                :title="editableCriteria.length === 1 ? 'At least one criterion required' : 'Remove criterion'"
                @click="removeCriterion(i)"
              >×</button>
            </div>
          </div>

          <button class="btn btn-secondary btn-sm" style="margin-top:4px" @click="addCriterion">
            + Add Criterion
          </button>

          <div class="muted" style="font-size:.8rem;margin-top:8px">
            Total: {{ criteriaTotal }} pts across {{ editableCriteria.length }} criteria
          </div>

          <div class="flex gap-2 justify-end" style="margin-top:24px">
            <button class="btn btn-secondary" @click="step = 2">← Back</button>
            <button class="btn btn-secondary" @click="cancel">Cancel</button>
            <button class="btn btn-primary" @click="goToStep4">Next: Confirm →</button>
          </div>
        </div>
      </div>

      <!-- ── Step 4: Confirmation Summary ───────────────────────────────────── -->
      <div v-else-if="step === 4" class="card" style="max-width:620px">
        <div class="card-header">
          <h3 style="font-size:1rem;font-weight:700">Confirm Section Changes</h3>
        </div>
        <div style="padding:20px">

          <div v-if="submitError" class="alert alert-error" style="margin-bottom:16px">
            {{ submitError }}
          </div>

          <p class="muted" style="font-size:.85rem;margin-bottom:20px">
            Review the changes below. Click <strong>Save Changes</strong> to update the section.
          </p>

          <!-- Section details summary -->
          <div class="card" style="padding:16px;margin-bottom:16px;background:var(--surface-2)">
            <p class="muted" style="font-size:.72rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;margin-bottom:8px">Section Details</p>
            <div class="grid-2" style="gap:12px">
              <div>
                <p class="muted" style="font-size:.72rem;text-transform:uppercase;font-weight:600">Name</p>
                <p style="font-weight:700">{{ form.name }}</p>
              </div>
              <div>
                <p class="muted" style="font-size:.72rem;text-transform:uppercase;font-weight:600">Start Date</p>
                <p>{{ form.startDate || '—' }}</p>
              </div>
              <div>
                <p class="muted" style="font-size:.72rem;text-transform:uppercase;font-weight:600">End Date</p>
                <p>{{ form.endDate || '—' }}</p>
              </div>
              <div>
                <p class="muted" style="font-size:.72rem;text-transform:uppercase;font-weight:600">Instructors</p>
                <p v-if="selectedInstructorIds.length === 0">—</p>
                <ul v-else style="margin:0;padding-left:16px;font-size:.88rem">
                  <li v-for="instrId in selectedInstructorIds" :key="instrId">
                    {{ instructors.find(i => i.id === instrId)?.firstName }}
                    {{ instructors.find(i => i.id === instrId)?.lastName }}
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <!-- Rubric summary -->
          <div class="card" style="padding:16px;background:var(--surface-2)">
            <p class="muted" style="font-size:.72rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;margin-bottom:8px">Rubric</p>

            <div v-if="selectedRubricId === null" class="muted">None.</div>

            <template v-else>
              <p style="font-weight:600;margin-bottom:4px">
                {{ criteriaEdited ? 'Copy of ' + selectedRubric?.name : selectedRubric?.name }}
                <span v-if="criteriaEdited" class="badge badge-purple" style="font-size:.7rem;margin-left:6px">Edited copy</span>
              </p>

              <div v-if="editableCriteria.length > 0" class="table-wrap" style="margin-top:8px">
                <table>
                  <thead>
                    <tr><th>#</th><th>Criterion</th><th>Description</th><th>Max Score</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="(c, i) in editableCriteria" :key="i">
                      <td class="muted">{{ i + 1 }}</td>
                      <td><strong>{{ c.name }}</strong></td>
                      <td class="muted">{{ c.description || '—' }}</td>
                      <td><span class="badge badge-purple">{{ c.maxScore }} pts</span></td>
                    </tr>
                    <tr style="font-weight:700;background:var(--surface-2)">
                      <td colspan="3" style="text-align:right">Total</td>
                      <td><span class="badge badge-purple">{{ criteriaTotal }} pts</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="muted" style="font-size:.82rem">No criteria defined.</p>
            </template>
          </div>

          <div class="flex gap-2 justify-end" style="margin-top:24px">
            <button class="btn btn-secondary" @click="selectedRubricId === null ? step = 2 : step = 3">← Back</button>
            <button class="btn btn-secondary" @click="cancel">Cancel</button>
            <button class="btn btn-primary" :disabled="submitting" @click="confirm">
              {{ submitting ? 'Saving…' : 'Save Changes' }}
            </button>
          </div>
        </div>
      </div>

    </template>
  </AppLayout>
</template>
