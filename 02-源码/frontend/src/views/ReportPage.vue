<template>
  <div>
    <div class="page-head">
      <div><h1>统计分析</h1><p>统计口径与看板一致，图表点击可下钻到对应明细列表</p></div>
      <div class="right">
        <el-select v-model="range" style="width: 140px">
          <el-option label="最近 30 天" value="30" />
          <el-option label="本季度" value="quarter" />
          <el-option label="本年" value="year" />
        </el-select>
        <el-button :icon="Download" @click="exportCsv">导出</el-button>
      </div>
    </div>

    <div class="metric-grid" style="grid-template-columns: repeat(4, 1fr)">
      <div class="metric"><div class="k">准入通过率</div><div class="v">{{ metrics.admissionRate || 0 }}<small>%</small></div><div class="d up">通过 {{ metrics.approvedTotal || 0 }} / 申请 {{ metrics.applyTotal || 0 }}</div></div>
      <div class="metric"><div class="k">在册供应商</div><div class="v">{{ metrics.supplierTotal || 0 }}<small> 家</small></div><div class="d">A 级占比 {{ metrics.levelARatio || 0 }}%</div></div>
      <div class="metric"><div class="k">绩效评价覆盖</div><div class="v">{{ coverage }}<small>%</small></div><div class="d">已发布任务数与在册数之比</div></div>
      <div class="metric"><div class="k">整改完成率</div><div class="v">{{ rectRate || 0 }}<small>%</small></div><div class="d">全部整改单统计</div></div>
    </div>

    <div class="grid" style="margin-top: 12px">
      <ChartBox title="准入趋势" unit="单位：件 · 申请与通过" :option="trendOption" :height="190" />
      <ChartBox title="分层模型结果分布" unit="单位：次 · A–D 级" :option="levelOption" :height="190" />
    </div>

    <div class="panel" style="margin-top: 12px">
      <div class="panel-head">
        <h2>供应商绩效明细</h2>
        <span class="sub">点击行进入分层模型计算记录</span>
        <div class="right"><el-tag type="info" effect="plain">共 {{ total }} 家</el-tag></div>
      </div>
      <el-table :data="rows" v-loading="loading" style="width: 100%" @row-click="(row) => $router.push(`/supplier/${row.id}`)">
        <el-table-column prop="supplierName" label="供应商" min-width="200" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="类目" width="100" />
        <el-table-column label="等级" width="76"><template #default="{ row }"><LevelBadge :level="row.level" /></template></el-table-column>
        <el-table-column label="合作状态" width="110"><template #default="{ row }"><StatusTag :text="row.supplierStatus" /></template></el-table-column>
        <el-table-column label="风险" width="80"><template #default="{ row }"><StatusTag :text="row.riskLevel" /></template></el-table-column>
        <el-table-column label="证照" width="90">
          <template #default="{ row }"><span class="num">{{ row.certCount || 0 }}</span> 张</template>
        </el-table-column>
        <el-table-column label="资质到期" width="112"><template #default="{ row }">{{ $date(row.certExpireDate) }}</template></el-table-column>
        <el-table-column prop="ownerName" label="责任人" width="90" />
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="load" @size-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import ChartBox from '../components/ChartBox.vue'
import LevelBadge from '../components/LevelBadge.vue'
import StatusTag from '../components/StatusTag.vue'
import { dashboardApi, supplierApi, rectApi } from '../api'

const data = ref({ metrics: {}, charts: {} })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const range = ref('30')
const rectRate = ref(0)
const query = reactive({ page: 1, size: 10 })

const metrics = computed(() => data.value.metrics || {})
const coverage = computed(() => {
  const s = Number(metrics.value.supplierTotal || 0)
  const p = Number(metrics.value.approvedTotal || 0)
  if (!s) return 0
  return Math.min(100, Math.round((p / s) * 1000) / 10)
})

const trendOption = computed(() => {
  const r = data.value.charts?.trend || []
  return {
    grid: { left: 8, right: 16, top: 28, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: r.map((x) => x.month), axisLabel: { fontSize: 10, color: '#5b6e77' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#e8eef0' } }, axisLabel: { fontSize: 10, color: '#8a9aa2' } },
    series: [
      { name: '提交申请', type: 'line', smooth: true, symbolSize: 5, data: r.map((x) => x.applied), itemStyle: { color: '#15344a' } },
      { name: '评审通过', type: 'line', smooth: true, symbolSize: 5, data: r.map((x) => x.approved), itemStyle: { color: '#237a8b' } }
    ]
  }
})

const levelOption = computed(() => {
  const r = data.value.charts?.snapshotLevel || []
  return {
    grid: { left: 8, right: 16, top: 16, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: r.map((x) => x.name + ' 级'), axisLabel: { fontSize: 10, color: '#5b6e77' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#e8eef0' } }, axisLabel: { fontSize: 10, color: '#8a9aa2' } },
    series: [{
      type: 'bar', barWidth: 32,
      data: r.map((x) => ({
        value: x.value,
        itemStyle: { color: { A: '#2d7a58', B: '#237a8b', C: '#a9701f', D: '#b03035' }[x.name] || '#7fa6b0', borderRadius: [3, 3, 0, 0] }
      }))
    }]
  }
})

async function load() {
  loading.value = true
  try {
    const res = await supplierApi.page({ page: query.page, size: query.size })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function exportCsv() {
  if (!rows.value.length) return ElMessage.warning('没有可导出数据')
  const head = ['供应商', '类目', '等级', '合作状态', '风险等级', '证照数', '资质到期']
  const body = rows.value.map((r) => [r.supplierName, r.categoryName, r.level, r.supplierStatus, r.riskLevel, r.certCount, r.certExpireDate])
  const csv = '\ufeff' + [head, ...body].map((r) => r.join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `供应商绩效明细_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  data.value = await dashboardApi.overview()
  const st = await rectApi.stats()
  rectRate.value = st.completionRate || 0
  load()
})
</script>

<style scoped>
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
</style>
