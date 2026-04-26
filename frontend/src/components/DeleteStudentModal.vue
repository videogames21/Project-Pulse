<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  student:    { type: Object,  required: true },
  modelValue: { type: Boolean, required: true },
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const countdown = ref(5)
let timer = null

watch(() => props.modelValue, (open) => {
  if (open) {
    countdown.value = 5
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } else {
    clearInterval(timer)
  }
}, { immediate: true })

function cancel() {
  clearInterval(timer)
  emit('update:modelValue', false)
}

function confirmDelete() {
  clearInterval(timer)
  emit('update:modelValue', false)
  emit('confirm')
}
</script>

<template>
  <div v-if="modelValue" class="overlay" @click.self="cancel">
    <div class="modal">
      <div class="modal-head">
        <h3>Delete Student</h3>
        <button class="btn btn-secondary btn-sm" @click="cancel">✕</button>
      </div>

      <div class="modal-body">
        <div class="alert alert-error" style="margin-bottom:16px">
          <strong>Warning — This action cannot be undone.</strong>
        </div>
        <p style="margin-bottom:12px">
          You are about to permanently delete
          <strong>{{ student.firstName }} {{ student.lastName }}</strong>.
          The following will be removed from the system:
        </p>
        <ul style="margin:0 0 12px 20px;line-height:1.8">
          <li>The student's account (cannot be recovered)</li>
          <li>All Weekly Activity Reports submitted by this student</li>
          <li>All Peer Evaluations given or received by this student</li>
          <li>The student will be removed from their section and team</li>
        </ul>
        <p>This deletion is <strong>permanent</strong> and cannot be reversed.</p>
      </div>

      <div class="modal-foot">
        <button class="btn btn-secondary" @click="cancel">Cancel</button>
        <button
          class="btn btn-danger"
          :disabled="countdown > 0"
          @click="confirmDelete"
        >
          {{ countdown > 0 ? `Delete Student (${countdown})` : 'Delete Student' }}
        </button>
      </div>
    </div>
  </div>
</template>
