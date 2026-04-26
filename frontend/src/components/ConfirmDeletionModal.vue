<script setup>
import { ref } from 'vue'
import studentsApi from '../api/students'

const props = defineProps({
  student:    { type: Object,  required: true },
  modelValue: { type: Boolean, required: true },
})

const emit = defineEmits(['update:modelValue', 'deleted'])

const password = ref('')
const error    = ref('')
const loading  = ref(false)

function cancel() {
  password.value = ''
  error.value    = ''
  emit('update:modelValue', false)
}

async function confirm() {
  if (!password.value.trim()) {
    error.value = 'Please enter your admin password.'
    return
  }
  loading.value = true
  error.value   = ''
  try {
    await studentsApi.delete(props.student.id, password.value)
    password.value = ''
    emit('update:modelValue', false)
    emit('deleted')
  } catch (e) {
    if (e.status === 401) {
      error.value = 'Incorrect password. Please try again.'
    } else {
      error.value = e.data?.message || e.message || 'An error occurred.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-if="modelValue" class="overlay" @click.self="cancel">
    <div class="modal">
      <div class="modal-head">
        <h3>Confirm Student Deletion</h3>
        <button class="btn btn-secondary btn-sm" @click="cancel">✕</button>
      </div>

      <div class="modal-body">
        <p style="margin-bottom:16px">
          You are about to permanently delete
          <strong>{{ student.firstName }} {{ student.lastName }}</strong>.
          Enter your admin password to confirm.
        </p>

        <div class="form-group" style="margin-bottom:0">
          <label>Enter Admin Password <span style="color:#dc2626">*</span></label>
          <input
            type="password"
            v-model="password"
            placeholder="Admin password..."
            @keyup.enter="confirm"
          />
        </div>

        <div v-if="error" class="alert alert-error" style="margin-top:12px">{{ error }}</div>
      </div>

      <div class="modal-foot">
        <button class="btn btn-secondary" :disabled="loading" @click="cancel">Cancel</button>
        <button class="btn btn-danger" :disabled="loading" @click="confirm">
          {{ loading ? 'Deleting...' : 'Confirm Student Deletion' }}
        </button>
      </div>
    </div>
  </div>
</template>
