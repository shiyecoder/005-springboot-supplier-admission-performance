<template>
  <div>
    <div class="page-head">
      <div>
        <h1>供应商动态分层模型</h1>
        <p>{{ rule.ruleName }} · 当前版本 {{ rule.version }} · 权重合计 {{ weightSum }}% · 同一输入与版本必然得到同一结果</p>
      </div>
      <div class="right">
        <el-select v-model="evalForm.supplierId" filterable placeholder="选择供应商" style="width: 220px">
          <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
        </el-select>
        <el-input v-model="evalForm.period" placeholder="周期，如 2026 Q4" style="width: 150px" />
        <el-button type="primary" @click="evaluate">执行计算</el-button>
      </div>
    </div>

    <div class="cols">
      <div class="panel">
        <div class="panel-head"><h2>规则配置</h2><span class="sub">修改后生成新版本，历史快照保留原版本</span></div>
        <div v-for="w in WEIGHTS" :key="w.key" class="wrow">
          <span>{{ w.label }}</span>
          <el-slider v-model="ruleForm[w.key]" :max="60" :step="5" show-stops />
          <span class="pv">{{ ruleForm[w.key] }}%</span>
        </div>
        <div class="sum">
          <span>合计</span><b>{{ weightSum }}%</b>
          <span v-if="weightSum === 100" class="tag-ok">校验通过</span>
          <span v-else class="tag-bad">必须等于 100%</span>
        </div>
        <div class="group-t">等级阈值</div>
        <el-row :gutter="12">
          <el-col :span="8"><div class="fi"><label>A 级 ≥</label><el-input-number v-model="ruleForm.thresholdA" :min="60" :max="100" controls-position="right" style="width: 100%" /></div></el-col>
          <el-col :span="8"><div class="fi"><label>B 级 ≥</label><el-input-number v-model="ruleForm.thresholdB" :min="50" :max="95" controls-position="right" style="width: 100%" /></div></el-col>
          <el-col :span="8"><div class="fi"><label>C 级 ≥</label><el-input-number v-model="ruleForm.thresholdC" :min="40" :max="90" controls-position="right" style="width: 100%" /></div></el-col>
        </el-row>
        <div class="note" style="margin-top: 12px">
          修改权重只影响新计算或主动重新计算的数据；人工修正必须填写原因，原始计算结果不覆盖。
        </div>
        <div style="display: flex; justify-content: flex-end; margin-top: 12px">
          <el-button type="primary" :disabled="weightSum !== 100" @click="saveRule">保存为新版本</el-button>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head"><h2>模型效果</h2><span class="sub">验证创新模块是否有效</span></div>
        <div class="stat-grid">
          <div><span class="k">计算总次数</span><b>{{ modelStats.total || 0 }}</b></div>
          <div><span class="k">推荐采纳率</span><b>{{ modelStats.adoptRate || 0 }}%</b></div>
          <div><span class="k">人工调整率</span><b>{{ modelStats.adjustRate || 0 }}%</b></div>
          <div><span class="k">调整次数</span><b>{{ modelStats.adjusted || 0 }}</b></div>
        </div>
        <ChartBox title="最终等级分布" unit="单位：次 · 按修正后等级" :option="levelOption" :height="150" style="margin-top: 12px; border: 0; padding: 0" />
        <div class="note" style="margin-top: 10px">
          推荐采纳率 = 未人工修正的结果数 / 总计算数。持续偏低说明权重或阈值需要复核。
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h2>计算留痕</h2>
        <span class="sub">输入快照 + 规则版本 + 各项得分 + 解释，可逐条核对</span>
        <div class="right">
          <el-select v-model="query.level" placeholder="等级" clearable style="width: 110px" @change="loadSnapshots">
            <el-option v-for="l in ['A', 'B', 'C', 'D']" :key="l" :label="l + ' 级'" :value="l" />
          </el-select>
        </div>
      </div>
      <el-table :data="snapshots" v-loading="loading" style="width: 100%" @row-click="openSnapshot">
        <el-table-column prop="supplierName" label="供应商" min-width="200" show-overflow-tooltip />
        <el-table-column prop="period" label="周期" width="110" />
        <el-table-column prop="ruleVersion" label="规则版本" width="110" />
        <el-table-column prop="totalScore" label="综合得分" width="100"><template #default="{ row }"><span class="num">{{ row.totalScore }}</span></template></el-table-column>
        <el-table-column label="原等级" width="80"><template #default="{ row }"><LevelBadge :level="row.resultLevel" /></template></el-table-column>
        <el-table-column label="最终等级" width="90"><template #default="{ row }"><LevelBadge :level="row.finalLevel" /></template></el-table-column>
        <el-table-column label="是否修正" width="96">
          <template #default="{ row }">
            <StatusTag :text="row.resultLevel === row.finalLevel ? '采纳' : '已调整'" />
          </template>
        </el-table-column>
        <el-table-column prop="adjustReason" label="修正原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="计算时间" width="160"><template #default="{ row }">{{ $time(row.createTime) }}</template></el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="snapTotal"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadSnapshots" @size-change="loadSnapshots" />
      </div>
    </div>

    <el-drawer v-model="drawer" size="760px" :title="detail?.snapshot?.supplierName || '计算明细'">
      <div v-if="detail">
        <div class="panel">
          <div class="score-top">
            <div><div class="sub2">综合得分</div><div class="big-score">{{ detail.snapshot.totalScore }}</div></div>
            <div><div class="sub2">结果等级</div><LevelBadge :level="detail.snapshot.resultLevel" /></div>
            <div><div class="sub2">最终等级</div><LevelBadge :level="detail.snapshot.finalLevel" /></div>
            <div><div class="sub2">规则版本</div><div class="big">{{ detail.snapshot.ruleVersion }}</div></div>
            <div><div class="sub2">计算时间</div><div class="big">{{ $time(detail.snapshot.createTime) }}</div></div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-head"><h2>指标明细</h2><span class="sub">缺失指标不参与计算，权重按剩余项归一</span></div>
          <el-table :data="detail.explain.details || []" size="small" empty-text="该次计算未保存指标明细（旧版本数据）">
            <el-table-column prop="name" label="指标" min-width="120" />
            <el-table-column prop="score" label="得分" width="86"><template #default="{ row }">{{ row.score ?? '缺失' }}</template></el-table-column>
            <el-table-column prop="weight" label="权重" width="80"><template #default="{ row }">{{ row.weight }}%</template></el-table-column>
            <el-table-column prop="weighted" label="加权" width="86" />
            <el-table-column prop="note" label="说明" min-width="160" />
          </el-table>
        </div>

        <div class="panel">
          <div class="panel-head"><h2>结果解释</h2><span class="sub">不能只给一个分数</span></div>
          <div class="explain">
            <div class="box"><h4>主要加分因素</h4><ul><li v-for="t in detail.explain.positive || []" :key="t">{{ t }}</li><li v-if="!(detail.explain.positive || []).length">无</li></ul></div>
            <div class="box"><h4>主要减分因素</h4><ul><li v-for="t in detail.explain.negative || []" :key="t">{{ t }}</li><li v-if="!(detail.explain.negative || []).length">无</li></ul></div>
            <div class="box"><h4>未满足条件</h4><ul><li v-for="t in detail.explain.unmet || []" :key="t">{{ t }}</li><li v-if="!(detail.explain.unmet || []).length">无</li></ul></div>
            <div class="box"><h4>可改进项</h4><ul><li v-for="t in detail.explain.improve || []" :key="t">{{ t }}</li><li v-if="!(detail.explain.improve || []).length">无</li></ul></div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-head"><h2>人工修正</h2><span class="sub">原始结果不覆盖，修正单独记录</span></div>
          <el-form label-width="90px">
            <el-form-item label="修正等级">
              <el-radio-group v-model="adjustForm.level">
                <el-radio-button v-for="l in ['A', 'B', 'C', 'D']" :key="l" :label="l" />
              </el-radio-group>
            </el-form-item>
            <el-form-item label="修正原因">
              <el-input v-model="adjustForm.reason" type="textarea" :rows="3" placeholder="不少于 5 个字，如：战略供应商，结合履约历史暂缓降级" />
            </el-form-item>
            <div style="display: flex; justify-content: flex-end"><el-button type="primary" @click="doAdjust">提交修正</el-button></div>
          </el-form>
          <div v-if="detail.snapshot.adjustReason" class="warnbox" style="margin-top: 10px">
            已修正：{{ detail.snapshot.resultLevel }} → {{ detail.snapshot.finalLevel }}，原因：{{ detail.snapshot.adjustReason }}
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ChartBox from '../components/ChartBox.vue'
import LevelBadge from '../components/LevelBadge.vue'
import StatusTag from '../components/StatusTag.vue'
import { modelApi, supplierApi } from '../api'

