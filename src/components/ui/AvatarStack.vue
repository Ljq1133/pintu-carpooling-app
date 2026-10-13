<script setup>
defineProps({
  avatars: {
    type: Array,
    default: () => []
  }
})
</script>

<template>
  <div class="avatar-stack">
    <span v-for="avatar in avatars" :key="avatar">{{ avatar }}</span>
    <span class="more">+</span>
  </div>
</template>

