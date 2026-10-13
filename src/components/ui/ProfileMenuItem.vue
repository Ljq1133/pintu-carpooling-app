<script setup>
import { ChevronRight } from 'lucide-vue-next'

defineProps({
  icon: {
    type: Object,
    required: true
  },
  label: {
    type: String,
    required: true
  }
})
</script>

<template>
  <button class="profile-menu-item">
    <component :is="icon" :size="18" />
    <span>{{ label }}</span>
    <ChevronRight :size="17" />
  </button>
</template>

