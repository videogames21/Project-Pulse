<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { invitationsApi } from '../../api/invitations.js'

const invitations    = ref([])
const loading        = ref(false)
const flash          = ref({ type: '', text: '' })
const viewingToken   = ref(null)   // which row's full link is shown
const expandedUsers  = ref(null)   // which row's accepted-users list is open
const confirmingDelete = ref(null) // which token is pending delete confirmation
const copiedToken    = ref(null)   // which token just had its link copied

const STATUS_CLS = { ACTIVE: 'badge-green', DISABLED: 'badge-gray' }

async function loadInvitations() {
  try {
    const res = await invitationsApi.getAll()
    invitations.value = res.data ?? []
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to load invitations.' }
  }
}

async function generate() {
  loading.value = true
  try {
    await invitationsApi.generate()
    await loadInvitations()
    flash.value = { type: 'alert-success', text: 'Invitation link generated successfully.' }
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to generate link. Please try again.' }
  } finally {
    loading.value = false
  }
}

function toggleLink(token) {
  viewingToken.value = viewingToken.value === token ? null : token
}

function toggleUsers(token) {
  expandedUsers.value = expandedUsers.value === token ? null : token
}

async function copyLink(link, token) {
  await navigator.clipboard.writeText(link)
  copiedToken.value = token
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

function promptDelete(token) {
  confirmingDelete.value = token
}

function cancelDelete() {
  confirmingDelete.value = null
}

async function confirmDelete(token) {
  confirmingDelete.value = null
  try {
    await invitationsApi.remove(token)
    await loadInvitations()
    flash.value = { type: 'alert-success', text: 'Invitation link permanently deleted.' }
  } catch {
    flash.value = { type: 'alert-warning', text: 'Failed to delete invitation link.' }
  }
}

onMounted(loadInvitations)
</script>

<template>
  <AppLayout>
    <div v-if="flash.text" :class="['alert', flash.type]" style="margin-bottom:12px">
      {{ flash.text }}
      <button class="btn-close" @click="flash.text = ''" style="float:right;background:none;border:none;cursor:pointer;font-size:1rem">×</button>
    </div>

    <div class="flex justify-between items-center mb-4">
      <p class="muted">Generate invitation links and share them with students. Links can be used multiple times while active.</p>
      <button class="btn btn-primary" :disabled="loading" @click="generate">
        {{ loading ? 'Generating…' : 'Generate Link' }}
      </button>
    </div>

    <div class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Link ID</th>
              <th>Full Link</th>
              <th>Generated</th>
              <th>Used By</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="invitations.length === 0">
              <td colspan="6" style="text-align:center;color:var(--text-muted)">No invitation links yet.</td>
            </tr>
            <template v-for="inv in invitations" :key="inv.id">
              <tr>
                <td><code>{{ inv.tokenShort }}…</code></td>

                <!-- Full Link toggle -->
                <td>
                  <button class="btn btn-secondary btn-sm" @click="toggleLink(inv.id)">
                    {{ viewingToken === inv.id ? 'Hide' : 'View' }}
                  </button>
                </td>

                <td>{{ inv.createdAt?.slice(0, 10) }}</td>

                <!-- Used By -->
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

                <!-- Status -->
                <td>
                  <span :class="['badge', STATUS_CLS[inv.status] ?? 'badge-gray']" style="text-transform:capitalize">
                    {{ inv.status?.toLowerCase() }}
                  </span>
                </td>

                <!-- Actions -->
                <td>
                  <template v-if="inv.status === 'ACTIVE'">
                    <button class="btn btn-secondary btn-sm" @click="disableLink(inv.token)">Disable</button>
                  </template>
                  <template v-else-if="inv.status === 'DISABLED'">
                    <button class="btn btn-secondary btn-sm" @click="enableLink(inv.token)" style="margin-right:6px">Re-enable</button>
                    <template v-if="confirmingDelete === inv.id">
                      <span style="font-size:0.8rem;color:var(--text-muted);margin-right:4px">Permanently delete?</span>
                      <button class="btn btn-danger btn-sm" @click="confirmDelete(inv.token)" style="margin-right:4px">Yes, Delete</button>
                      <button class="btn btn-secondary btn-sm" @click="cancelDelete">Cancel</button>
                    </template>
                    <button v-else class="btn btn-danger btn-sm" @click="promptDelete(inv.id)">Delete</button>
                  </template>
                </td>
              </tr>

              <!-- Full link expansion row -->
              <tr v-if="viewingToken === inv.id">
                <td colspan="6" style="background:var(--bg-secondary,#f8f8f8);padding:8px 16px">
                  <div class="flex items-center" style="gap:8px">
                    <input
                      type="text"
                      :value="inv.registrationLink"
                      readonly
                      style="flex:1;padding:5px 10px;border:1px solid var(--border);border-radius:6px;font-size:0.82rem;background:var(--bg)"
                    />
                    <button class="btn btn-secondary btn-sm" @click="copyLink(inv.registrationLink, inv.id)">
                      {{ copiedToken === inv.id ? 'Copied!' : 'Copy' }}
                    </button>
                  </div>
                </td>
              </tr>

              <!-- Accepted users expansion row -->
              <tr v-if="expandedUsers === inv.id && inv.usageCount > 0">
                <td colspan="6" style="background:var(--bg-secondary,#f8f8f8);padding:8px 16px">
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
.btn-danger:hover {
  background: #b91c1c;
}
</style>
