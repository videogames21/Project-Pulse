<script setup>
import { ref } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { MOCK_SECTIONS } from '../../data/mockData'

const sections  = ref([...MOCK_SECTIONS])
const showModal = ref(false)
const form      = ref({ name: '', startDate: '', endDate: '' })
const flash     = ref('')

function create() {
  if (!form.value.name.trim()) return
  const id = Math.max(0, ...sections.value.map(s => s.id)) + 1
  sections.value.push({ id, ...form.value, rubricId: 1 })
  form.value = { name: '', startDate: '', endDate: '' }
  showModal.value = false
  flash.value = 'Section created.'; setTimeout(() => flash.value = '', 3000)
}
</script>

<template>
  <AppLayout>
    <div v-if="flash" class="alert alert-success">{{ flash }}</div>

    <div class="flex justify-between items-center mb-4">
      <p class="muted">Manage Senior Design sections (one per academic year).</p>
      <button class="btn btn-primary" @click="showModal = true">+ New Section</button>
    </div>

    <div class="card" style="padding:0;overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead><tr><th>Section Name</th><th>Start Date</th><th>End Date</th></tr></thead>
          <tbody>
            <tr v-for="s in sections" :key="s.id">
              <td><strong>{{ s.name }}</strong></td>
              <td>{{ s.startDate }}</td>
              <td>{{ s.endDate }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showModal" class="overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-head"><h3>Create Section</h3><button class="modal-close" @click="showModal = false">×</button></div>
        <div class="modal-body">
          <div class="form-group"><label>Section Name (YYYY-YYYY)</label><input v-model="form.name" placeholder="e.g. 2025-2026" /></div>
          <div class="grid-2">
            <div class="form-group"><label>Start Date</label><input type="date" v-model="form.startDate" /></div>
            <div class="form-group"><label>End Date</label><input type="date" v-model="form.endDate" /></div>
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
