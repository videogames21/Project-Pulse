<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { usersApi } from '../../api/users.js'

const route      = useRoute()
const router     = useRouter()
const instructor = ref(null)
const error      = ref('')

onMounted(async () => {
  try {
    const res = await usersApi.getInstructorById(route.params.id)
    instructor.value = res.data
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

    <div v-else-if="!instructor" class="empty">Loading...</div>

    <template v-else>
      <!-- Header card -->
      <div class="card">
        <div class="card-header">
          <div>
            <h2 style="font-size:1.15rem;font-weight:700;margin:0">
              {{ instructor.firstName }} {{ instructor.lastName }}
            </h2>
            <p class="muted" style="margin:2px 0 0 0;font-size:.875rem">{{ instructor.email }}</p>
          </div>
          <span
            class="badge"
            style="margin-left:auto"
            :style="instructor.status === 'ACTIVE'
              ? 'background:var(--green,#16a34a);color:#fff'
              : 'background:#6b7280;color:#fff'"
          >
            {{ instructor.status === 'ACTIVE' ? 'Active' : 'Deactivated' }}
          </span>
        </div>

        <div class="grid-2" style="gap:18px;margin-top:12px">
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">First Name</p>
            <p>{{ instructor.firstName }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Last Name</p>
            <p>{{ instructor.lastName }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Email</p>
            <p>{{ instructor.email }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Status</p>
            <p>{{ instructor.status === 'ACTIVE' ? 'Active' : 'Deactivated' }}</p>
          </div>
        </div>
      </div>

      <!-- Supervised Teams -->
      <div class="card" style="margin-top:16px;padding:0;overflow:hidden">
        <div class="card-header" style="padding:12px 16px">
          <h3 style="font-size:.95rem;font-weight:700">Supervised Team</h3>
        </div>

        <div v-if="!instructor.supervisedTeam" style="padding:16px">
          <p class="muted">This instructor is not currently assigned to a team.</p>
        </div>

        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Team Name</th>
                <th>Section</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <strong
                    style="color:var(--purple);cursor:pointer"
                    @click="router.push(`/admin/teams/${instructor.supervisedTeam.teamId}`)"
                  >
                    {{ instructor.supervisedTeam.teamName }}
                  </strong>
                </td>
                <td class="muted">{{ instructor.supervisedTeam.sectionName }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </AppLayout>
</template>
