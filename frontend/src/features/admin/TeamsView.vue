<script setup>
import { ref } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { MOCK_TEAMS, MOCK_SECTIONS } from '../../data/mockData'

const teams     = ref([...MOCK_TEAMS])
const showModal = ref(false)
const form      = ref({ name: '', description: '', website: '', sectionId: 1 })
const flash     = ref('')

const sectionName = (id) => MOCK_SECTIONS.find(s => s.id === id)?.name ?? '—'

function create() {
  if (!form.value.name.trim()) return
  const id = Math.max(0, ...teams.value.map(t => t.id)) + 1
  teams.value.push({ id, ...form.value, memberCount: 0 })
  form.value = { name: '', description: '', website: '', sectionId: 1 }
  showModal.value = false
  flash.value = 'Team created.'; setTimeout(() => flash.value = '', 3000)
}

function remove(id) {
  if (!confirm('Delete this team? All associated WARs and peer evaluations will also be deleted.')) return
  teams.value = teams.value.filter(t => t.id !== id)
}
</script>

<template>
  <AppLayout>
    <div v-if="flash" class="alert alert-success">{{ flash }}</div>

    <div class="flex justify-between items-center mb-4">
      <p class="muted">Create and manage Senior Design project teams.</p>
      <button class="btn btn-primary" @click="showModal = true">+ New Team</button>
    </div>

    <div class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Description</th><th>Section</th><th>Members</th><th></th></tr></thead>
          <tbody>
            <tr v-for="t in teams" :key="t.id">
              <td><strong>{{ t.name }}</strong></td>
              <td class="muted">{{ t.description }}</td>
              <td>{{ sectionName(t.sectionId) }}</td>
              <td>{{ t.memberCount }}</td>
              <td><button class="btn btn-danger btn-sm" @click="remove(t.id)">Delete</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showModal" class="overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-head"><h3>Create Team</h3><button class="modal-close" @click="showModal = false">×</button></div>
        <div class="modal-body">
          <div class="form-group"><label>Team Name</label><input v-model="form.name" placeholder="e.g. Team Delta" /></div>
          <div class="form-group"><label>Description</label><textarea v-model="form.description" placeholder="Brief project description" /></div>
          <div class="form-group"><label>Website URL</label><input v-model="form.website" placeholder="https://" /></div>
          <div class="form-group">
            <label>Section</label>
            <select v-model.number="form.sectionId">
              <option v-for="s in MOCK_SECTIONS" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
          <button class="btn btn-primary" @click="create">Create</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
