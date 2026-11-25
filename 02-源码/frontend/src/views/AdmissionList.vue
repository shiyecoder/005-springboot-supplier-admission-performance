<template>
  <div>
    <div class="page-head">
      <div><h1>准入申请</h1><p>待办优先、更新时间倒序 · 形式审查与专业评审在同一列表按阶段区分</p></div>
      <div class="right">
        <el-button :icon="Download" @click="exportCsv">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建申请</el-button>
      </div>
    </div>

    <div class="panel">
      <el-tabs v-model="tab" @tab-change="search">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待办" name="todo" />
        <el-tab-pane label="处理中" name="doing" />
        <el-tab-pane label="已完成" name="done" />
        <el-tab-pane label="异常" name="abnormal" />
      </el-tabs>
      <div class="filter-row">
        <el-input v-model="query.kw" placeholder="申请编号 / 企业名称 / 信用代码" clearable @keyup.enter="search" />
        <el-select v-model="query.categoryId" placeholder="供应类别" clearable>
          <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="当前阶段" clearable>
          <el-option v-for="s in STATUS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h2>申请清单</h2>
        <span class="sub">材料完整度按类目必填证照数计算</span>
        <div class="right">
          <el-button size="small" :disabled="!selected.length" @click="batchReview">批量提交下一节点</el-button>
          <el-tag type="info" effect="plain">已选 {{ selected.length }} · 共 {{ total }} 条</el-tag>
        </div>
      </div>
      <el-table :data="rows" v-loading="loading" @selection-change="(v) => (selected = v)" style="width: 100%">
        <el-table-column type="selection" width="42" />
        <el-table-column label="申请编号" width="150">
          <template #default="{ row }"><span class="mono">{{ row.applicationNo }}</span></template>
        </el-table-column>
        <el-table-column label="企业名称" min-width="220">
          <template #default="{ row }">
            <div>{{ row.supplierName }}</div>
            <div class="sub2">{{ row.creditCode }} · {{ row.contactName }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="供应类别" width="100" />
        <el-table-column label="当前阶段" width="108">
          <template #default="{ row }"><StatusTag :text="row.status" /></template>
        </el-table-column>
        <el-table-column label="材料完整度" width="150">
          <template #default="{ row }">
            <div class="bar"><i :style="{ width: pct(row) + '%', background: pct(row) >= 100 ? '#2d7a58' : '#a9701f' }" /></div>
            <span class="sub2 num">{{ row.certUploaded || 0 }}/{{ row.certRequired || 6 }} 必填</span>
          </template>
        </el-table-column>
        <el-table-column label="评审均分" width="96">
          <template #default="{ row }"><span class="num">{{ row.avgScore ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="reviewerName" label="评审人" width="90" />
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ $time(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button v-if="canReview(row)" link type="primary" @click="openReview(row)">评分</el-button>
            <el-button v-else-if="nextActions(row).length" link type="primary" @click="openDetail(row)">处理</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty">当前筛选条件下无记录，<el-link type="primary" @click="reset">清空筛选</el-link> 或新建准入申请</div>
        </template>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-drawer v-model="detailDrawer" size="880px" :title="detail?.application?.applicationNo || '准入详情'">
      <template #header>
        <div class="drawer-title">
          <b>{{ detail?.application?.applicationNo }}</b>
          <StatusTag v-if="detail" :text="detail.application.status" />
          <span class="sub2">{{ detail?.application?.supplierName }}</span>
        </div>
      </template>
      <div v-if="detail">
        <div class="panel">
          <el-descriptions :column="3" size="small" border>
            <el-descriptions-item label="供应类别">{{ detail.application.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="统一信用代码"><span class="mono">{{ detail.application.creditCode }}</span></el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ detail.application.currentNode }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ detail.application.contactName }} · {{ detail.application.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ $time(detail.application.submitTime) }}</el-descriptions-item>
            <el-descriptions-item label="截止时间">{{ $time(detail.application.deadline) }}</el-descriptions-item>
            <el-descriptions-item label="年产能">{{ detail.application.annualCapacity || '—' }}</el-descriptions-item>
            <el-descriptions-item label="主要客户" :span="2">{{ detail.application.mainCustomers || '—' }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.application.rejectReason" label="驳回原因" :span="3">
              <span style="color: #b03035">{{ detail.application.rejectReason }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="panel">
          <div class="panel-head">
            <h2>材料核验</h2>
            <span class="sub">要求 {{ detail.requiredCerts.length }} 项 · 已上传 {{ detail.certificates.length }} 项</span>
          </div>
          <div v-if="detail.requiredCerts.length" class="cert-check">
            <div v-for="name in detail.requiredCerts" :key="name" class="cc-item">
              <el-icon :class="hasCert(name) ? 'ok' : 'miss'"><component :is="hasCert(name) ? 'CircleCheck' : 'CircleClose'" /></el-icon>
              <span>{{ name.trim() }}</span>
              <span class="sub2">{{ hasCert(name) ? '已上传' : '缺失' }}</span>
            </div>
          </div>
          <el-table :data="detail.certificates" size="small" style="margin-top: 10px">
            <el-table-column prop="certType" label="证照类型" min-width="150" />
            <el-table-column prop="certNo" label="编号" min-width="160" />
            <el-table-column label="到期" width="110"><template #default="{ row }">{{ $date(row.expireDate) }}</template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :text="row.certStatus" /></template></el-table-column>
            <el-table-column label="附件" width="80">
              <template #default="{ row }"><el-link type="primary" :href="row.filePath" target="_blank">查看</el-link></template>
            </el-table-column>
          </el-table>
        </div>

        <div class="panel">
          <div class="panel-head"><h2>专业评审</h2><span class="sub">多人独立评分，提交后不可修改</span></div>
          <el-table :data="detail.scores" size="small" empty-text="暂无评审记录，指派评审人后可查看打分">
            <el-table-column prop="reviewerName" label="评分人" width="90" />
            <el-table-column prop="qualityScore" label="质量 30%" width="96" />
            <el-table-column prop="techScore" label="技术 25%" width="96" />
            <el-table-column prop="deliveryScore" label="交付 20%" width="96" />
            <el-table-column prop="priceScore" label="价格 15%" width="96" />
            <el-table-column prop="serviceScore" label="服务 10%" width="96" />
            <el-table-column prop="totalScore" label="加权总分" width="96" />
            <el-table-column prop="conclusion" label="结论" width="100" />
            <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><StatusTag :text="row.status" /></template></el-table-column>
          </el-table>

          <div v-if="canReview(detail.application)" class="review-form">
            <div class="group-t">我的评分</div>
            <el-row :gutter="12">
              <el-col :span="8"><div class="fi"><label>质量保证 30%</label><el-input-number v-model="review.qualityScore" :min="0" :max="100" controls-position="right" style="width: 100%" /></div></el-col>
              <el-col :span="8"><div class="fi"><label>技术能力 25%</label><el-input-number v-model="review.techScore" :min="0" :max="100" controls-position="right" style="width: 100%" /></div></el-col>
              <el-col :span="8"><div class="fi"><label>交付保障 20%</label><el-input-number v-model="review.deliveryScore" :min="0" :max="100" controls-position="right" style="width: 100%" /></div></el-col>
              <el-col :span="8"><div class="fi"><label>价格竞争 15%</label><el-input-number v-model="review.priceScore" :min="0" :max="100" controls-position="right" style="width: 100%" /></div></el-col>
              <el-col :span="8"><div class="fi"><label>服务响应 10%</label><el-input-number v-model="review.serviceScore" :min="0" :max="100" controls-position="right" style="width: 100%" /></div></el-col>
              <el-col :span="8"><div class="fi"><label>加权总分</label><div class="score-preview">{{ reviewTotal }}</div></div></el-col>
            </el-row>
            <el-input v-model="review.comment" type="textarea" :rows="3" placeholder="评审意见：资质核验结论、存疑项与建议" style="margin-top: 10px" />
            <div style="display: flex; gap: 8px; margin-top: 10px; justify-content: flex-end">
              <el-button @click="submitReview(false)">保存草稿</el-button>
              <el-button type="primary" @click="submitReview(true)">提交评分</el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDrawer = false">关闭</el-button>
        <el-button
          v-for="a in nextActions(detail?.application || {})"
          :key="a.to"
          :type="a.primary ? 'primary' : 'default'"
          :class="{ 'danger-btn': a.danger }"
          @click="doTransition(a.to, a.needReason, a.label)"
        >{{ a.label }}</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="reasonDialog" :title="reasonTitle" width="480px">
      <div class="warnbox" style="margin-bottom: 12px">退回、驳回必须填写不少于 5 个字的原因，操作会写入时间轴。</div>
      <el-input v-model="reasonText" type="textarea" :rows="4" placeholder="请说明原因" />
      <template #footer>
        <el-button @click="reasonDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmReason">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createDialog" title="新建准入申请" width="720px">
      <el-form ref="createRef" :model="applyForm" :rules="applyRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="企业名称" prop="supplierName"><el-input v-model="applyForm.supplierName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="统一信用代码" prop="creditCode"><el-input v-model="applyForm.creditCode" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="供应类别" prop="categoryId">
              <el-select v-model="applyForm.categoryId" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="联系人" prop="contactName"><el-input v-model="applyForm.contactName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话" prop="contactPhone"><el-input v-model="applyForm.contactPhone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="年产能"><el-input v-model="applyForm.annualCapacity" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="注册地址"><el-input v-model="applyForm.address" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="主要客户"><el-input v-model="applyForm.mainCustomers" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="产能说明"><el-input v-model="applyForm.capacityDesc" type="textarea" :rows="3" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveApply">保存草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/StatusTag.vue'
import { admissionApi, supplierApi } from '../api'
import { store } from '../store/user'

const STATUS = ['待提交', '准入审核', '现场评估', '待审批', '已准入', '已驳回', '合作评价', '分级调整']
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const tab = ref('all')
const categories = ref([])
const selected = ref([])
const query = reactive({ page: 1, size: 10, kw: '', categoryId: null, status: '' })

const detailDrawer = ref(false)
const detail = ref(null)
const review = reactive({ qualityScore: 85, techScore: 82, deliveryScore: 80, priceScore: 78, serviceScore: 88, comment: '' })
const reasonDialog = ref(false)
const reasonTitle = ref('')
const reasonText = ref('')
const pending = ref(null)
const createDialog = ref(false)
const saving = ref(false)
const createRef = ref()
const applyForm = reactive({})

const applyRules = {
  supplierName: [{ required: true, message: '必填', trigger: 'blur' }],
  creditCode: [{ required: true, message: '必填', trigger: 'blur' }],
  categoryId: [{ required: true, message: '必选', trigger: 'change' }],
  contactName: [{ required: true, message: '必填', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '必填', trigger: 'blur' }]
}

const reviewTotal = computed(() => {
  const r = review
  return (r.qualityScore * 0.3 + r.techScore * 0.25 + r.deliveryScore * 0.2 + r.priceScore * 0.15 + r.serviceScore * 0.1).toFixed(1)
})

const pct = (row) => Math.min(100, Math.round(((row.certUploaded || 0) / (row.certRequired || 6)) * 100))

function hasCert(name) {
  if (!detail.value) return false
  return detail.value.certificates.some((c) => c.certType === name.trim() && c.certStatus !== '已作废')
}

function canReview(row) {
  if (!row || !row.status) return false
  const roleOk = ['reviewer', 'admin', 'buyer'].includes(store.user?.roleCode)
  return roleOk && ['准入审核', '现场评估'].includes(row.status)
}

// 按角色与状态计算可执行的流转动作，前端只做展示，后端仍会二次校验
function nextActions(row) {
  if (!row || !row.status) return []
  const role = store.user?.roleCode
  const s = row.status
  if (s === '待提交') return [{ to: '准入审核', label: '提交申请', primary: true }]
  if (s === '准入审核' && role !== 'reviewer') {
    return [
      { to: '现场评估', label: '通过形式审查', primary: true },
      { to: '待提交', label: '退回补充', needReason: true, danger: true }
    ]
  }
  if (s === '现场评估') {
    return [
      { to: '待审批', label: '提交审批', primary: true },
      { to: '准入审核', label: '退回重新核验', needReason: true, danger: true }
    ]
  }
  if (s === '待审批' && role === 'admin') {
    return [
      { to: '已准入', label: '审批准入', primary: true },
      { to: '已驳回', label: '驳回', needReason: true, danger: true }
    ]
  }
  if (s === '已准入') return [{ to: '合作评价', label: '进入合作评价' }]
  if (s === '合作评价') return [{ to: '分级调整', label: '转分级调整' }]
  return []
}

async function load() {
  loading.value = true
  try {
    const res = await admissionApi.page({ ...query, tab: tab.value })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const search = () => { query.page = 1; load() }
const reset = () => {
  Object.assign(query, { page: 1, kw: '', categoryId: null, status: '' })
  tab.value = 'all'
  load()
}

async function openDetail(row) {
  detail.value = await admissionApi.detail(row.id)
  detailDrawer.value = true
}

async function openReview(row) {
  await openDetail(row)
}

async function submitReview(submit) {
  try {
    const total = await admissionApi.review(detail.value.application.id, { ...review, submit })
    ElMessage.success(`已${submit ? '提交' : '保存'}评分，加权总分 ${total}`)
    detail.value = await admissionApi.detail(detail.value.application.id)
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

function doTransition(target, needReason, label) {
  pending.value = { id: detail.value.application.id, target }
  if (needReason) {
    reasonTitle.value = label
    reasonText.value = ''
    reasonDialog.value = true
    return
  }
  ElMessageBox.confirm(`确认执行「${label}」？`, '状态流转确认', { type: 'warning' }).then(async () => {
    await admissionApi.transition(pending.value.id, { targetStatus: target, reason: '' })
    ElMessage.success('已流转')
    detail.value = await admissionApi.detail(pending.value.id)
    load()
  })
}

async function confirmReason() {
  if (reasonText.value.trim().length < 5) return ElMessage.warning('原因不少于 5 个字')
  await admissionApi.transition(pending.value.id, { targetStatus: pending.value.target, reason: reasonText.value })
  ElMessage.success('已流转')
  reasonDialog.value = false
  detail.value = await admissionApi.detail(pending.value.id)
  load()
}

async function batchReview() {
  if (!selected.value.length) return ElMessage.warning('请先勾选数据')
  const invalid = selected.value.filter((r) => r.status !== '准入审核')
  if (invalid.length) return ElMessage.warning(`有 ${invalid.length} 条不在「准入审核」阶段，不能批量提交`)
  try {
    await ElMessageBox.confirm(`将对 ${selected.value.length} 条申请执行通过形式审查，确认继续？`, '批量操作确认', { type: 'warning' })
    const results = []
    for (const row of selected.value) {
      try {
        await admissionApi.transition(row.id, { targetStatus: '现场评估', reason: '' })
        results.push({ no: row.applicationNo, ok: true })
      } catch (e) {
        results.push({ no: row.applicationNo, ok: false, msg: e.message })
      }
    }
    const fail = results.filter((r) => !r.ok)
    ElMessage[fail.length ? 'warning' : 'success'](`成功 ${results.length - fail.length} 条${fail.length ? `，失败 ${fail.length} 条：${fail.map((f) => f.no).join('、')}` : ''}`)
    load()
  } catch (e) { /* 取消 */ }
}

function openCreate() {
  Object.keys(applyForm).forEach((k) => delete applyForm[k])
  Object.assign(applyForm, { supplierId: null })
  createDialog.value = true
}

async function saveApply() {
  await createRef.value.validate()
  saving.value = true
  try {
    await admissionApi.create(applyForm)
    ElMessage.success('草稿已保存，可在列表中提交')
    createDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

function exportCsv() {
  if (!rows.value.length) return ElMessage.warning('没有可导出数据')
  const head = ['申请编号', '企业名称', '供应类别', '当前阶段', '评审均分', '提交时间']
  const body = rows.value.map((r) => [r.applicationNo, r.supplierName, r.categoryName, r.status, r.avgScore ?? '', r.submitTime ?? ''])
  const csv = '\ufeff' + [head, ...body].map((r) => r.join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `准入申请_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  categories.value = await supplierApi.categories()
  load()
})
</script>

<style scoped>
.bar { height: 6px; border-radius: 3px; background: #edf2f4; overflow: hidden; margin-bottom: 2px; }
.bar i { display: block; height: 100%; border-radius: 3px; }
.empty { padding: 26px 0; color: #8a9aa2; font-size: 13px; }
.drawer-title { display: flex; align-items: center; gap: 10px; }
.drawer-title b { font-family: ui-monospace, Menlo, monospace; font-size: 15px; }
.cert-check { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.cc-item {
  display: flex; align-items: center; gap: 6px; font-size: 12.5px;
  background: #fbfdfd; border: 1px solid #e8eef0; border-radius: 6px; padding: 7px 10px;
}
.cc-item .ok { color: #2d7a58; }
.cc-item .miss { color: #b03035; }
.cc-item .sub2 { margin-left: auto; }
.review-form { margin-top: 16px; border-top: 1px dashed #dce5e8; padding-top: 12px; }
.fi { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.fi label { font-size: 12px; color: #5b6e77; }
.score-preview { font-size: 22px; font-weight: 600; color: #15344a; line-height: 32px; font-variant-numeric: tabular-nums; }
.danger-btn { color: #b03035; border-color: #b03035; }
</style>
