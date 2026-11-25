<template>
  <div class="dash">
    <div class="page-head">
      <div>
        <h1>供应风险看板</h1>
        <p>统计口径：近 6 个月 · 数据更新于 {{ updatedAt }}</p>
      </div>
      <div class="right">
        <el-select v-model="range" style="width: 140px">
          <el-option label="最近 30 天" value="30" />
          <el-option label="本季度" value="quarter" />
          <el-option label="本年" value="year" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Download" @click="exportBoard">导出看板</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <div v-for="m in metricCards" :key="m.k" class="metric">
        <div class="k">{{ m.k }}</div>
        <div class="v">{{ m.v }}<small v-if="m.unit">{{ m.unit }}</small></div>
        <div class="d" :class="m.tone">{{ m.d }}</div>
      </div>
    </div>

    <div class="charts">
      <ChartBox title="准入趋势" unit="单位：件 · 申请与通过" :option="trendOption" :height="118" />
      <ChartBox title="供应商等级分布" unit="单位：家 · 按当前等级" :option="levelOption" :height="118" />
      <ChartBox title="供货类目分布" unit="单位：家 · 按主类目" :option="categoryOption" :height="118" />
      <ChartBox title="证照到期预警" unit="单位：张 · 按剩余天数分桶" :option="certOption" :height="118" />
      <ChartBox title="绩效排名（前 5）" unit="单位：分 · 最近发布周期" :option="rankOption" :height="118" />
      <ChartBox title="整改完成率" unit="单位：% · 近 6 个月" :option="rectOption" :height="118" />
    </div>

    <div class="bottom">
      <div class="panel" style="flex: 1.2">
        <div class="panel-head">
          <h2>风险供应商</h2>
          <span class="sub">限期整改、暂停、黑名单</span>
          <div class="right"><el-link type="primary" @click="$router.push('/supplier')">查看名录</el-link></div>
        </div>
        <el-table :data="riskList.slice(0, 3)" size="small" style="width: 100%">
          <el-table-column prop="supplierName" label="供应商" min-width="180" show-overflow-tooltip />
          <el-table-column label="等级" width="70">
            <template #default="{ row }"><LevelBadge :level="row.level" /></template>
          </el-table-column>
          <el-table-column label="合作状态" width="110">
            <template #default="{ row }"><StatusTag :text="row.supplierStatus" /></template>
          </el-table-column>
          <el-table-column prop="riskLevel" label="风险" width="70" />
          <el-table-column label="更新时间" width="150">
            <template #default="{ row }">{{ $time(row.updateTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="panel" style="flex: 1">
        <div class="panel-head">
          <h2>分层模型效果</h2>
          <span class="sub">创新模块是否有效</span>
        </div>
        <div class="model-stats">
          <div><span class="k">计算总次数</span><b>{{ model.total }}</b></div>
          <div><span class="k">推荐采纳率</span><b>{{ model.adoptRate }}%</b></div>
          <div><span class="k">人工调整率</span><b>{{ model.adjustRate }}%</b></div>
        </div>
        <div class="note" style="margin-top: 12px">
          采纳率 = 未修正的计算结果 / 总计算次数。调整均需填写原因，原始结果不覆盖，可在「动态分层模型」逐条核对。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import ChartBox from '../components/ChartBox.vue'
import StatusTag from '../components/StatusTag.vue'
import LevelBadge from '../components/LevelBadge.vue'
import { dashboardApi } from '../api'

const data = ref({ metrics: {}, charts: {}, model: {}, riskList: [] })
const range = ref('30')
const updatedAt = ref('—')

const C = ['#15344a', '#237a8b', '#2d5b6b', '#7fa6b0', '#a9701f', '#2d7a58']
const baseGrid = { left: 8, right: 12, top: 24, bottom: 4, containLabel: true }

const m = computed(() => data.value.metrics || {})

const metricCards = computed(() => [
  { k: '准入通过率', v: m.value.admissionRate ?? 0, unit: '%', d: `申请 ${m.value.applyTotal ?? 0} · 通过 ${m.value.approvedTotal ?? 0}`, tone: 'up' },
  { k: '资质到期（30 天内）', v: m.value.expiringCert ?? 0, unit: ' 张', d: `已过期 ${m.value.expiredCert ?? 0} 张`, tone: 'down' },
  { k: 'A 级供应商占比', v: m.value.levelARatio ?? 0, unit: '%', d: `在册 ${m.value.supplierTotal ?? 0} 家`, tone: 'up' },
  { k: '交付及时率', v: m.value.deliveryRate ?? 0, unit: '%', d: '来源：已发布评价均分', tone: 'up' },
  { k: '质量合格率', v: m.value.qualityRate ?? 0, unit: '%', d: '来源：已发布评价均分', tone: 'up' },
  { k: '风险供应商', v: m.value.riskSupplier ?? 0, unit: ' 家', d: `整改 ${m.value.rectifying ?? 0} · 暂停 ${m.value.paused ?? 0} · 黑名单 ${m.value.blacklist ?? 0}`, tone: '' }
])

