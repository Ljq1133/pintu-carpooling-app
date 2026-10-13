<script setup>
import { computed, watch } from 'vue'
import { useSharedPlatform } from './platform/sharedPlatform.js'

const { passengerState } = useSharedPlatform()
const accountPreferences = computed(() => passengerState.accountPreferences || {})

function applyAccountPreferences(preferences = {}) {
  const darkMode = Boolean(preferences.darkMode)
  const fontScale = preferences.fontScale || 'standard'
  const locale = preferences.locale || 'zh-CN'
  const scaleValue = {
    compact: '0.94',
    standard: '1',
    large: '1.08'
  }[fontScale] || '1'

  document.documentElement.lang = locale
  document.documentElement.dataset.pintuTheme = darkMode ? 'dark' : 'light'
  document.documentElement.dataset.pintuFontScale = fontScale
  document.documentElement.style.setProperty('--pintu-font-scale', scaleValue)
}

watch(accountPreferences, applyAccountPreferences, { immediate: true, deep: true })
</script>

<template>
  <RouterView />
</template>
