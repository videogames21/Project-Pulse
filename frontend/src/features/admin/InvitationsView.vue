<script setup>
import { ref } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { MOCK_INVITATIONS, MOCK_TEAMS } from '../../data/mockData'

const invitations = ref([...MOCK_INVITATIONS])
const showModal   = ref(false)
const form        = ref({ email: '', role: 'student', team: '' })
const flash       = ref('')

const STATUS_CLS = { pending: 'badge-orange', accepted: 'badge-green', expired: 'badge-gray' }

function send() {
  if (!form.value.email.trim()) return
  const id = Math.max(0, ...invitations.value.map(i => i.id)) + 1
  invitations.value.push({ id, ...form.value, sentAt: new Date().toISOString().split('T')[0], status: 'pending' })
  const email = form.value.email
  form.value = { email: '', role: 'student', team: '' }
  showModal.value = false
  flash.value = `Invitation sent to ${email}.`; setTimeout(() => flash.value = '', 3000)
}
</script>

<template>
  <AppLayout>
    <div v-if="flash" class="alert alert-success">{{ flash }}</div>

    <div class="flex justify-between items-center mb-4">
      <p class="muted">Invite students and instructors via unique registration email links.</p>
      <button class="btn btn-primary" @click="showModal = true">+ Send Invitation</button>
    </div>

    <div class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead><tr><th>Email</th><th>Role</th><th>Team</th><th>Sent</th><th>Status</th></tr></thead>
          <tbody>
            <tr v-for="inv in invitations" :key="inv.id">
              <td>{{ inv.email }}</td>
              <td><span class="badge badge-purple" style="text-transform:capitalize">{{ inv.role }}</span></td>
              <td>{{ inv.team || '—' }}</td>
              <td>{{ inv.sentAt }}</td>
              <td><span :class="['badge', STATUS_CLS[inv.status]]" style="text-transform:capitalize">{{ inv.status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showModal" class="overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-head"><h3>Send Invitation</h3><button class="modal-close" @click="showModal = false">×</button></div>
        <div class="modal-body">
          <div class="form-group"><label>Email Address</label><input type="email" v-model="form.email" placeholder="name@tcu.edu" /></div>
          <div class="form-group">
            <label>Role</label>
            <select v-model="form.role">
              <option value="student">Student</option>
              <option value="instructor">Instructor</option>
            </select>
          </div>
          <div class="form-group">
            <label>Assign to Team (optional)</label>
            <select v-model="form.team">
              <option value="">— Select a team —</option>
              <option v-for="t in MOCK_TEAMS" :key="t.id" :value="t.name">{{ t.name }}</option>
            </select>
          </div>
          <div class="alert alert-info" style="margin-top:4px">A unique registration link will be emailed via Gmail.</div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
          <button class="btn btn-primary" @click="send">Send Invite</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