const WEIGHTS = [
  { key: 'weightQuality', label: '质量合格率' },
  { key: 'weightDelivery', label: '交付及时率' },
  { key: 'weightPrice', label: '价格稳定性' },
  { key: 'weightResponse', label: '服务响应' },
  { key: 'weightComplaint', label: '投诉情况' },
  { key: 'weightCert', label: '资质有效性' }
]

const rule = reactive({ ruleName: '供应商动态分层模型', version: '—' })
const ruleForm = reactive({
  weightQuality: 30, weightDelivery: 25, weightPrice: 15, weightResponse: 15, weightComplaint: 10, weightCert: 5,
  thresholdA: 85, thresholdB: 75, thresholdC: 60
})
const suppliers = ref([])
const evalForm = reactive({ supplierId: null, period: '' })
const modelStats = ref({})
const snapshots = ref([])
const snapTotal = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, level: '' })
const drawer = ref(false)
const detail = ref(null)
const adjustForm = reactive({ level: 'A', reason: '' })

const weightSum = computed(() => WEIGHTS.reduce((s, w) => s + Number(ruleForm[w.key] || 0), 0))
const levelOption = computed(() => {
  const rows = modelStats.value.levelDistribution || []
  return {
    grid: { left: 8, right: 12, top: 16, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: rows.map((r) => r.name + ' 级'), axisLabel: { fontSize: 10, color: '#5b6e77' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#e8eef0' } }, axisLabel: { fontSize: 10, color: '#8a9aa2' } },
    series: [{
      type: 'bar', barWidth: 26,
      data: rows.map((r) => ({
        value: r.value,
        itemStyle: {
          color: { A: '#2d7a58', B: '#237a8b', C: '#a9701f', D: '#b03035' }[r.name] || '#7fa6b0',
          borderRadius: [3, 3, 0, 0]
        }
      }))
    }]
  }
})

async function loadRule() {
  const r = await modelApi.rule()
  Object.assign(rule, r)
  WEIGHTS.forEach((w) => { ruleForm[w.key] = Number(r[w.key]) })
  ruleForm.thresholdA = Number(r.thresholdA)
  ruleForm.thresholdB = Number(r.thresholdB)
  ruleForm.thresholdC = Number(r.thresholdC)
}

async function saveRule() {
  const saved = await modelApi.saveRule({ ...ruleForm, ruleName: rule.ruleName })
  ElMessage.success(`已保存为新版本 ${saved.version}，仅影响后续计算`)
  await loadRule()
  loadStats()
}

async function evaluate() {
  if (!evalForm.supplierId) return ElMessage.warning('请选择供应商')
  const res = await modelApi.evaluate(evalForm)
  ElMessage.success(`计算完成：${res.snapshot.totalScore} 分 · ${res.snapshot.resultLevel} 级`)
  loadSnapshots()
  loadStats()
  openById(res.snapshot.id)
}

async function loadSnapshots() {
  loading.value = true
  try {
    const res = await modelApi.snapshots({ ...query, supplierId: evalForm.supplierId || undefined })
    snapshots.value = res.records
    snapTotal.value = res.total
  } finally {
    loading.value = false
  }
}

async function openById(id) {
  detail.value = await modelApi.snapshot(id)
  adjustForm.level = detail.value.snapshot.finalLevel
  adjustForm.reason = ''
  drawer.value = true
}

const openSnapshot = (row) => openById(row.id)

async function doAdjust() {
  try {
    await modelApi.adjust(detail.value.snapshot.id, adjustForm)
    ElMessage.success('已提交人工修正，原始结果保留')
    drawer.value = false
    loadSnapshots()
    loadStats()
  } catch (e) { /* 已提示 */ }
}

async function loadStats() {
  modelStats.value = await modelApi.stats()
}

onMounted(async () => {
  suppliers.value = await supplierApi.options()
  await loadRule()
  await loadStats()
  await loadSnapshots()
})
</script>

<style scoped>
.cols { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; align-items: start; }
.wrow { display: grid; grid-template-columns: 104px 1fr 56px; gap: 12px; align-items: center; font-size: 13px; }
.wrow .pv { text-align: right; font-variant-numeric: tabular-nums; color: #15344a; font-weight: 600; }
.sum {
  display: flex; align-items: center; gap: 10px; background: #f7fbfc; border: 1px solid #dce5e8;
  border-radius: 6px; padding: 8px 12px; font-size: 12.5px; margin-top: 6px;
}
.sum b { font-size: 15px; color: #15344a; }
.tag-ok { margin-left: auto; color: #2d7a58; background: rgba(45, 122, 88, 0.1); padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.tag-bad { margin-left: auto; color: #b03035; background: rgba(176, 48, 53, 0.1); padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.group-t { font-size: 13.5px; font-weight: 600; margin: 16px 0 10px; padding-left: 9px; border-left: 3px solid #237a8b; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.stat-grid > div { background: #f7fbfc; border: 1px solid #dce5e8; border-radius: 8px; padding: 10px 12px; }
.stat-grid .k { display: block; font-size: 12px; color: #5b6e77; }
.stat-grid b { font-size: 19px; color: #15344a; font-variant-numeric: tabular-nums; }
.score-top { display: flex; align-items: center; gap: 34px; }
.score-top .big-score { font-size: 32px; line-height: 38px; font-weight: 600; color: #15344a; font-variant-numeric: tabular-nums; }
.score-top .big { font-size: 14px; font-weight: 500; }
.explain { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.explain .box { border: 1px solid #dce5e8; border-radius: 8px; padding: 12px 14px; }
.explain h4 { margin: 0 0 8px; font-size: 12.5px; color: #5b6e77; font-weight: 500; }
.explain ul { margin: 0; padding-left: 16px; font-size: 12.5px; line-height: 22px; color: #1c2b33; }
.fi { display: flex; flex-direction: column; gap: 4px; }
.fi label { font-size: 12px; color: #5b6e77; }
</style>
