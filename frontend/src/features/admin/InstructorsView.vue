<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { usersApi } from '../../api/users.js'

const instructors = ref([])
const loading     = ref(false)
const error       = ref('')
const filterName  = ref('')

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

onMounted(fetchInstructors)
</script>

<template>
  <AppLayout>
    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div class="flex justify-between items-center mb-4">
      <p class="muted">View and manage instructors in the system.</p>
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
              <th>Assigned Team</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="3" class="empty">Loading...</td>
            </tr>
            <tr v-else-if="instructors.length === 0">
              <td colspan="3" class="empty">No instructors found.</td>
            </tr>
            <tr v-for="i in instructors" :key="i.id">
              <td><strong>{{ i.name }}</strong></td>
              <td class="muted">{{ i.email }}</td>
              <td>
                <span v-if="i.teamId" >Team #{{ i.teamId }}</span>
                <span v-else class="muted">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AppLayout>
</template>
