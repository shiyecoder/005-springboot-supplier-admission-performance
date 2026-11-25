<template>
  <div>
    <div class="page-head">
      <div><h1>绩效评价</h1><p>按周期创建任务，多人独立评分后按规则版本汇总；发布后不可修改，更正需作废重评</p></div>
      <div class="right">
        <el-select v-model="query.period" placeholder="周期" clearable style="width: 150px" @change="search">
          <el-option v-for="p in PERIODS" :key="p" :label="p" :value="p" />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="openCreate">创建评价任务</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="filter-row">
        <el-input v-model="query.kw" placeholder="任务编号 / 供应商" clearable @keyup.enter="search" />
        <el-select v-model="query.status" placeholder="任务状态" clearable>
          <el-option v-for="s in ['待评分', '评分中', '已发布', '已作废']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h2>评价任务</h2>
        <span class="sub">按周期倒序 · 支持多人评分</span>
        <div class="right"><el-tag type="info" effect="plain">共 {{ total }} 条</el-tag></div>
      </div>
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="任务编号" width="160"><template #default="{ row }"><span class="mono">{{ row.taskNo }}</span></template></el-table-column>
        <el-table-column prop="supplierName" label="供应商" min-width="200" show-overflow-tooltip />
        <el-table-column prop="period" label="周期" width="110" />
        <el-table-column prop="periodType" label="类型" width="80" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :text="row.status" /></template></el-table-column>
        <el-table-column label="已提交评分" width="110">
          <template #default="{ row }"><span class="num">{{ row.scoredCount || 0 }}</span> 人</template>
        </el-table-column>
        <el-table-column label="综合得分" width="100">
          <template #default="{ row }"><span class="num">{{ row.avgScore ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column label="等级" width="76"><template #default="{ row }"><LevelBadge :level="row.finalLevel" /></template></el-table-column>
        <el-table-column label="截止日期" width="112"><template #default="{ row }">{{ $date(row.deadline) }}</template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ needScore(row) ? '评分' : '查看' }}</el-button>
            <el-button v-if="row.status !== '已发布' && row.status !== '已作废' && canPublish" link type="primary" @click="publish(row)">发布</el-button>
            <el-button v-if="row.status === '已发布'" link type="danger" @click="voidTask(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="load" @size-change="search" />
      </div>
    </div>

    <el-drawer v-model="drawer" size="760px" :title="detail?.task?.taskNo || '绩效评分卡'">
      <div v-if="detail">
        <div class="panel">
          <div class="card-top">
            <div>
              <div class="sub2">供应商</div>
              <div class="big">{{ detail.task.supplierName }}</div>
            </div>
            <div><div class="sub2">周期</div><div class="big">{{ detail.task.period }}</div></div>
            <div><div class="sub2">状态</div><StatusTag :text="detail.task.status" /></div>
            <div><div class="sub2">已提交</div><div class="big num">{{ detail.submittedCount }} 人</div></div>
            <div><div class="sub2">规则版本</div><div class="big">{{ ruleVersion }}</div></div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-head"><h2>评分明细</h2><span class="sub">质量 30% · 交付 25% · 服务响应 20% · 价格 15% · 投诉 10%</span></div>
          <el-table :data="detail.scores" size="small">
            <el-table-column prop="reviewerName" label="评分人" width="90" />
            <el-table-column prop="qualityScore" label="质量" width="80" />
            <el-table-column prop="deliveryScore" label="交付" width="80" />
            <el-table-column prop="responseScore" label="响应" width="80" />
            <el-table-column prop="priceScore" label="价格" width="80" />
            <el-table-column prop="complaintScore" label="投诉" width="80" />
            <el-table-column prop="totalScore" label="总分" width="80" />
            <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><StatusTag :text="row.status" /></template></el-table-column>
            <el-table-column label="提交时间"><template #default="{ row }">{{ $time(row.updateTime) }}</template></el-table-column>
          </el-table>
        </div>

        <div v-if="editable" class="panel">
          <div class="panel-head"><h2>我的评分</h2><span class="sub">提交后不可修改，如需更正请申请作废重评</span></div>
          <el-row :gutter="12">
            <el-col v-for="f in FIELDS" :key="f.key" :span="8">
              <div class="fi">
                <label>{{ f.label }}</label>
                <el-input-number v-model="score[f.key]" :min="0" :max="100" controls-position="right" style="width: 100%" />
              </div>
            </el-col>
            <el-col :span="8">
              <div class="fi"><label>加权总分</label><div class="score-preview">{{ scoreTotal }}</div></div>
            </el-col>
          </el-row>
          <el-input v-model="score.comment" type="textarea" :rows="3" placeholder="评分说明：交付延迟、质量波动、投诉处理等" />
          <div style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 10px">
            <el-button @click="submitScore(false)">暂存</el-button>
            <el-button type="primary" @click="submitScore(true)">提交评分</el-button>
          </div>
        </div>

        <div v-else class="panel">
          <div class="note">
            当前账号对该任务无可编辑评分：可能已提交、任务已发布，或该任务不属于你的评分范围。
            发布后如需更正，必须由采购专员作废并重新发起评价。
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="createDialog" title="创建评价任务" width="560px">
      <el-form label-width="100px">
        <el-form-item label="供应商" required>
          <el-select v-model="taskForm.supplierId" filterable style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期类型" required>
          <el-select v-model="taskForm.periodType" style="width: 100%">
            <el-option v-for="t in ['月度', '季度', '年度']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期" required>
          <el-select v-model="taskForm.period" allow-create filterable style="width: 100%">
            <el-option v-for="p in PERIODS" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="taskForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="taskForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="评分截止"><el-date-picker v-model="taskForm.deadline" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="taskForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTask">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/StatusTag.vue'
