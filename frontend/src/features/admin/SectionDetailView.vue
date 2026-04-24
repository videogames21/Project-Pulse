<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { sectionsApi } from '../../api/sections.js'

const route   = useRoute()
const router  = useRouter()
const section = ref(null)
const error   = ref('')

onMounted(async () => {
  try {
    const res = await sectionsApi.getById(route.params.id)
    section.value = res.data
  } catch (e) {
    if (e.status === 404) {
      error.value = 'Section not found.'
    } else if (e.status === 400) {
      error.value = 'Invalid section ID.'
    } else {
      error.value = 'Failed to load section. Please try again.'
    }
  }
})
</script>

<template>
  <AppLayout>
    <div class="flex items-center gap-2 mb-4" style="margin-bottom:16px">
      <button class="btn btn-secondary btn-sm" @click="router.push('/admin/sections')">← Back</button>
      <button
        v-if="section"
        class="btn btn-primary btn-sm"
        @click="router.push(`/admin/sections/${route.params.id}/edit`)"
      >Edit Section</button>
      <button
        v-if="section"
        class="btn btn-secondary btn-sm"
        @click="router.push(`/admin/sections/${route.params.id}/active-weeks`)"
      >Active Weeks</button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="!section" class="muted">Loading…</div>

    <template v-else>
      <!-- Header card -->
      <div class="card" style="margin-bottom:16px">
        <div class="card-header">
          <h2 style="font-size:1.1rem;font-weight:700">{{ section.sectionName }}</h2>
        </div>
        <div class="grid-2" style="gap:18px">
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Start Date</p>
            <p>{{ section.startDate ?? '—' }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">End Date</p>
            <p>{{ section.endDate ?? '—' }}</p>
          </div>
          <div>
            <p class="muted" style="margin-bottom:4px;font-size:.75rem;font-weight:600;text-transform:uppercase">Rubric</p>
            <p>{{ section.rubricName ?? '—' }}</p>
          </div>
        </div>
      </div>

      <!-- Teams -->
      <div class="card" style="margin-bottom:16px;padding:0;overflow:hidden">
        <div class="card-header" style="padding:12px 16px">
          <h3 style="font-size:.95rem;font-weight:700">Teams</h3>
        </div>
        <div v-if="(section.teams ?? []).length === 0" style="padding:16px">
          <p class="muted">No teams assigned to this section.</p>
        </div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Team Name</th>
                <th>Students</th>
                <th>Instructors</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="t in (section.teams ?? [])"
                :key="t.id"
                style="cursor:pointer"
                @click="router.push(`/admin/teams/${t.id}`)"
              >
                <td>{{ t.name }}</td>
                <td>
                  <span v-if="(t.students ?? []).length === 0" class="muted">—</span>
                  <span v-else>{{ (t.students ?? []).join(', ') }}</span>
                </td>
                <td>
                  <span v-if="(t.instructors ?? []).length === 0" class="muted">—</span>
                  <span v-else>{{ (t.instructors ?? []).join(', ') }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Instructors not on a team -->
      <div class="card" style="margin-bottom:16px;padding:0;overflow:hidden">
        <div class="card-header" style="padding:12px 16px">
          <h3 style="font-size:.95rem;font-weight:700">Instructors Not Assigned to a Team</h3>
        </div>
        <div style="padding:16px">
          <p v-if="(section.instructorsNotOnTeam ?? []).length === 0" class="muted">None.</p>
          <ul v-else style="margin:0;padding-left:1.25rem">
            <li v-for="name in (section.instructorsNotOnTeam ?? [])" :key="name">{{ name }}</li>
          </ul>
        </div>
      </div>

      <!-- Students not on a team -->
      <div class="card" style="padding:0;overflow:hidden">
        <div class="card-header" style="padding:12px 16px">
          <h3 style="font-size:.95rem;font-weight:700">Students Not Assigned to a Team</h3>
        </div>
        <div style="padding:16px">
          <p v-if="(section.studentsNotOnTeam ?? []).length === 0" class="muted">None.</p>
          <ul v-else style="margin:0;padding-left:1.25rem">
            <li v-for="name in (section.studentsNotOnTeam ?? [])" :key="name">{{ name }}</li>
          </ul>
        </div>
      </div>
    </template>
  </AppLayout>
</template>
