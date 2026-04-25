<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { usersApi } from '../../api/users.js'
import { invitationsApi } from '../../api/invitations.js'

const router      = useRouter()
const instructors = ref([])
const loading     = ref(false)
const error       = ref('')
const filterName  = ref('')

// Invite modal
const showInvite      = ref(false)
const inviteEmails    = ref('')
const inviteError     = ref('')
const inviting        = ref(false)
const inviteResults   = ref([])
const showInviteLinks = ref(false)

async function fetchInstructors() {
  loading.value = true
  error.value   = ''
  try {
    const res = await usersApi.getInstructors(filterName.value)
    instructors.value = res.data
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function clearFilters() {
  filterName.value = ''
  fetchInstructors()
}

function openInvite() {
  inviteEmails.value    = ''
  inviteError.value     = ''
  inviteResults.value   = []
  showInviteLinks.value = false
  showInvite.value      = true
}

async function sendInvitations() {
  inviteError.value = ''
  if (!inviteEmails.value.trim()) {
    inviteError.value = 'Please enter at least one email address.'
    return
  }
  inviting.value = true
  try {
    const res = await invitationsApi.inviteInstructors(inviteEmails.value)
    inviteResults.value   = res.data
    showInviteLinks.value = true
  } catch (e) {
    inviteError.value = e.message
  } finally {
    inviting.value = false
  }
}

onMounted(fetchInstructors)
</script>

<template>
  <AppLayout>
    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div class="flex justify-between items-center mb-4" style="margin-bottom:16px">
      <p class="muted">View and manage instructors in the system.</p>
      <button class="btn btn-primary btn-sm" @click="openInvite">+ Invite Instructors</button>
    </div>

    <!-- Filters -->
    <div class="card mb-4" style="margin-bottom:16px">
      <div class="flex gap-3 items-center" style="flex-wrap:wrap">
        <div class="form-group" style="margin-bottom:0;flex:1;min-width:160px">
          <label>Name</label>
          <input v-model="filterName" placeholder="Search by name..." @keyup.enter="fetchInstructors" />
        </div>
        <div style="display:flex;gap:8px;align-self:flex-end;padding-bottom:1px">
          <button class="btn btn-primary" @click="fetchInstructors" :disabled="loading">Search</button>
          <button class="btn btn-secondary" @click="clearFilters">Clear</button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Status</th>
              <th>Assigned Team</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="4" class="empty">Loading...</td>
            </tr>
            <tr v-else-if="instructors.length === 0">
              <td colspan="4" class="empty">No instructors found.</td>
            </tr>
            <tr
              v-for="i in instructors"
              :key="i.id"
              style="cursor:pointer"
              @click="router.push(`/admin/instructors/${i.id}`)"
            >
              <td><strong>{{ i.firstName }} {{ i.lastName }}</strong></td>
              <td class="muted">{{ i.email }}</td>
              <td>
                <span
                  class="badge"
                  :style="i.status === 'ACTIVE'
                    ? 'background:var(--green,#16a34a);color:#fff'
                    : 'background:#6b7280;color:#fff'"
                >
                  {{ i.status === 'ACTIVE' ? 'Active' : 'Deactivated' }}
                </span>
              </td>
              <td>
                <span v-if="i.teamName">{{ i.teamName }}</span>
                <span v-else class="muted">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Invite Modal -->
    <div v-if="showInvite" class="overlay" @click.self="showInvite = false">
      <div class="modal">
        <div class="modal-head">
          <h3>Invite Instructors</h3>
          <button class="modal-close" @click="showInvite = false">×</button>
        </div>

        <!-- Input step -->
        <div v-if="!showInviteLinks" class="modal-body">
          <div v-if="inviteError" class="alert alert-error" style="margin-bottom:12px">{{ inviteError }}</div>
          <div class="form-group" style="margin-bottom:0">
            <label class="form-label">Email Addresses</label>
            <textarea
              v-model="inviteEmails"
              class="form-input"
              rows="4"
              placeholder="smith@tcu.edu; jones@tcu.edu; lee@tcu.edu"
              style="resize:vertical"
            />
            <p class="muted" style="font-size:.8rem;margin-top:4px">Separate multiple emails with semicolons.</p>
          </div>
        </div>

        <!-- Results step -->
        <div v-else class="modal-body">
          <p style="margin-bottom:12px;color:#16a34a;font-weight:600">
            {{ inviteResults.length }} invitation(s) sent successfully.
          </p>
          <p class="muted" style="font-size:.85rem;margin-bottom:10px">
            Share these registration links with the instructors:
          </p>
          <div v-for="r in inviteResults" :key="r.id" style="margin-bottom:10px;padding:10px;background:var(--surface-alt, #f9fafb);border-radius:6px;border:1px solid var(--border)">
            <p style="font-size:.85rem;font-weight:600">{{ r.email }}</p>
            <p style="font-size:.8rem;word-break:break-all;color:var(--purple);margin-top:4px">{{ r.registrationLink }}</p>
          </div>
        </div>

        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showInvite = false">Close</button>
          <button
            v-if="!showInviteLinks"
            class="btn btn-primary"
            :disabled="inviting"
            @click="sendInvitations"
          >
            {{ inviting ? 'Sending…' : 'Send Invitations' }}
          </button>
        </div>
      </div>
    </div>

  </AppLayout>
</template>
