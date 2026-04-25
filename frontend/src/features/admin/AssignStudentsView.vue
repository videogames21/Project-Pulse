<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { teamsApi } from '../../api/teams.js'
import { usersApi } from '../../api/users.js'

const teams              = ref([])
const unassigned         = ref([])
const selectedTeamId     = ref(null)
const selectedStudentIds = ref([])
const loading            = ref(false)
const flash              = ref({ type: '', text: '' })
const showConfirm        = ref(false)

const selectedTeam = computed(() => teams.value.find(t => t.id === selectedTeamId.value))

async function load() {
  loading.value = true
  try {
    const [teamsRes, unassignedRes] = await Promise.all([
      teamsApi.getAll(),
      usersApi.getUnassignedStudents(),
    ])
    teams.value     = teamsRes.data     ?? []
    unassigned.value = unassignedRes.data ?? []
  } catch {
    flash.value = { type: 'alert-error', text: 'Failed to load data.' }
  } finally {
    loading.value = false
  }
}

function selectTeam(team) {
  selectedTeamId.value     = team.id
  selectedStudentIds.value = []
}

function toggleStudent(id) {
  const idx = selectedStudentIds.value.indexOf(id)
  if (idx === -1) selectedStudentIds.value.push(id)
  else            selectedStudentIds.value.splice(idx, 1)
}

async function confirmAssign() {
  try {
    await teamsApi.assignStudents(selectedTeamId.value, selectedStudentIds.value)
    showConfirm.value        = false
    selectedStudentIds.value = []
    showFlash('alert-success', `Students assigned to ${selectedTeam.value?.name ?? 'team'}.`)
    await load()
  } catch (e) {
    showConfirm.value = false
    showFlash('alert-error', e.message)
  }
}

async function removeStudent(teamId, studentId, studentName) {
  try {
    await teamsApi.removeStudent(teamId, studentId)
    showFlash('alert-success', `${studentName} removed from team.`)
    await load()
  } catch (e) {
    showFlash('alert-error', e.message)
  }
}

