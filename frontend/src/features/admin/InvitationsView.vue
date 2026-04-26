<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { invitationsApi } from '../../api/invitations.js'
import { sectionsApi } from '../../api/sections.js'

const invitations      = ref([])
const sections         = ref([])
const flash            = ref({ type: '', text: '' })
const viewingToken     = ref(null)
const expandedUsers    = ref(null)
const confirmingDelete = ref(null)
const copiedToken      = ref(null)
const viewingCode      = ref(null)

const generatingStudent    = ref(false)
const generatingInstructor = ref(false)

// Section picker modal for student link generation
const sectionPickerModal  = ref(false)
const selectedSectionId   = ref(null)
const sectionPickerError  = ref('')

// Modal for newly generated instructor invitation
const newInstructorModal = ref(null)  // { link, code } or null

const STATUS_CLS = {
  ACTIVE:   'badge-green',
  DISABLED: 'badge-gray',
  ACCEPTED: 'badge-blue',
}

const studentLinks    = computed(() => invitations.value.filter(i => i.role === 'STUDENT'))
const instructorLinks = computed(() => invitations.value.filter(i => i.role === 'INSTRUCTOR'))

async function loadInvitations() {
  try {
    const res = await invitationsApi.getAll()
    invitations.value = res.data ?? []
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to load invitations.' }
  }
}

async function loadSections() {
  try {
    const res = await sectionsApi.getAll()
    sections.value = res.data ?? []
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to load sections.' }
  }
}

function openSectionPicker() {
  selectedSectionId.value = null
  sectionPickerError.value = ''
  sectionPickerModal.value = true
}

async function generateStudent() {
  if (!selectedSectionId.value) {
    sectionPickerError.value = 'Please select a section.'
    return
  }
  sectionPickerModal.value = false
  generatingStudent.value = true
  try {
    await invitationsApi.generate(selectedSectionId.value)
    await loadInvitations()
    flash.value = { type: 'alert-success', text: 'Student invitation link generated.' }
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to generate link.' }
  } finally {
    generatingStudent.value = false
  }
}

async function generateInstructor() {
  generatingInstructor.value = true
  try {
    const res = await invitationsApi.generateInstructorLink()
    await loadInvitations()
    newInstructorModal.value = {
      link: res.data.registrationLink,
      code: res.data.accessCode,
    }
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to generate instructor link.' }
  } finally {
    generatingInstructor.value = false
  }
}

function toggleLink(id)  { viewingToken.value  = viewingToken.value  === id ? null : id }
function toggleUsers(id) { expandedUsers.value = expandedUsers.value === id ? null : id }
function toggleCode(id)  { viewingCode.value   = viewingCode.value   === id ? null : id }

async function copyLink(link, id) {
  await navigator.clipboard.writeText(link)
  copiedToken.value = id
  setTimeout(() => { copiedToken.value = null }, 2000)
}

async function disableLink(token) {
  try {
    await invitationsApi.disable(token)
    await loadInvitations()
    flash.value = { type: 'alert-success', text: 'Invitation link disabled.' }
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to disable invitation link.' }
  }
}

async function enableLink(token) {
  try {
    await invitationsApi.enable(token)
    await loadInvitations()
    flash.value = { type: 'alert-success', text: 'Invitation link re-enabled.' }
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to re-enable invitation link.' }
  }
}

function promptDelete(id) { confirmingDelete.value = id }
function cancelDelete()   { confirmingDelete.value = null }

async function confirmDelete(token, id) {
  confirmingDelete.value = null
  try {
    await invitationsApi.remove(token)
    await loadInvitations()
    flash.value = { type: 'alert-success', text: 'Invitation link permanently deleted.' }
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to delete invitation link.' }
  }
}

onMounted(() => {
  loadInvitations()
  loadSections()
})
</script>

