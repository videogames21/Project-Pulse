<script setup>
import { ref } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import { MOCK_RUBRICS } from '../../data/mockData'

const rubrics   = ref(MOCK_RUBRICS.map(r => ({ ...r, criteria: [...r.criteria] })))
const expanded  = ref(1)
const showModal = ref(false)
const cForm     = ref({ name: '', description: '', maxScore: 10 })
const flash     = ref('')

function addCriterion() {
  if (!cForm.value.name.trim()) return
  const rubric = rubrics.value.find(r => r.id === expanded.value)
  if (!rubric) return
  const id = Math.max(0, ...rubric.criteria.map(c => c.id)) + 1
  rubric.criteria.push({ id, ...cForm.value, maxScore: Number(cForm.value.maxScore) })
  cForm.value = { name: '', description: '', maxScore: 10 }
  showModal.value = false
  flash.value = 'Criterion added.'; setTimeout(() => flash.value = '', 3000)
}

function totalMax(rubric) { return rubric.criteria.reduce((s, c) => s + c.maxScore, 0) }
</script>

<template>
  <AppLayout>
    <div v-if="flash" class="alert alert-success">{{ flash }}</div>

    <div v-for="r in rubrics" :key="r.id" class="card mb-4">
      <div class="card-header" style="cursor:pointer" @click="expanded = expanded === r.id ? null : r.id">
        <div>
          <h3>{{ r.name }}</h3>
          <p class="muted" style="font-size:.8rem;margin-top:2px">{{ r.criteria.length }} criteria · {{ totalMax(r) }} pts total</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="badge badge-purple">{{ totalMax(r) }} pts</span>
          <span>{{ expanded === r.id ? '▲' : '▼' }}</span>
        </div>
      </div>

      <template v-if="expanded === r.id">
        <div v-if="r.criteria.length === 0" class="empty" style="padding:20px"><p>No criteria yet.</p></div>
        <div v-else class="table-wrap">
          <table>
            <thead><tr><th>Criterion</th><th>Description</th><th>Max Score</th></tr></thead>
            <tbody>
              <tr v-for="c in r.criteria" :key="c.id">
                <td><strong>{{ c.name }}</strong></td>
                <td class="muted">{{ c.description }}</td>
                <td><span class="badge badge-purple">{{ c.maxScore }} pts</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div style="padding:12px 0 0">
          <button class="btn btn-secondary btn-sm" @click="showModal = true">+ Add Criterion</button>
        </div>
      </template>
    </div>

    <div v-if="showModal" class="overlay" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-head"><h3>Add Criterion</h3><button class="modal-close" @click="showModal = false">×</button></div>
        <div class="modal-body">
          <div class="form-group"><label>Name</label><input v-model="cForm.name" placeholder="e.g. Technical Contribution" /></div>
          <div class="form-group"><label>Description</label><textarea v-model="cForm.description" placeholder="What does this measure?" /></div>
          <div class="form-group"><label>Max Score</label><input type="number" v-model.number="cForm.maxScore" min="1" max="100" /></div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
          <button class="btn btn-primary" @click="addCriterion">Add</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