function showFlash(type, text) {
  flash.value = { type, text }
  setTimeout(() => { flash.value = { type: '', text: '' } }, 4000)
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div v-if="flash.text" :class="['alert', flash.type]" style="margin-bottom:16px">
      {{ flash.text }}
    </div>

    <p class="muted" style="margin-bottom:16px">
      Select a team on the left, check students on the right, then click Assign.
    </p>

    <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-muted)">Loading…</div>

    <div v-else style="display:flex;gap:16px;align-items:flex-start">

      <!-- Left panel: teams -->
      <div style="flex:1;min-width:220px">
        <div class="card" style="padding:0;overflow:hidden">
          <div style="padding:12px 16px;border-bottom:1px solid var(--border);font-weight:600">Teams</div>

          <div v-if="teams.length === 0" style="padding:16px;color:var(--text-muted);font-size:0.9rem">
            No teams found.
          </div>

          <template v-for="team in teams" :key="team.id">
            <!-- Team header row -->
            <div
              :style="{
                padding: '12px 16px',
                borderBottom: '1px solid var(--border)',
                cursor: 'pointer',
                background: selectedTeamId === team.id ? 'rgba(124,58,237,.08)' : '',
                borderLeft: selectedTeamId === team.id
                  ? '3px solid var(--purple)'
                  : '3px solid transparent',
              }"
              @click="selectTeam(team)"
            >
              <div style="display:flex;justify-content:space-between;align-items:center">
                <strong>{{ team.name }}</strong>
                <span
                  class="badge"
                  style="background:var(--purple);color:#fff;font-size:0.75rem;min-width:20px;text-align:center"
                >
                  {{ team.students?.length ?? 0 }}
                </span>
              </div>
              <div class="muted" style="font-size:0.8rem">{{ team.sectionName }}</div>
            </div>

            <!-- Expanded: current members of selected team -->
            <template v-if="selectedTeamId === team.id">
              <div
                v-if="team.students?.length"
                v-for="student in team.students"
                :key="student.id"
                style="display:flex;justify-content:space-between;align-items:center;
                       padding:7px 16px 7px 28px;font-size:0.875rem;
                       background:#f9f8ff;border-bottom:1px solid var(--border)"
              >
                <span>{{ student.firstName }} {{ student.lastName }}</span>
                <button
                  class="btn btn-secondary btn-sm"
                  style="font-size:0.75rem;padding:2px 8px;color:#dc2626"
                  @click.stop="removeStudent(team.id, student.id, `${student.firstName} ${student.lastName}`)"
                >
                  Remove
                </button>
              </div>
              <div
                v-else
                style="padding:8px 28px;font-size:0.8rem;color:var(--text-muted);
                       background:#f9f8ff;border-bottom:1px solid var(--border)"
              >
                No students assigned yet.
              </div>
            </template>
          </template>
        </div>
      </div>

      <!-- Right panel: unassigned students -->
      <div style="flex:1;min-width:220px">
        <div class="card" style="padding:0;overflow:hidden">
          <div style="padding:12px 16px;border-bottom:1px solid var(--border);font-weight:600">
            Unassigned Students
            <span class="muted" style="font-weight:400;font-size:0.85rem"> ({{ unassigned.length }})</span>
          </div>

          <div
            v-if="unassigned.length === 0"
            style="padding:24px 16px;color:var(--text-muted);font-size:0.9rem;text-align:center"
          >
            All students have been assigned to teams.
          </div>

          <label
            v-else
            v-for="student in unassigned"
            :key="student.id"
            style="display:flex;align-items:center;gap:10px;padding:10px 16px;
                   border-bottom:1px solid var(--border);cursor:pointer"
          >
            <input
              type="checkbox"
              :checked="selectedStudentIds.includes(student.id)"
              @change="toggleStudent(student.id)"
              style="width:16px;height:16px;cursor:pointer;flex-shrink:0"
            />
            <span style="flex:1">{{ student.firstName }} {{ student.lastName }}</span>
            <span class="muted" style="font-size:0.8rem">{{ student.email }}</span>
          </label>

          <div style="padding:12px 16px;border-top:1px solid var(--border)">
            <button
              class="btn btn-primary"
              style="width:100%"
              :disabled="!selectedTeamId || selectedStudentIds.length === 0"
              @click="showConfirm = true"
            >
              Assign to {{ selectedTeam?.name ?? 'a team' }}
            </button>
            <p v-if="!selectedTeamId" class="muted" style="font-size:0.78rem;margin-top:6px;text-align:center">
              Select a team on the left first.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Confirmation dialog -->
    <div
      v-if="showConfirm"
      style="position:fixed;inset:0;background:rgba(0,0,0,.45);
             display:flex;align-items:center;justify-content:center;z-index:9999"
      @click.self="showConfirm = false"
    >
      <div class="card" style="width:400px;max-width:90vw;padding:24px">
        <h3 style="margin:0 0 12px 0">Confirm Assignment</h3>
        <p style="margin:0 0 8px 0;color:var(--text-muted)">
          Assign
          <strong>{{ selectedStudentIds.length }} student{{ selectedStudentIds.length !== 1 ? 's' : '' }}</strong>
          to <strong>{{ selectedTeam?.name }}</strong>?
        </p>
        <ul style="margin:0 0 20px 0;padding-left:20px;font-size:0.9rem">
          <li v-for="id in selectedStudentIds" :key="id">
            {{ unassigned.find(s => s.id === id)?.firstName }} {{ unassigned.find(s => s.id === id)?.lastName }}
          </li>
        </ul>
        <div style="display:flex;justify-content:flex-end;gap:8px">
          <button class="btn btn-secondary" @click="showConfirm = false">Cancel</button>
          <button class="btn btn-primary" @click="confirmAssign">Confirm</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
