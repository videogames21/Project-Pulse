<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

// ── State ──────────────────────────────────────────────────
const rubrics   = ref([])
const expanded  = ref(null)
const loading   = ref(false)
const flash     = ref({ text: '', type: '' })

// Modal (shared for create + edit)
const showModal  = ref(false)
const editingId  = ref(null)       // null = create mode, number = edit mode
const step       = ref(1)
const saving     = ref(false)
const formError  = ref('')

const form = ref({
  name: '',
  criteria: [{ name: '', description: '', maxScore: 10 }],
})

// Delete confirm state
const deletingId    = ref(null)    // rubric id pending delete confirmation
const deleteLoading = ref(false)

// ── Helpers ────────────────────────────────────────────────
function totalMax(rubric) {
  return rubric.criteria.reduce((s, c) => s + Number(c.maxScore ?? 0), 0)
}

function showFlash(text, type = 'success') {
  flash.value = { text, type }
  setTimeout(() => flash.value = { text: '', type: '' }, 3500)
}

// ── API calls ──────────────────────────────────────────────
async function loadRubrics() {
  loading.value = true
  try {
    const res = await api.get('/api/v1/rubrics')
    rubrics.value = res.data ?? []
    if (rubrics.value.length > 0 && expanded.value === null) {
      expanded.value = rubrics.value[0].id
    }
  } catch {
    showFlash('Failed to load rubrics.', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(loadRubrics)

// ── Create flow ────────────────────────────────────────────
function openCreateModal() {
  form.value = { name: '', criteria: [{ name: '', description: '', maxScore: 10 }] }
  formError.value = ''
  editingId.value = null
  step.value = 1
  showModal.value = true
}

// ── Edit flow ──────────────────────────────────────────────
function openEditModal(rubric) {
  form.value = {
    name: rubric.name,
    criteria: rubric.criteria.map(c => ({
      name: c.name,
      description: c.description ?? '',
      maxScore: Number(c.maxScore),
    })),
  }
  formError.value = ''
  editingId.value = rubric.id
  step.value = 1
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

function addCriterionRow() {
  form.value.criteria.push({ name: '', description: '', maxScore: 10 })
}

function removeCriterionRow(index) {
  if (form.value.criteria.length === 1) return
  form.value.criteria.splice(index, 1)
}

function goToPreview() {
  formError.value = ''
  if (!form.value.name.trim()) {
    formError.value = 'Rubric name is required.'
    return
  }
  for (const c of form.value.criteria) {
    if (!c.name.trim()) {
      formError.value = 'All criterion names are required.'
      return
    }
    if (!c.maxScore || Number(c.maxScore) <= 0) {
      formError.value = 'All max scores must be greater than 0.'
      return
    }
  }
  step.value = 2
}

async function confirmSave() {
  saving.value = true
  formError.value = ''
  try {
    const payload = {
      name: form.value.name.trim(),
      criteria: form.value.criteria.map(c => ({
        name: c.name.trim(),
        description: c.description.trim(),
        maxScore: Number(c.maxScore),
      })),
    }

    if (editingId.value !== null) {
      await api.put(`/api/v1/rubrics/${editingId.value}`, payload)
      showFlash('Rubric "' + payload.name + '" updated successfully.')
    } else {
      await api.post('/api/v1/rubrics', payload)
      showFlash('Rubric "' + payload.name + '" created successfully.')
    }

    showModal.value = false
    await loadRubrics()
  } catch (err) {
    const status = err.status ?? err.response?.status
    if (status === 409) {
      formError.value = 'A rubric with that name already exists.'
      step.value = 1
    } else {
      formError.value = 'Something went wrong. Please try again.'
      step.value = 1
    }
  } finally {
    saving.value = false
  }
}

// ── Delete flow ────────────────────────────────────────────
function promptDelete(rubric) {
  deletingId.value = rubric.id
}

function cancelDelete() {
  deletingId.value = null
}

async function confirmDelete(rubric) {
  deleteLoading.value = true
  try {
    await api.delete(`/api/v1/rubrics/${rubric.id}`)
    deletingId.value = null
    if (expanded.value === rubric.id) expanded.value = null
    showFlash('Rubric "' + rubric.name + '" deleted.')
    await loadRubrics()
  } catch {
    showFlash('Failed to delete rubric.', 'error')
    deletingId.value = null
  } finally {
    deleteLoading.value = false
  }
}

const previewTotal = computed(() =>
  form.value.criteria.reduce((s, c) => s + Number(c.maxScore ?? 0), 0)
)

const modalTitle = computed(() => editingId.value !== null ? 'Edit Rubric' : 'Create Rubric')
const confirmLabel = computed(() => {
  if (saving.value) return 'Saving…'
  return editingId.value !== null ? 'Confirm & Save' : 'Confirm & Create'
})
</script>

<template>
  <AppLayout>

    <!-- Flash message -->
    <div v-if="flash.text" :class="['alert', flash.type === 'error' ? 'alert-error' : 'alert-success']">
      {{ flash.text }}
    </div>

    <!-- Page header -->
    <div class="page-header">
      <h2 style="font-size:1.2rem;font-weight:700">Rubrics</h2>
      <button class="btn btn-primary" @click="openCreateModal">+ Create Rubric</button>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="empty"><p>Loading rubrics…</p></div>

    <!-- Empty state -->
    <div v-else-if="rubrics.length === 0" class="empty">
      <p>No rubrics yet. Create one to get started.</p>
    </div>

    <!-- Rubric cards -->
    <div v-else>
      <div v-for="r in rubrics" :key="r.id" class="card mb-4">
        <div
          class="card-header"
          style="cursor:pointer"
          @click="expanded = expanded === r.id ? null : r.id"
        >
          <div>
            <h3>{{ r.name }}</h3>
            <p class="muted" style="font-size:.8rem;margin-top:2px">
              {{ r.criteria.length }} criteria · {{ totalMax(r) }} pts total
            </p>
          </div>
          <div class="flex items-center gap-2">
            <span class="badge badge-purple">{{ totalMax(r) }} pts</span>

            <!-- Edit button -->
            <button
              class="btn btn-sm btn-secondary"
              style="padding:4px 10px;font-size:.78rem"
              @click.stop="openEditModal(r)"
            >Edit</button>

            <!-- Delete button / inline confirm -->
            <template v-if="deletingId === r.id">
              <span class="muted" style="font-size:.78rem">Delete?</span>
              <button
                class="btn btn-sm"
                style="padding:4px 10px;font-size:.78rem;background:var(--red);color:#fff;border:none"
                :disabled="deleteLoading"
                @click.stop="confirmDelete(r)"
              >{{ deleteLoading ? '…' : 'Yes' }}</button>
              <button
                class="btn btn-sm btn-secondary"
                style="padding:4px 10px;font-size:.78rem"
                @click.stop="cancelDelete"
              >No</button>
            </template>
            <button
              v-else
              class="btn btn-sm"
              style="padding:4px 10px;font-size:.78rem;color:var(--red);background:transparent;border:1px solid var(--red)"
              @click.stop="promptDelete(r)"
            >Delete</button>

            <span>{{ expanded === r.id ? '▲' : '▼' }}</span>
          </div>
        </div>

        <template v-if="expanded === r.id">
          <div v-if="r.criteria.length === 0" class="empty" style="padding:20px">
            <p>No criteria defined.</p>
          </div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr><th>Criterion</th><th>Description</th><th>Max Score</th></tr>
              </thead>
              <tbody>
                <tr v-for="c in r.criteria" :key="c.id">
                  <td><strong>{{ c.name }}</strong></td>
                  <td class="muted">{{ c.description }}</td>
                  <td><span class="badge badge-purple">{{ c.maxScore }} pts</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </div>

    <!-- Create / Edit Rubric Modal -->
    <div v-if="showModal" class="overlay" @click.self="closeModal">
      <div class="modal" style="max-width:620px;width:100%">

        <!-- Step 1: Build Rubric -->
        <template v-if="step === 1">
          <div class="modal-head">
            <h3>{{ modalTitle }}</h3>
            <button class="modal-close" @click="closeModal">×</button>
          </div>

          <div class="modal-body">
            <div v-if="formError" class="alert alert-error" style="margin-bottom:12px">
              {{ formError }}
            </div>

            <div class="form-group">
              <label>Rubric Name <span style="color:var(--red)">*</span></label>
              <input
                v-model="form.name"
                placeholder="e.g. Peer Eval Rubric v1"
                @keyup.enter="goToPreview"
              />
            </div>

            <div style="margin-top:16px">
              <div class="flex items-center gap-2" style="margin-bottom:8px">
                <label style="font-weight:600;font-size:.85rem">Criteria</label>
                <span class="muted" style="font-size:.78rem">(at least one required)</span>
              </div>

              <div
                v-for="(c, i) in form.criteria"
                :key="i"
                class="card"
                style="padding:12px;margin-bottom:8px;background:var(--surface-2)"
              >
                <div style="display:flex;gap:8px;align-items:flex-start">
                  <div style="flex:1">
                    <div class="form-group" style="margin-bottom:8px">
                      <label>Name <span style="color:var(--red)">*</span></label>
                      <input v-model="c.name" placeholder="e.g. Quality of work" />
                    </div>
                    <div class="form-group" style="margin-bottom:8px">
                      <label>Description</label>
                      <input v-model="c.description" placeholder="What does this measure?" />
                    </div>
                    <div class="form-group">
                      <label>Max Score <span style="color:var(--red)">*</span></label>
                      <input
                        type="number"
                        v-model.number="c.maxScore"
                        min="0.01"
                        step="0.5"
                        style="max-width:100px"
                      />
                    </div>
                  </div>
                  <button
                    v-if="form.criteria.length > 1"
                    class="btn btn-sm"
                    style="color:var(--red);background:transparent;border:1px solid var(--red);padding:4px 8px"
                    @click="removeCriterionRow(i)"
                    title="Remove criterion"
                  >×</button>
                </div>
              </div>

              <button class="btn btn-secondary btn-sm" @click="addCriterionRow">
                + Add Criterion
              </button>
            </div>
          </div>

          <div class="modal-foot">
            <button class="btn btn-secondary" @click="closeModal">Cancel</button>
            <button class="btn btn-primary" @click="goToPreview">Next: Preview →</button>
          </div>
        </template>

        <!-- Step 2: Preview & Confirm -->
        <template v-else-if="step === 2">
          <div class="modal-head">
            <h3>{{ editingId !== null ? 'Confirm Changes' : 'Confirm Rubric' }}</h3>
            <button class="modal-close" @click="closeModal">×</button>
          </div>

          <div class="modal-body">
            <div v-if="formError" class="alert alert-error" style="margin-bottom:12px">
              {{ formError }}
            </div>

            <p class="muted" style="margin-bottom:16px;font-size:.85rem">
              Review the rubric below. Click <strong>{{ confirmLabel }}</strong> to save it.
            </p>

            <div style="margin-bottom:12px">
              <span class="muted" style="font-size:.78rem;text-transform:uppercase;font-weight:700;letter-spacing:.06em">Rubric Name</span>
              <p style="font-size:1rem;font-weight:600;margin-top:2px">{{ form.name }}</p>
            </div>

            <div class="table-wrap">
              <table>
                <thead>
                  <tr><th>#</th><th>Criterion</th><th>Description</th><th>Max Score</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(c, i) in form.criteria" :key="i">
                    <td class="muted">{{ i + 1 }}</td>
                    <td><strong>{{ c.name }}</strong></td>
                    <td class="muted">{{ c.description || '—' }}</td>
                    <td><span class="badge badge-purple">{{ c.maxScore }} pts</span></td>
                  </tr>
                  <tr style="font-weight:700;background:var(--surface-2)">
                    <td colspan="3" style="text-align:right">Total</td>
                    <td><span class="badge badge-purple">{{ previewTotal }} pts</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="modal-foot">
            <button class="btn btn-secondary" @click="step = 1">← Back</button>
            <button class="btn btn-primary" :disabled="saving" @click="confirmSave">
              {{ confirmLabel }}
            </button>
          </div>
        </template>

      </div>
    </div>

  </AppLayout>
</template>