import LevelBadge from '../components/LevelBadge.vue'
import { performanceApi, supplierApi, modelApi } from '../api'
import { store } from '../store/user'

const PERIODS = ['2026 Q4', '2026 Q3', '2026 Q2', '2026 Q1', '2026 年 10 月', '2026 年 9 月']
const FIELDS = [
  { key: 'qualityScore', label: '质量合格率得分' },
  { key: 'deliveryScore', label: '准时交付率得分' },
  { key: 'responseScore', label: '服务响应得分' },
  { key: 'priceScore', label: '价格稳定性得分' },
  { key: 'complaintScore', label: '投诉与整改得分' }
]

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, kw: '', period: '', status: '' })
const drawer = ref(false)
const detail = ref(null)
const suppliers = ref([])
const ruleVersion = ref('—')
const score = reactive({ qualityScore: 90, deliveryScore: 88, responseScore: 86, priceScore: 80, complaintScore: 90, comment: '' })
const createDialog = ref(false)
const taskForm = reactive({})

const canPublish = computed(() => ['admin', 'buyer'].includes(store.user?.roleCode))
const editable = computed(() => {
  if (!detail.value) return false
  const t = detail.value.task
  return ['待评分', '评分中'].includes(t.status) && !['admin'].includes(store.user?.roleCode)
})
const needScore = (row) => ['待评分', '评分中'].includes(row.status) && store.user?.roleCode !== 'admin'
const scoreTotal = computed(() => {
  const s = score
  return (s.qualityScore * 0.3 + s.deliveryScore * 0.25 + s.responseScore * 0.2 + s.priceScore * 0.15 + s.complaintScore * 0.1).toFixed(1)
})

async function load() {
  loading.value = true
  try {
    const res = await performanceApi.page(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }
const reset = () => { Object.assign(query, { page: 1, kw: '', period: '', status: '' }); load() }

async function openDetail(row) {
  detail.value = await performanceApi.detail(row.id)
  if (detail.value.mine) {
    Object.assign(score, {
      qualityScore: Number(detail.value.mine.qualityScore),
      deliveryScore: Number(detail.value.mine.deliveryScore),
      responseScore: Number(detail.value.mine.responseScore),
      priceScore: Number(detail.value.mine.priceScore),
      complaintScore: Number(detail.value.mine.complaintScore),
      comment: detail.value.mine.comment || ''
    })
  }
  drawer.value = true
}

async function submitScore(submit) {
  try {
    const total = await performanceApi.score(detail.value.task.id, { ...score, submit })
    ElMessage.success(`已${submit ? '提交' : '暂存'}，加权总分 ${total}`)
    detail.value = await performanceApi.detail(detail.value.task.id)
    load()
  } catch (e) { /* 已提示 */ }
}

async function publish(row) {
  try {
    await ElMessageBox.confirm(`发布后整单锁定，不可再修改。确认发布 ${row.taskNo}？`, '发布评价', { type: 'warning' })
    const res = await performanceApi.publish(row.id)
    ElMessage.success(`已发布：均分 ${res.avgScore}，等级 ${res.level}${res.rectificationId ? '，已自动生成整改任务' : ''}`)
    load()
  } catch (e) { /* 取消 */ }
}

async function voidTask(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写作废原因（不少于 5 个字）', '作废评价', { inputType: 'textarea' })
    await performanceApi.voidTask(row.id, { reason: value })
    ElMessage.success('已作废，可重新发起评价')
    load()
  } catch (e) { /* 取消 */ }
}

function openCreate() {
  Object.keys(taskForm).forEach((k) => delete taskForm[k])
  Object.assign(taskForm, { periodType: '季度', period: '2026 Q4' })
  createDialog.value = true
}

async function saveTask() {
  if (!taskForm.supplierId || !taskForm.period) return ElMessage.warning('请选择供应商与周期')
  await performanceApi.create(taskForm)
  ElMessage.success('评价任务已创建')
  createDialog.value = false
  load()
}

onMounted(async () => {
  suppliers.value = await supplierApi.options()
  try {
    ruleVersion.value = (await modelApi.rule()).version
  } catch (e) { ruleVersion.value = '未配置' }
  load()
})
</script>

<style scoped>
.card-top { display: flex; gap: 32px; align-items: flex-end; }
.card-top .big { font-size: 15px; font-weight: 600; color: #1c2b33; }
.fi { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.fi label { font-size: 12px; color: #5b6e77; }
.score-preview { font-size: 22px; font-weight: 600; color: #15344a; line-height: 32px; font-variant-numeric: tabular-nums; }
</style>
