<template>
  <div class="chart-box">
    <h3>{{ title }}</h3>
    <div class="u">{{ unit }}</div>
    <div ref="el" :style="{ height: height + 'px' }"></div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  title: String,
  unit: { type: String, default: '' },
  option: { type: Object, required: true },
  height: { type: Number, default: 168 }
})

const el = ref()
let chart = null
let timer = null

function render() {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value)
  chart.setOption(props.option, true)
}

onMounted(() => {
  nextTick(render)
  timer = setInterval(() => chart && chart.resize(), 800)
  window.addEventListener('resize', render)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('resize', render)
  chart?.dispose()
  chart = null
})
</script>
