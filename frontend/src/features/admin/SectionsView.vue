<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { sectionsApi } from '../../api/sections.js'

const router = useRouter()

const sections = ref([])
const search   = ref('')
const loading  = ref(false)
const error    = ref('')

async function fetchSections() {
  loading.value = true
  error.value   = ''
  try {
    const res    = await sectionsApi.getAll(search.value.trim() || undefined)
    sections.value = res.data
  } catch {
    error.value = 'Failed to load sections.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchSections)
</script>

<template>
  <AppLayout>
    <div class="flex justify-between items-center mb-4">
      <p class="muted">Manage Senior Design sections (one per academic year).</p>
      <button class="btn btn-primary" @click="router.push('/admin/sections/create')">+ Create Section</button>
    </div>

    <div class="flex gap-2 mb-4">
      <input
        v-model="search"
        placeholder="Search by section name…"
        style="flex:1"
        @keyup.enter="fetchSections"
      />
      <button class="btn btn-primary" @click="fetchSections">Search</button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div v-if="loading" class="muted">Loading…</div>

    <div v-else-if="sections.length === 0 && !error" class="card" style="padding: 1.5rem; text-align:center;">
      <p class="muted">No sections found.</p>
    </div>

    <div v-else class="card" style="padding:0; overflow:hidden">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Section Name</th>
              <th>Start Date</th>
              <th>End Date</th>
              <th>Teams</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="s in sections"
              :key="s.id"
              style="cursor:pointer"
              @click="router.push(`/admin/sections/${s.id}`)"
            >
              <td><strong>{{ s.sectionName }}</strong></td>
              <td>{{ s.startDate ?? '—' }}</td>
              <td>{{ s.endDate ?? '—' }}</td>
              <td>
                <span v-if="s.teamNames.length === 0" class="muted">No teams</span>
                <span v-else>{{ s.teamNames.join(', ') }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AppLayout>
</template>
