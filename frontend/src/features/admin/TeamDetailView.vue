<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { api } from '../../services/api.js'

const route  = useRoute()
const router = useRouter()
const team   = ref(null)
const error  = ref('')

onMounted(async () => {
  try {
    const res = await api.get(`/api/v1/teams/${route.params.id}`)
    team.value = res.data
  } catch (e) {
    error.value = e.message
  }
})
</script>

<template>
  <AppLayout>
    <div class="flex items-center gap-2 mb-4" style="margin-bottom:16px">
      <button class="btn btn-secondary btn-sm" @click="router.back()">← Back</button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="!team" class="empty">Loading...</div>

    <div v-else class="card">
      <div class="card-header">
        <h2 style="font-size:1.1rem;font-weight:700">{{ team.name }}</h2>
        <span class="badge badge-purple">{{ team.sectionName }}</span>
      </div>

      <div class="grid-2" style="gap:18px">
        <div>
          <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Description</p>
          <p>{{ team.description ?? '—' }}</p>
        </div>
        <div>
          <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Website</p>
          <a v-if="team.websiteUrl" :href="team.websiteUrl" target="_blank" style="color:var(--purple)">{{ team.websiteUrl }}</a>
          <p v-else class="muted">—</p>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
