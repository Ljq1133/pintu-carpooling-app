<script setup>
import { ArrowRight } from 'lucide-vue-next'

defineProps({
  label: {
    type: String,
    required: true
  },
  dark: {
    type: Boolean,
    default: false
  }
})
</script>

<template>
  <button class="primary-button" :class="{ dark }">
    <span>{{ label }}</span>
    <i><ArrowRight :size="17" /></i>
  </button>
</template>

