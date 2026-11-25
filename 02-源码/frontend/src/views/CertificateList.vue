<template>
  <div>
    <div class="page-head">
      <div><h1>证照与预警</h1><p>过期证照未更新时暂停新增合作类目，并进入整改流程</p></div>
      <div class="right">
        <el-button :icon="Refresh" @click="refresh">重新扫描</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">登记证照</el-button>
      </div>
    </div>

    <div class="metric-grid" style="grid-template-columns: repeat(4, 1fr)">
      <div class="metric"><div class="k">已过期</div><div class="v" style="color: #b03035">{{ stats.expired || 0 }}<small> 张</small></div><div class="d">需催办换证</div></div>
      <div class="metric"><div class="k">30 天内到期</div><div class="v" style="color: #a9701f">{{ stats.within30 || 0 }}<small> 张</small></div><div class="d">已发送站内提醒</div></div>
      <div class="metric"><div class="k">31–90 天到期</div><div class="v">{{ stats.within90 || 0 }}<small> 张</small></div><div class="d">建议提前换证</div></div>
      <div class="metric"><div class="k">在库证照</div><div class="v">{{ stats.total || 0 }}<small> 张</small></div><div class="d">不含已作废</div></div>
    </div>

    <div class="panel" style="margin-top: 12px">
      <div class="filter-row">
        <el-input v-model="query.kw" placeholder="企业名称 / 编码" clearable @keyup.enter="search" />
        <el-select v-model="query.certType" placeholder="证照类型" clearable>
          <el-option v-for="t in types" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="query.certStatus" placeholder="状态" clearable>
          <el-option v-for="s in ['有效', '已过期', '已作废']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h2>证照清单</h2>
        <span class="sub">按到期日期升序，过期置顶</span>
      </div>
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="供应商" min-width="200">
          <template #default="{ row }">
            <div>{{ row.supplierName }}</div>
            <div class="sub2">SUP · {{ row.supplierId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="certType" label="证照类型" min-width="160" />
        <el-table-column prop="certNo" label="证照编号" min-width="170"><template #default="{ row }"><span class="mono">{{ row.certNo }}</span></template></el-table-column>
        <el-table-column label="到期日期" width="112"><template #default="{ row }">{{ $date(row.expireDate) }}</template></el-table-column>
        <el-table-column label="剩余天数" width="100">
          <template #default="{ row }">
            <span class="num" :style="{ color: days(row.expireDate) < 0 ? '#b03035' : days(row.expireDate) <= 30 ? '#a9701f' : '#5b6e77' }">{{ days(row.expireDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="106"><template #default="{ row }"><StatusTag :text="row.certStatus" /></template></el-table-column>
        <el-table-column prop="versionNo" label="版本" width="66" />
        <el-table-column label="附件" width="80">
          <template #default="{ row }"><el-link type="primary" :href="row.filePath" target="_blank">查看</el-link></template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="renew(row)">换证</el-button>
            <el-button link type="danger" @click="voidCert(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="load" @size-change="search" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="form.certType ? '登记 / 换证' : '登记证照'" width="560px">
      <el-form label-width="100px">
        <el-form-item label="供应商" required>
          <el-select v-model="form.supplierId" filterable style="width: 100%" :disabled="!!form.supplierIdFixed">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="证照类型" required>
          <el-select v-model="form.certType" allow-create filterable style="width: 100%">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="证照编号" required><el-input v-model="form.certNo" /></el-form-item>
        <el-form-item label="签发日期"><el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="到期日期" required><el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="证照附件" required>
          <UploadField v-model="form.filePath" biz-type="certificate" tip="同类型证照再次登记视为换证，旧版本自动作废并保留历史" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import StatusTag from '../components/StatusTag.vue'
import UploadField from '../components/UploadField.vue'
import { certApi, supplierApi } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const suppliers = ref([])
const query = reactive({ page: 1, size: 10, kw: '', certType: '', certStatus: '' })
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})
const types = ['营业执照', '质量管理体系认证', '环境管理体系认证', '特种设备制造许可', '排污许可证', '食品接触材料检测报告', '强制性产品认证', '道路运输经营许可', '危险化学品经营许可']

const days = (d) => (d ? Math.round((new Date(d) - new Date()) / 86400000) : '—')

async function load() {
  loading.value = true
  try {
    const res = await certApi.page(query)
    rows.value = res.records
    total.value = res.total
    stats.value = await certApi.stats()
  } finally {
    loading.value = false
  }
}

const search = () => { query.page = 1; load() }
const reset = () => { Object.assign(query, { page: 1, kw: '', certType: '', certStatus: '' }); load() }

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.issueDate = new Date().toISOString().slice(0, 10)
  dialog.value = true
}

function renew(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { supplierId: row.supplierId, supplierIdFixed: true, certType: row.certType, certNo: row.certNo })
  dialog.value = true
}

async function save() {
  if (!form.supplierId || !form.certType || !form.filePath) return ElMessage.warning('供应商、证照类型与附件为必填')
  saving.value = true
  try {
    await certApi.save(form)
    ElMessage.success('已保存，历史版本保留为已作废')
    dialog.value = false
    load()
    if (form.supplierId) {
      /* 主档到期日由后端回写，无需前端处理 */
    }
  } finally {
    saving.value = false
  }
}

async function voidCert(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写作废原因（不少于 5 个字）', '作废证照', {
      inputType: 'textarea', inputPlaceholder: '如：证书信息有误，重新办理'
    })
    await certApi.voidCert(row.id, { reason: value })
    ElMessage.success('已作废')
    load()
  } catch (e) { /* 取消 */ }
}

async function refresh() {
  await certApi.refresh()
  ElMessage.success('已重新扫描到期状态')
  load()
}

onMounted(async () => {
  suppliers.value = await supplierApi.options()
  load()
})
</script>
