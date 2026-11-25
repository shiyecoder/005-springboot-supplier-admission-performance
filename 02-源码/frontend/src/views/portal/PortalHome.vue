<template>
  <div>
    <div class="hero">
      <div>
        <h2>{{ company.supplierName || '我的企业' }}</h2>
        <p>
          供应商编码 {{ company.supplierCode || '—' }} · {{ company.level || '未评级' }} 级 ·
          {{ company.supplierStatus || '—' }} ·
          合格名录有效期至 {{ $date(company.certExpireDate) }}
        </p>
      </div>
      <div class="st">
        <div class="pill"><b>{{ company.level || '—' }}</b><span>当前等级</span></div>
        <div class="pill"><b>{{ latestScore }}</b><span>最近综合得分</span></div>
        <div class="pill"><b>{{ todoCount }}</b><span>待办事项</span></div>
      </div>
    </div>

    <div class="cols">
      <div>
        <div class="pcard">
          <div class="panel-head">
            <h2>准入进度</h2>
            <StatusTag v-if="currentApp" :text="currentApp.status" />
            <div class="right"><span class="sub2">{{ currentApp?.applicationNo || '暂无申请' }}</span></div>
          </div>
          <div class="steps">
            <div v-for="(s, i) in steps" :key="s" class="step" :class="stepClass(i)">
              <div class="dot">{{ i < stepIndex ? '✓' : i + 1 }}</div>{{ s }}
            </div>
          </div>
          <div class="note" style="margin-top: 14px">
            {{ currentApp ? `当前节点：${currentApp.currentNode || currentApp.status}，最近更新 ${$time(currentApp.updateTime)}` : '尚未提交准入申请，可在「准入申请」中新建' }}
          </div>
        </div>

        <div class="pcard">
          <div class="panel-head"><h2>待我处理</h2><div class="right"><span class="sub2">整改与评价任务</span></div></div>
          <div v-if="!todos.length" class="empty">暂无待办，保持良好履约记录</div>
          <div v-for="t in todos" :key="t.key" class="todo">
            <div class="ico" :class="t.tone"><el-icon><component :is="t.icon" /></el-icon></div>
            <div class="t">
              <b>{{ t.title }}</b>
              <span class="sub2">{{ t.desc }}</span>
            </div>
            <el-button size="small" @click="$router.push(t.path)">{{ t.action }}</el-button>
          </div>
        </div>

        <div class="pcard">
          <div class="panel-head"><h2>我的证照</h2><div class="right"><el-link type="primary" @click="$router.push('/portal/certificate')">全部 {{ certs.length }} 份</el-link></div></div>
          <el-table :data="certs.slice(0, 5)" size="small">
            <el-table-column prop="certType" label="证照类型" min-width="150" />
            <el-table-column label="到期日期" width="112"><template #default="{ row }">{{ $date(row.expireDate) }}</template></el-table-column>
            <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag :text="row.certStatus" /></template></el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }"><el-link type="primary" :href="row.filePath" target="_blank">查看</el-link></template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div>
        <div class="pcard">
          <div class="panel-head"><h2>等级变化</h2><span class="sub">近 4 次计算</span></div>
          <ChartBox title="" unit="" :option="scoreOption" :height="150" style="border: 0; padding: 0" />
          <div class="note" style="margin-top: 8px">{{ scoreNote }}</div>
        </div>

        <div class="pcard">
          <div class="panel-head"><h2>企业信息</h2><div class="right"><el-link type="primary" @click="$router.push('/portal/company')">查看与变更</el-link></div></div>
          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="统一信用代码"><span class="mono">{{ company.creditCode }}</span></el-descriptions-item>
            <el-descriptions-item label="供应类别">{{ company.categoryName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ company.contactName }} · {{ company.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="主要客户">{{ company.mainCustomers || '—' }}</el-descriptions-item>
            <el-descriptions-item label="年产能">{{ company.annualCapacity || '—' }}</el-descriptions-item>
          </el-descriptions>
          <div class="warnbox" style="margin-top: 10px">
            信息变更需提交申请，审核通过后覆盖当前版本；变更期间原信息继续有效。
          </div>
        </div>

        <div class="pcard">
          <div class="panel-head"><h2>消息通知</h2></div>
          <div v-if="!messages.length" class="empty">暂无消息</div>
          <div v-for="m in messages.slice(0, 4)" :key="m.id" class="msg">
            <b>{{ m.title }}</b>
            <div class="sub2">{{ m.content }}</div>
            <div class="sub2">{{ $time(m.createTime) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import ChartBox from '../../components/ChartBox.vue'
import StatusTag from '../../components/StatusTag.vue'
import { authApi, supplierApi, certApi, admissionApi, rectApi, performanceApi, modelApi } from '../../api'
import { store } from '../../store/user'

const company = ref({})
const certs = ref([])
const apps = ref([])
const tasks = ref([])
const rects = ref([])
const messages = ref([])
const snaps = ref([])

const steps = ['提交申请', '形式审查', '专业评审', '审批准入', '合作评价']
const statusIndex = { 待提交: 0, 准入审核: 1, 现场评估: 2, 待审批: 3, 已准入: 4, 已驳回: 1, 合作评价: 4, 分级调整: 4 }

const currentApp = computed(() => apps.value[0])
const stepIndex = computed(() => (currentApp.value ? statusIndex[currentApp.value.status] ?? 0 : 0))
const latestScore = computed(() => tasks.value.find((t) => t.avgScore)?.avgScore ?? snaps.value[0]?.totalScore ?? '—')

const todos = computed(() => {
  const list = []
  rects.value
    .filter((r) => ['待提交措施', '已逾期'].includes(r.status))
    .forEach((r) => list.push({
      key: 'r' + r.id, title: r.issueDesc, desc: `整改 ${r.rectificationNo} · 截止 ${r.deadline}`,
      path: '/portal/rectification', action: '提交措施', icon: 'Warning', tone: 'warn'
    }))
  tasks.value
    .filter((t) => ['待评分', '评分中'].includes(t.status))
    .forEach((t) => list.push({
      key: 't' + t.id, title: `${t.period} 绩效评价待配合`, desc: `任务 ${t.taskNo} · 截止 ${t.deadline}`,
      path: '/portal/performance', action: '查看', icon: 'TrendCharts', tone: 'info'
    }))
  return list
})
const todoCount = computed(() => todos.value.length)

// 等级变化折线：纵轴用总分，便于看出趋势
const scoreOption = computed(() => {
  const rows = [...snaps.value].reverse()
  return {
    grid: { left: 6, right: 12, top: 16, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: rows.map((r) => r.period), axisLabel: { fontSize: 10, color: '#5b6e77' } },
    yAxis: { type: 'value', min: 50, max: 100, splitLine: { lineStyle: { color: '#e8eef0' } }, axisLabel: { fontSize: 10, color: '#8a9aa2' } },
    series: [{
      type: 'line', smooth: true, symbolSize: 6, data: rows.map((r) => r.totalScore),
      itemStyle: { color: '#237a8b' }, areaStyle: { color: 'rgba(35,122,139,.10)' }
    }]
  }
})

const scoreNote = computed(() => {
  const latest = snaps.value[0]
  if (!latest) return '暂无计算记录，采购专员完成评价后可查看。'
  return `最近一次 ${latest.period}：综合得分 ${latest.totalScore}，等级 ${latest.finalLevel}；规则版本 ${latest.ruleVersion}。`
})

function stepClass(i) {
  if (i < stepIndex.value) return 'done'
  if (i === stepIndex.value) return 'cur'
  return ''
}

onMounted(async () => {
  const sid = store.user?.supplierId
  if (!sid) return
  const detail = await supplierApi.detail(sid)
  company.value = detail.supplier
  certs.value = detail.certificates
  apps.value = detail.applications
  tasks.value = detail.tasks
  rects.value = (await rectApi.page({ page: 1, size: 20 })).records
  messages.value = await authApi.messages()
  try {
    snaps.value = (await modelApi.snapshots({ page: 1, size: 4 })).records
  } catch (e) { snaps.value = [] }
})
</script>

<style scoped>
.hero {
  background: linear-gradient(120deg, #237a8b, #1b5e6b); color: #fff; padding: 24px 28px;
  border-radius: 8px; display: flex; align-items: center; gap: 24px;
}
.hero h2 { font-size: 20px; margin: 0; font-weight: 600; }
.hero p { font-size: 12.5px; color: rgba(255, 255, 255, 0.8); margin: 6px 0 0; }
.hero .st { margin-left: auto; display: flex; gap: 10px; }
.hero .pill {
  background: rgba(255, 255, 255, 0.14); border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 6px; padding: 8px 14px; text-align: center; min-width: 92px;
}
.hero .pill b { display: block; font-size: 18px; font-variant-numeric: tabular-nums; }
.hero .pill span { font-size: 11.5px; color: rgba(255, 255, 255, 0.75); }
.cols { display: grid; grid-template-columns: 1.35fr 1fr; gap: 16px; margin-top: 16px; align-items: start; }
.pcard { background: #fff; border: 1px solid #dce5e8; border-radius: 8px; padding: 18px 20px; margin-bottom: 16px; }
.steps { display: flex; }
.step { flex: 1; text-align: center; position: relative; font-size: 12.5px; color: #8a9aa2; }
.step:before { content: ""; position: absolute; top: 11px; left: 0; right: 0; height: 2px; background: #dce5e8; }
.step:first-child:before { left: 50%; }
.step:last-child:before { right: 50%; }
.step .dot {
  position: relative; z-index: 1; width: 24px; height: 24px; border-radius: 50%; background: #fff;
  border: 2px solid #dce5e8; display: grid; place-items: center; margin: 0 auto 6px; font-size: 11px;
}
.step.done { color: #2d7a58; }
.step.done .dot { background: #2d7a58; border-color: #2d7a58; color: #fff; }
.step.cur { color: #237a8b; font-weight: 600; }
.step.cur .dot { border-color: #237a8b; color: #237a8b; }
.todo { display: flex; align-items: center; gap: 12px; padding: 11px 0; border-bottom: 1px solid #eef2f4; }
.todo:last-child { border-bottom: 0; }
.todo .ico { width: 32px; height: 32px; border-radius: 6px; display: grid; place-items: center; flex: none; }
.todo .ico.warn { background: rgba(169, 112, 31, 0.12); color: #a9701f; }
.todo .ico.info { background: rgba(35, 122, 139, 0.1); color: #237a8b; }
.todo .t { flex: 1; }
.todo .t b { display: block; font-size: 13px; font-weight: 500; }
.empty { padding: 18px 0; text-align: center; color: #8a9aa2; font-size: 12.5px; }
.msg { padding: 9px 0; border-bottom: 1px solid #eef2f4; font-size: 13px; }
.msg:last-child { border-bottom: 0; }
</style>