<template>
  <AppLayout>
    <div v-if="flash.text" :class="['alert', flash.type]" style="margin-bottom:12px">
      {{ flash.text }}
      <button class="btn-close" @click="flash.text = ''" style="float:right;background:none;border:none;cursor:pointer;font-size:1rem">×</button>
    </div>

    <!-- ── Student Links ─────────────────────────────────────────────────── -->
    <div style="margin-bottom:32px">
      <div class="flex justify-between items-center" style="margin-bottom:12px">
        <h3 style="margin:0;font-size:1.05rem;font-weight:700;color:#4D1979">Student Links</h3>
        <button class="btn btn-primary btn-sm" :disabled="generatingStudent" @click="openSectionPicker">
          <template v-if="generatingStudent">Generating…</template>
          <template v-else>+ Generate Student Link</template>
        </button>
      </div>
      <p class="muted" style="margin-bottom:12px;font-size:.85rem">Reusable links — any student can register while the link is active.</p>

      <div class="card" style="padding:0;overflow:hidden">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Link ID</th>
                <th>Section</th>
                <th>Full Link</th>
                <th>Generated</th>
                <th>Used By</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="studentLinks.length === 0">
                <td colspan="7" style="text-align:center;color:var(--text-muted)">No student links yet.</td>
              </tr>
              <template v-for="inv in studentLinks" :key="inv.id">
                <tr>
                  <td><code>{{ inv.tokenShort }}…</code></td>
                  <td>
                    <span v-if="inv.sectionName">{{ inv.sectionName }}</span>
                    <span v-else class="muted">—</span>
                  </td>
                  <td>
                    <button class="btn btn-secondary btn-sm" @click="toggleLink(inv.id)">
                      {{ viewingToken === inv.id ? 'Hide' : 'View' }}
                    </button>
                  </td>
                  <td>{{ inv.createdAt?.slice(0, 10) }}</td>
                  <td>
                    <span>{{ inv.usageCount }} student{{ inv.usageCount !== 1 ? 's' : '' }}</span>
                    <button
                      v-if="inv.usageCount > 0"
                      class="btn-link"
                      @click="toggleUsers(inv.id)"
                      style="margin-left:6px;font-size:0.8rem"
                    >
                      {{ expandedUsers === inv.id ? '▴ Hide' : '▾ Names' }}
                    </button>
                  </td>
                  <td>
                    <span :class="['badge', STATUS_CLS[inv.status] ?? 'badge-gray']">
                      {{ inv.status?.toLowerCase() }}
                    </span>
                  </td>
                  <td>
                    <template v-if="inv.status === 'ACTIVE'">
                      <button class="btn btn-secondary btn-sm" @click="disableLink(inv.token)">Disable</button>
                    </template>
                    <template v-else-if="inv.status === 'DISABLED'">
                      <button class="btn btn-secondary btn-sm" @click="enableLink(inv.token)" style="margin-right:6px">Re-enable</button>
                      <template v-if="confirmingDelete === inv.id">
                        <span style="font-size:0.8rem;color:var(--text-muted);margin-right:4px">Delete?</span>
                        <button class="btn btn-danger btn-sm" @click="confirmDelete(inv.token, inv.id)" style="margin-right:4px">Yes</button>
                        <button class="btn btn-secondary btn-sm" @click="cancelDelete">No</button>
                      </template>
                      <button v-else class="btn btn-danger btn-sm" @click="promptDelete(inv.id)">Delete</button>
                    </template>
                  </td>
                </tr>
                <tr v-if="viewingToken === inv.id">
                  <td colspan="7" style="background:var(--bg-secondary,#f8f8f8);padding:8px 16px">
                    <div class="flex items-center" style="gap:8px">
                      <input type="text" :value="inv.registrationLink" readonly style="flex:1;padding:5px 10px;border:1px solid var(--border);border-radius:6px;font-size:0.82rem;background:var(--bg)" />
                      <button class="btn btn-secondary btn-sm" @click="copyLink(inv.registrationLink, inv.id)">
                        {{ copiedToken === inv.id ? 'Copied!' : 'Copy' }}
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="expandedUsers === inv.id && inv.usageCount > 0">
                  <td colspan="7" style="background:var(--bg-secondary,#f8f8f8);padding:8px 16px">
                    <ul style="margin:0;padding-left:20px;font-size:0.85rem">
                      <li v-for="u in inv.acceptedUsers" :key="u.email">
                        {{ u.name }} — <span style="color:var(--text-muted)">{{ u.email }}</span>
                      </li>
                    </ul>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ── Instructor Links ──────────────────────────────────────────────── -->
    <div>
      <div class="flex justify-between items-center" style="margin-bottom:12px">
        <h3 style="margin:0;font-size:1.05rem;font-weight:700;color:#4D1979">Instructor Links</h3>
        <button class="btn btn-primary btn-sm" :disabled="generatingInstructor" @click="generateInstructor">
          <template v-if="generatingInstructor">Generating…</template>
          <template v-else>+ Generate Instructor Link</template>
        </button>
      </div>
      <p class="muted" style="margin-bottom:12px;font-size:.85rem">Single-use links — each link can only be used once. Comes with an access code for identity verification.</p>

      <div class="card" style="padding:0;overflow:hidden">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Link ID</th>
                <th>Full Link</th>
                <th>Generated</th>
                <th>Used By</th>
                <th>Access Code</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="instructorLinks.length === 0">
                <td colspan="7" style="text-align:center;color:var(--text-muted)">No instructor links yet.</td>
              </tr>
              <template v-for="inv in instructorLinks" :key="inv.id">
                <tr>
                  <td><code>{{ inv.tokenShort }}…</code></td>
                  <td>
                    <button class="btn btn-secondary btn-sm" @click="toggleLink(inv.id)">
                      {{ viewingToken === inv.id ? 'Hide' : 'View' }}
                    </button>
                  </td>
                  <td>{{ inv.createdAt?.slice(0, 10) }}</td>
                  <td>
                    <template v-if="inv.acceptedUsers?.length > 0">
                      <strong>{{ inv.acceptedUsers[0].name }}</strong>
                      <span class="muted" style="display:block;font-size:.8rem">{{ inv.acceptedUsers[0].email }}</span>
                    </template>
                    <span v-else class="muted">—</span>
                  </td>
                  <td>
                    <button class="btn-link" style="font-size:.82rem" @click="toggleCode(inv.id)">
                      {{ viewingCode === inv.id ? 'Hide' : 'View Access Code' }}
                    </button>
                    <code v-if="viewingCode === inv.id" style="display:block;margin-top:4px;font-size:.95rem;letter-spacing:.1em;color:#4D1979">{{ inv.accessCode }}</code>
                  </td>
                  <td>
                    <span :class="['badge', STATUS_CLS[inv.status] ?? 'badge-gray']">
                      {{ inv.status === 'ACCEPTED' ? 'Used' : inv.status?.toLowerCase() }}
                    </span>
                  </td>
                  <td>
                    <template v-if="inv.status === 'ACTIVE'">
                      <button class="btn btn-secondary btn-sm" @click="disableLink(inv.token)">Disable</button>
                    </template>
                    <template v-else-if="inv.status === 'DISABLED'">
                      <button class="btn btn-secondary btn-sm" @click="enableLink(inv.token)" style="margin-right:6px">Re-enable</button>
                      <template v-if="confirmingDelete === inv.id">
                        <span style="font-size:0.8rem;color:var(--text-muted);margin-right:4px">Delete?</span>
                        <button class="btn btn-danger btn-sm" @click="confirmDelete(inv.token, inv.id)" style="margin-right:4px">Yes</button>
                        <button class="btn btn-secondary btn-sm" @click="cancelDelete">No</button>
                      </template>
                      <button v-else class="btn btn-danger btn-sm" @click="promptDelete(inv.id)">Delete</button>
                    </template>
                    <template v-else-if="inv.status === 'ACCEPTED'">
                      <template v-if="confirmingDelete === inv.id">
                        <span style="font-size:0.8rem;color:var(--text-muted);margin-right:4px">Delete?</span>
                        <button class="btn btn-danger btn-sm" @click="confirmDelete(inv.token, inv.id)" style="margin-right:4px">Yes</button>
                        <button class="btn btn-secondary btn-sm" @click="cancelDelete">No</button>
                      </template>
                      <button v-else class="btn btn-danger btn-sm" @click="promptDelete(inv.id)">Delete</button>
                    </template>
                  </td>
                </tr>
                <tr v-if="viewingToken === inv.id">
                  <td colspan="7" style="background:var(--bg-secondary,#f8f8f8);padding:8px 16px">
                    <div class="flex items-center" style="gap:8px">
                      <input type="text" :value="inv.registrationLink" readonly style="flex:1;padding:5px 10px;border:1px solid var(--border);border-radius:6px;font-size:0.82rem;background:var(--bg)" />
                      <button class="btn btn-secondary btn-sm" @click="copyLink(inv.registrationLink, inv.id)">
                        {{ copiedToken === inv.id ? 'Copied!' : 'Copy' }}
                      </button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ── Section Picker Modal (for student links) ────────────────────── -->
    <div v-if="sectionPickerModal" class="overlay" @click.self="sectionPickerModal = false">
      <div class="modal">
        <div class="modal-head">
          <h3>Select Section for Student Link</h3>
          <button class="modal-close" @click="sectionPickerModal = false">×</button>
        </div>
        <div class="modal-body">
          <p style="margin-bottom:16px;font-size:.9rem;color:#5a5070">
            Choose the section this registration link will be for. Students who register with this link will automatically be enrolled in the selected section.
          </p>
          <div v-if="sectionPickerError" class="alert alert-warning" style="margin-bottom:12px">
            {{ sectionPickerError }}
          </div>
          <div v-if="sections.length === 0" style="color:var(--text-muted);font-size:.9rem">
            No sections found. Create a section first.
          </div>
          <div v-else style="display:flex;flex-direction:column;gap:8px">
            <label
              v-for="sec in sections"
              :key="sec.id"
              style="display:flex;align-items:center;gap:10px;padding:10px 12px;border:1.5px solid;border-radius:8px;cursor:pointer;transition:all .15s"
              :style="{
                borderColor: selectedSectionId === sec.id ? '#4D1979' : '#d8d3e3',
                background:  selectedSectionId === sec.id ? '#f3e8ff' : '#fff',
              }"
            >
              <input type="radio" :value="sec.id" v-model="selectedSectionId" style="accent-color:#4D1979" />
              <span style="font-weight:600;color:#4D1979">{{ sec.sectionName }}</span>
            </label>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="sectionPickerModal = false">Cancel</button>
          <button class="btn btn-primary" :disabled="!selectedSectionId || sections.length === 0" @click="generateStudent">
            Generate Link
          </button>
        </div>
      </div>
    </div>

    <!-- ── New Instructor Link Modal ─────────────────────────────────────── -->
    <div v-if="newInstructorModal" class="overlay" @click.self="newInstructorModal = null">
      <div class="modal">
        <div class="modal-head">
          <h3>New Instructor Invitation Created</h3>
          <button class="modal-close" @click="newInstructorModal = null">×</button>
        </div>
        <div class="modal-body">
          <p style="margin-bottom:16px;font-size:.9rem;color:#5a5070;line-height:1.55">
            Share both pieces of information with the instructor privately. Do <strong>not</strong> share the access code with anyone besides the intended recipient, and tell them to keep it confidential.
          </p>
          <div class="info-block">
            <span class="info-label">Registration Link</span>
            <div class="flex items-center" style="gap:8px;margin-top:4px">
              <input type="text" :value="newInstructorModal.link" readonly class="link-input" />
            </div>
          </div>
          <div class="info-block" style="margin-top:16px">
            <span class="info-label">Access Code</span>
            <code class="access-code-display">{{ newInstructorModal.code }}</code>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-primary" @click="newInstructorModal = null">Close</button>
        </div>
      </div>
    </div>

  </AppLayout>
