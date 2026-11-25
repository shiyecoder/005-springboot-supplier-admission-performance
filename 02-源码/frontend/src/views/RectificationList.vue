<template>
  <div>
    <div class="page-head">
      <div><h1>整改管理</h1><p>低于阈值自动生成整改任务，供应商提交措施，采购专员验证后关闭或退回</p></div>
      <div class="right"><el-button type="primary" :icon="Plus" @click="openCreate">新建整改任务</el-button></div>
    </div>

    <div class="metric-grid" style="grid-template-columns: repeat(4, 1fr)">
      <div class="metric"><div class="k">待提交措施</div><div class="v" style="color: #a9701f">{{ stats.pendingMeasure || 0 }}<small> 条</small></div><div class="d">超期 {{ stats.overdue || 0 }} 条</div></div>
      <div class="metric"><div class="k">待验证</div><div class="v">{{ stats.pendingVerify || 0 }}<small> 条</small></div><div class="d">需采购专员处理</div></div>
      <div class="metric"><div class="k">已关闭</div><div class="v" style="color: #2d7a58">{{ stats.closed || 0 }}<small> 条</small></div><div class="d">完成率 {{ stats.completionRate || 0 }}%</div></div>
      <div class="metric"><div class="k">已逾期</div><div class="v" style="color: #b03035">{{ stats.overdue || 0 }}<small> 条</small></div><div class="d">逾期 7 天可升级审批</div></div>
    </div>

    <div class="panel" style="margin-top: 12px">
      <div class="filter-row">
        <el-input v-model="query.kw" placeholder="整改编号 / 供应商" clearable @keyup.enter="search" />
        <el-select v-model="query.status" placeholder="整改状态" clearable>
          <el-option v-for="s in ['待提交措施', '待验证', '已关闭', '已逾期', '已转黑名单']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="query.source" placeholder="问题来源" clearable>
          <el-option v-for="s in ['绩效评价', '证照过期', '现场评审', '投诉']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head"><h2>整改任务</h2><span class="sub">按截止日期升序</span><div class="right"><el-tag type="info" effect="plain">共 {{ total }} 条</el-tag></div></div>
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="整改编号" width="150"><template #default="{ row }"><span class="mono">{{ row.rectificationNo }}</span></template></el-table-column>
        <el-table-column prop="supplierName" label="供应商" min-width="180" show-overflow-tooltip />
        <el-table-column prop="source" label="问题来源" width="110" />
        <el-table-column prop="issueDesc" label="问题摘要" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag :text="row.status" /></template></el-table-column>
        <el-table-column label="截止日期" width="112"><template #default="{ row }">{{ $date(row.deadline) }}</template></el-table-column>
        <el-table-column label="剩余" width="90">
          <template #default="{ row }">
            <span class="num" :style="{ color: days(row.deadline) < 0 ? '#b03035' : days(row.deadline) <= 5 ? '#a9701f' : '#5b6e77' }">
              {{ days(row.deadline) < 0 ? days(row.deadline) : days(row.deadline) + ' 天' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ row.status === '待验证' ? '验证' : '详情' }}</el-button>
            <el-button v-if="['待提交措施', '已逾期'].includes(row.status) && canVerify" link type="danger" @click="toBlacklist(row)">黑名单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="load" @size-change="search" />
      </div>
    </div>

    <el-drawer v-model="drawer" size="680px" :title="current?.rectificationNo || '整改详情'">
      <div v-if="current">
        <div class="panel">
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="供应商">{{ current.supplierName }}</el-descriptions-item>
            <el-descriptions-item label="问题来源">{{ current.source }}</el-descriptions-item>
            <el-descriptions-item label="状态"><StatusTag :text="current.status" /></el-descriptions-item>
            <el-descriptions-item label="截止日期">{{ $date(current.deadline) }}</el-descriptions-item>
            <el-descriptions-item label="问题描述" :span="2">{{ current.issueDesc }}</el-descriptions-item>
            <el-descriptions-item label="整改措施" :span="2">{{ current.measure || '供应商尚未提交' }}</el-descriptions-item>
            <el-descriptions-item v-if="current.verifyComment" label="验证意见" :span="2">{{ current.verifyComment }}</el-descriptions-item>
            <el-descriptions-item v-if="current.verifyTime" label="验证时间">{{ $time(current.verifyTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-if="current.status === '待验证' && canVerify" class="panel">
          <div class="panel-head"><h2>执行验证</h2><span class="sub">验证意见不少于 5 个字</span></div>
          <el-form label-width="90px">
            <el-form-item label="验证结论">
              <el-radio-group v-model="verifyForm.conclusion">
                <el-radio-button label="通过" />
                <el-radio-button label="不通过" />
                <el-radio-button label="延期" />
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="verifyForm.conclusion === '延期'" label="新截止日">
              <el-date-picker v-model="verifyForm.newDeadline" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="验证意见">
              <el-input v-model="verifyForm.comment" type="textarea" :rows="3" placeholder="如：9-10 月交付及时率已回升至 94%，同意关闭" />
            </el-form-item>
            <div style="display: flex; justify-content: flex-end"><el-button type="primary" @click="doVerify">提交验证</el-button></div>
          </el-form>
        </div>

        <div v-if="['待提交措施', '已逾期'].includes(current.status) && isSupplierSelf" class="panel">
          <div class="panel-head"><h2>提交整改措施</h2></div>
          <el-input v-model="measureText" type="textarea" :rows="4" placeholder="请说明具体措施、责任人与完成时间" />
          <div style="display: flex; justify-content: flex-end; margin-top: 10px"><el-button type="primary" @click="doMeasure">提交措施</el-button></div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="createDialog" title="新建整改任务" width="560px">
      <el-form label-width="100px">
        <el-form-item label="供应商" required>
          <el-select v-model="createForm.supplierId" filterable style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题来源">
          <el-select v-model="createForm.source" style="width: 100%">
            <el-option v-for="s in ['绩效评价', '证照过期', '现场评审', '投诉']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" required><el-input v-model="createForm.issueDesc" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="截止日期"><el-date-picker v-model="createForm.deadline" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/StatusTag.vue'
import { rectApi, supplierApi } from '../api'
import { store } from '../store/user'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const suppliers = ref([])
const query = reactive({ page: 1, size: 10, kw: '', status: '', source: '' })
const drawer = ref(false)
const current = ref(null)
const measureText = ref('')
const verifyForm = reactive({ conclusion: '通过', comment: '', newDeadline: '' })
const createDialog = ref(false)
const createForm = reactive({})

const canVerify = computed(() => ['admin', 'buyer'].includes(store.user?.roleCode))
const isSupplierSelf = computed(() => store.user?.roleCode === 'supplier')
const days = (d) => (d ? Math.round((new Date(d) - new Date()) / 86400000) : '—')

async function load() {
  loading.value = true
  try {
    const res = await rectApi.page(query)
    rows.value = res.records
    total.value = res.total
    stats.value = await rectApi.stats()
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }
const reset = () => { Object.assign(query, { page: 1, kw: '', status: '', source: '' }); load() }

function openDetail(row) {
  current.value = row
  measureText.value = row.measure || ''
  verifyForm.conclusion = '通过'
  verifyForm.comment = ''
  verifyForm.newDeadline = ''
  drawer.value = true
}

async function doVerify() {
  try {
    await rectApi.verify(current.value.id, verifyForm)
    ElMessage.success('验证已提交')
    drawer.value = false
    load()
  } catch (e) { /* 已提示 */ }
}

async function doMeasure() {
  try {
    await rectApi.measure(current.value.id, { measure: measureText.value })
    ElMessage.success('措施已提交，等待采购专员验证')
    drawer.value = false
    load()
  } catch (e) { /* 已提示 */ }
}

function openCreate() {
  Object.keys(createForm).forEach((k) => delete createForm[k])
  createForm.source = '绩效评价'
  createDialog.value = true
}

async function saveCreate() {
  if (!createForm.supplierId || !createForm.issueDesc) return ElMessage.warning('供应商与问题描述为必填')
  await rectApi.create(createForm)
  ElMessage.success('整改任务已创建')
  createDialog.value = false
  load()
}

async function toBlacklist(row) {
  try {
    const { value } = await ElMessageBox.prompt('列入黑名单必须记录原因与期限，请填写原因（不少于 5 个字）', '转为黑名单', { inputType: 'textarea' })
    await rectApi.blacklist(row.id, { reason: value })
    ElMessage.success('已列入黑名单，供应商状态同步更新')
    load()
  } catch (e) { /* 取消 */ }
}

onMounted(async () => {
  suppliers.value = await supplierApi.options()
  load()
})
</script>
