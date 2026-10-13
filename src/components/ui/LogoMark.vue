<script setup>
defineProps({
  size: {
    type: String,
    default: 'medium'
  }
})
</script>

<template>
  <div class="logo-mark" :class="`logo-${size}`" aria-label="拼图 PinTu 标识">
    <div class="piece piece-top"></div>
    <div class="piece piece-gray"></div>
    <div class="piece piece-left"></div>
    <div class="car-body">
      <span class="wheel wheel-left"></span>
      <span class="wheel wheel-right"></span>
    </div>
  </div>
</template>