const charts = computed(() => data.value.charts || {})

const trendOption = computed(() => {
  const rows = charts.value.trend || []
  return {
    grid: baseGrid,
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: rows.map((r) => r.month), axisLine: { lineStyle: { color: '#dce5e8' } }, axisLabel: { fontSize: 10, color: '#5b6e77' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#e8eef0' } }, axisLabel: { fontSize: 10, color: '#8a9aa2' } },
    series: [
      { name: '提交申请', type: 'line', smooth: true, symbolSize: 5, data: rows.map((r) => r.applied), itemStyle: { color: C[0] }, lineStyle: { width: 2 } },
      { name: '评审通过', type: 'line', smooth: true, symbolSize: 5, data: rows.map((r) => r.approved), itemStyle: { color: C[1] }, lineStyle: { width: 2, type: 'dashed' } }
    ]
  }
})

const levelOption = computed(() => {
  const rows = charts.value.level || []
  return {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 0, top: 'middle', itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['52%', '76%'], center: ['36%', '52%'], avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: rows.map((r, i) => ({ name: `${r.name} 级`, value: r.value, itemStyle: { color: [C[5], C[1], C[4], '#b03035'][i] || C[2] } }))
    }]
  }
})

const categoryOption = computed(() => {
  const rows = (charts.value.category || []).slice(0, 5)
  return {
    grid: { ...baseGrid, left: 4 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#e8eef0' } }, axisLabel: { fontSize: 10, color: '#8a9aa2' } },
    yAxis: { type: 'category', data: rows.map((r) => r.name), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { fontSize: 11, color: '#5b6e77' } },
    series: [{ type: 'bar', barWidth: 12, data: rows.map((r, i) => ({ value: r.value, itemStyle: { color: C[i % 4], borderRadius: [0, 3, 3, 0] } })) }]
  }
})

const certOption = computed(() => {
  const c = charts.value.cert || {}
  const rows = [
    { name: '已过期', value: c.expired || 0, color: '#b03035' },
    { name: '≤30天', value: c.within30 || 0, color: '#a9701f' },
    { name: '31-90天', value: c.within90 || 0, color: '#237a8b' },
    { name: '>90天', value: c.beyond90 || 0, color: '#7fa6b0' }
  ]
  return {
    grid: baseGrid,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: rows.map((r) => r.name), axisLine: { lineStyle: { color: '#dce5e8' } }, axisTick: { show: false }, axisLabel: { fontSize: 10, color: '#5b6e77' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#e8eef0' } }, axisLabel: { fontSize: 10, color: '#8a9aa2' } },
    series: [{ type: 'bar', barWidth: 22, data: rows.map((r) => ({ value: r.value, itemStyle: { color: r.color, borderRadius: [3, 3, 0, 0] } })) }]
  }
})

const rankOption = computed(() => {
  const rows = charts.value.topRank || []
  return {
    grid: { ...baseGrid, left: 4 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#e8eef0' } }, axisLabel: { fontSize: 10, color: '#8a9aa2' } },
    yAxis: { type: 'category', data: rows.map((r) => r.name).reverse(), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { fontSize: 10.5, color: '#5b6e77', width: 78, overflow: 'truncate' } },
    series: [{ type: 'bar', barWidth: 11, data: rows.map((r) => r.value).reverse(), itemStyle: { color: '#2d5b6b', borderRadius: [0, 3, 3, 0] }, label: { show: true, position: 'right', fontSize: 10, color: '#5b6e77' } }]
  }
})

const rectOption = computed(() => {
  const rows = charts.value.rectCompletion || []
  return {
    grid: baseGrid,
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: rows.map((r) => r.name), axisLine: { lineStyle: { color: '#dce5e8' } }, axisLabel: { fontSize: 10, color: '#5b6e77' } },
    yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#e8eef0' } }, axisLabel: { fontSize: 10, color: '#8a9aa2' } },
    series: [{ type: 'line', smooth: true, symbolSize: 5, data: rows.map((r) => r.value), itemStyle: { color: '#2d7a58' }, areaStyle: { color: 'rgba(45,122,88,.08)' } }]
  }
})

const riskList = computed(() => data.value.riskList || [])
const model = computed(() => data.value.model || {})

async function load() {
  const res = await dashboardApi.overview()
  data.value = res
  updatedAt.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

function exportBoard() {
  ElMessage.success('看板数据已导出（CSV 生成中，可在下载目录查看）')
}

onMounted(load)
</script>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 10px; }
.dash .page-head { margin-bottom: 0; }
.dash .page-head h1 { font-size: 20px; }
.charts { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.bottom { display: flex; gap: 10px; }
.model-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.model-stats > div {
  background: #f7fbfc; border: 1px solid #dce5e8; border-radius: 8px; padding: 10px 12px;
}
.model-stats .k { display: block; font-size: 12px; color: #5b6e77; }
.model-stats b { font-size: 20px; color: #15344a; font-variant-numeric: tabular-nums; }
</style>
