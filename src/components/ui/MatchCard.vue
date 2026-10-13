<script setup>
import AvatarStack from './AvatarStack.vue'

defineProps({
  match: {
    type: Object,
    required: true
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<template>
  <article class="match-card" :class="{ compact }">
    <div class="match-top">
      <strong>{{ match.route }}</strong>
      <span class="match-score">顺路 {{ match.score }}</span>
    </div>
    <div class="match-meta">
      <span>{{ match.time }}</span>
      <span>{{ match.seats }}</span>
      <b>¥ {{ match.price }}</b>
    </div>
    <div v-if="match.driverName || match.vehicle || match.driverCredit" class="match-driver">
      <div>
        <strong v-if="match.driverName">{{ match.driverName }}</strong>
        <span v-if="match.vehicle">{{ match.vehicle }}</span>
      </div>
      <span v-if="match.driverCredit" class="match-credit">信用 {{ match.driverCredit }}</span>
    </div>
    <AvatarStack v-if="match.avatars && match.avatars.length" :avatars="match.avatars" />
  </article>
</template>

