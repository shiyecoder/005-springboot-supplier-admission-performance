<template>
  <span v-if="level" class="lvl" :class="`lvl-${level}`">{{ level }}</span>
  <span v-else class="hint">未评级</span>
</template>

<script setup>
defineProps({ level: { type: String, default: '' } })
</script>