</template>

<style scoped>
.btn-link {
  background: none;
  border: none;
  color: var(--primary, #6366f1);
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}
.btn-sm {
  padding: 4px 10px;
  font-size: 0.82rem;
}
.btn-danger {
  background: #dc2626;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.btn-danger:hover { background: #b91c1c; }
.badge-blue { background: #3b82f6; color: #fff; }
.overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 200;
}
.modal {
  background: #fff; border-radius: 14px; padding: 0;
  width: 100%; max-width: 500px;
  box-shadow: 0 20px 60px rgba(0,0,0,.25);
}
.modal-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #ede8f5;
}
.modal-head h3 { margin: 0; font-size: 1rem; font-weight: 700; color: #4D1979; }
.modal-close { background: none; border: none; font-size: 1.3rem; cursor: pointer; color: #888; }
.modal-body { padding: 20px 24px; }
.modal-foot { padding: 12px 24px 20px; display: flex; justify-content: flex-end; gap: 8px; border-top: 1px solid #ede8f5; }
.info-block {}
.info-label { font-size: .75rem; font-weight: 700; text-transform: uppercase; letter-spacing: .05em; color: #9e9aaa; }
.link-input {
  flex: 1; width: 100%; padding: 8px 10px;
  border: 1.5px solid #d8d3e3; border-radius: 8px;
  font-size: .82rem; background: #f9f7ff;
  box-sizing: border-box;
}
.access-code-display {
  display: block; margin-top: 6px;
  font-size: 1.6rem; font-weight: 700;
  letter-spacing: .18em; color: #4D1979;
  background: #f3e8ff; border-radius: 8px;
  padding: 10px 16px; text-align: center;
}
</style>
