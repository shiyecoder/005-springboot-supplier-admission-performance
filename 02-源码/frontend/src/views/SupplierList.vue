<template>
  <div>
    <div class="page-head">
      <div>
        <h1>供应商名录</h1>
        <p>已准入与潜在供应商主档，共 {{ total }} 家 · 等级与风险来自最近一次评价</p>
      </div>
      <div class="right">
        <el-button :icon="Download" @click="exportCsv">导出筛选结果</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建供应商</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="filter-row">
        <el-input v-model="query.kw" placeholder="企业名称 / 编码 / 信用代码" clearable @keyup.enter="search" />
        <el-select v-model="query.categoryId" placeholder="供应类别" clearable>
          <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
        <el-select v-model="query.supplierStatus" placeholder="合作状态" clearable>
          <el-option v-for="s in ['潜在', '合格', '暂停', '限期整改', '黑名单', '已退出']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="query.level" placeholder="等级" clearable style="width: 120px">
          <el-option v-for="s in ['A', 'B', 'C', 'D']" :key="s" :label="s + ' 级'" :value="s" />
        </el-select>
        <el-select v-model="query.riskLevel" placeholder="风险等级" clearable style="width: 140px">
          <el-option v-for="s in ['低', '中', '高']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-date-picker v-model="certRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="资质到期起" end-placeholder="止" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h2>供应商列表</h2>
        <span class="sub">按更新时间倒序 · 服务端分页</span>
        <div class="right"><el-tag type="info" effect="plain">共 {{ total }} 条</el-tag></div>
      </div>
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="企业" min-width="260">
          <template #default="{ row }">
            <div class="ent">
              <img :src="row.coverImage || '/uploads/seed/supplier/01.jpg'" alt="厂区" />
              <div>
                <div class="nm">{{ row.supplierName }}</div>
                <div class="sub2">{{ row.supplierCode }} · {{ row.creditCode }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="供应类别" min-width="100" />
        <el-table-column label="等级" width="72">
          <template #default="{ row }"><LevelBadge :level="row.level" /></template>
        </el-table-column>
        <el-table-column label="合作状态" width="110">
          <template #default="{ row }"><StatusTag :text="row.supplierStatus" /></template>
        </el-table-column>
        <el-table-column label="风险等级" width="96">
          <template #default="{ row }"><StatusTag :text="row.riskLevel" /></template>
        </el-table-column>
        <el-table-column label="证照" width="96">
          <template #default="{ row }">
            <span class="num">{{ row.certCount || 0 }} 张</span>
            <span v-if="row.expiringCertCount" class="sub2" style="color: #b03035"> · 过期 {{ row.expiringCertCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="资质到期" width="112">
          <template #default="{ row }">{{ $date(row.certExpireDate) }}</template>
        </el-table-column>
        <el-table-column prop="ownerName" label="责任人" width="90" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/supplier/${row.id}`)">查看</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd) => onCommand(cmd, row)">
              <el-button link type="primary">更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="suspend">暂停合作</el-dropdown-item>
                  <el-dropdown-item command="resume">恢复合格</el-dropdown-item>
                  <el-dropdown-item command="blacklist" divided>列入黑名单</el-dropdown-item>
                  <el-dropdown-item command="quit">标记已退出</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty">
            当前筛选条件下无记录，<el-link type="primary" @click="reset">清空筛选</el-link> 或
            <el-link type="primary" @click="openCreate">新建供应商</el-link>
          </div>
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

    <el-drawer v-model="drawer" :title="form.id ? '编辑供应商' : '新建供应商'" size="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="供应商编码" prop="supplierCode"><el-input v-model="form.supplierCode" placeholder="不超过 50 字符" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="企业名称" prop="supplierName"><el-input v-model="form.supplierName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="统一信用代码" prop="creditCode"><el-input v-model="form.creditCode" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="供应类别" prop="categoryId">
              <el-select v-model="form.categoryId" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="联系人" prop="contactName"><el-input v-model="form.contactName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="合作状态">
              <el-select v-model="form.supplierStatus" style="width: 100%">
                <el-option v-for="s in ['潜在', '合格', '暂停', '限期整改', '黑名单', '已退出']" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="等级">
              <el-select v-model="form.level" style="width: 100%">
                <el-option v-for="s in ['A', 'B', 'C', 'D']" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级">
              <el-select v-model="form.riskLevel" style="width: 100%">
                <el-option v-for="s in ['低', '中', '高']" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="准入日期"><el-date-picker v-model="form.admissionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="资质到期日"><el-date-picker v-model="form.certExpireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="年产能"><el-input v-model="form.annualCapacity" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="主要客户"><el-input v-model="form.mainCustomers" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="注册地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="产能说明"><el-input v-model="form.capacityDesc" type="textarea" :rows="3" /></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="厂区照片">
              <UploadField v-model="form.coverImage" biz-type="supplier" tip="存相对路径，禁止外链；建议 1024x768" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="statusDialog" title="变更合作状态" width="480px">
      <div class="warnbox" style="margin-bottom: 14px">状态变更会写入操作日志，黑名单与暂停必须填写原因（不少于 5 个字）。</div>
      <el-form label-width="90px">
        <el-form-item label="目标状态"><el-tag>{{ statusTarget }}</el-tag></el-form-item>
        <el-form-item label="变更原因"><el-input v-model="statusReason" type="textarea" :rows="3" placeholder="请说明业务判断依据" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialog = false">取消</el-button>
        <el-button type="danger" @click="doChangeStatus">确认变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Search } from '@element-plus/icons-vue'
import LevelBadge from '../components/LevelBadge.vue'
import StatusTag from '../components/StatusTag.vue'
import UploadField from '../components/UploadField.vue'
import { supplierApi } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])
const certRange = ref([])
const query = reactive({ page: 1, size: 10, kw: '', categoryId: null, supplierStatus: '', level: '', riskLevel: '' })

const drawer = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({})
const statusDialog = ref(false)
const statusTarget = ref('')
const statusReason = ref('')
const statusRow = ref(null)

const rules = {
  supplierCode: [{ required: true, message: '必填', trigger: 'blur' }],
  supplierName: [{ required: true, message: '必填', trigger: 'blur' }],
  creditCode: [{ required: true, message: '必填', trigger: 'blur' }],
  categoryId: [{ required: true, message: '必选', trigger: 'change' }],
  contactName: [{ required: true, message: '必填', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const params = {
      ...query,
      certStart: certRange.value?.[0],
      certEnd: certRange.value?.[1]
    }
    const res = await supplierApi.page(params)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  Object.assign(query, { page: 1, kw: '', categoryId: null, supplierStatus: '', level: '', riskLevel: '' })
  certRange.value = []
  load()
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    supplierCode: 'SUP-2026-' + String(Math.floor(Math.random() * 900) + 100),
    supplierStatus: '潜在', level: 'C', riskLevel: '低',
    coverImage: '/uploads/seed/supplier/01.jpg'
  })
  drawer.value = true
}

async function openEdit(row) {
  const detail = await supplierApi.detail(row.id)
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, detail.supplier)
  drawer.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await supplierApi.update(form.id, form)
      ElMessage.success('已保存')
    } else {
      await supplierApi.create(form)
      ElMessage.success('已创建')
    }
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}

function onCommand(cmd, row) {
  const map = { suspend: '暂停', resume: '合格', blacklist: '黑名单', quit: '已退出' }
  statusTarget.value = map[cmd]
  statusRow.value = row
  statusReason.value = ''
  if (cmd === 'resume') {
    doChangeStatus()
    return
  }
  statusDialog.value = true
}

async function doChangeStatus() {
  try {
    await supplierApi.changeStatus(statusRow.value.id, { status: statusTarget.value, reason: statusReason.value })
    ElMessage.success('状态已更新')
    statusDialog.value = false
    load()
  } catch (e) { /* 提示已由拦截器给出 */ }
}

function exportCsv() {
  if (!rows.value.length) return ElMessage.warning('当前没有可导出的数据')
  const head = ['供应商编码', '企业名称', '供应类别', '等级', '合作状态', '风险等级', '资质到期']
  const body = rows.value.map((r) => [r.supplierCode, r.supplierName, r.categoryName, r.level, r.supplierStatus, r.riskLevel, r.certExpireDate])
  const csv = '\ufeff' + [head, ...body].map((r) => r.join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `供应商名录_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出当前筛选结果')
}

onMounted(async () => {
  categories.value = await supplierApi.categories()
  load()
})
</script>

<style scoped>
.ent { display: flex; align-items: center; gap: 10px; }
.ent img { width: 48px; height: 36px; object-fit: cover; border-radius: 4px; border: 1px solid #dce5e8; flex: none; }
.ent .nm { font-size: 13.5px; line-height: 19px; }
.empty { padding: 26px 0; color: #8a9aa2; font-size: 13px; }
</style>
