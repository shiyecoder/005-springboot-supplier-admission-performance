<template>
  <div>
    <div class="page-head">
      <div><h1>我的证照</h1><p>证照到期前请及时换证；过期未换证将暂停新增合作类目</p></div>
      <div class="right"><el-button type="primary" @click="openUpload">上传 / 换证</el-button></div>
    </div>

    <div class="metric-grid" style="grid-template-columns: repeat(4, 1fr)">
      <div class="metric"><div class="k">在库证照</div><div class="v">{{ certs.length }}<small> 张</small></div><div class="d">不含已作废</div></div>
      <div class="metric"><div class="k">已过期</div><div class="v" style="color:#b03035">{{ expired }}<small> 张</small></div><div class="d">需立即换证</div></div>
      <div class="metric"><div class="k">30 天内到期</div><div class="v" style="color:#a9701f">{{ soon }}<small> 张</small></div><div class="d">建议提前办理</div></div>
      <div class="metric"><div class="k">有效</div><div class="v" style="color:#2d7a58">{{ valid }}<small> 张</small></div><div class="d">可正常参与评审</div></div>
    </div>

    <div class="pcard" style="margin-top: 12px">
      <div class="panel-head"><h2>证照清单</h2><span class="sub">同类型再次上传视为换证，旧版本保留为已作废</span></div>
      <el-table :data="certs" style="width: 100%">
        <el-table-column prop="certType" label="证照类型" min-width="160" />
        <el-table-column prop="certNo" label="证照编号" min-width="170"><template #default="{ row }"><span class="mono">{{ row.certNo }}</span></template></el-table-column>
        <el-table-column label="签发日期" width="112"><template #default="{ row }">{{ $date(row.issueDate) }}</template></el-table-column>
        <el-table-column label="到期日期" width="112"><template #default="{ row }">{{ $date(row.expireDate) }}</template></el-table-column>
        <el-table-column label="状态" width="106"><template #default="{ row }"><StatusTag :text="row.certStatus" /></template></el-table-column>
        <el-table-column prop="versionNo" label="版本" width="70" />
        <el-table-column label="附件" width="80">
          <template #default="{ row }"><el-link type="primary" :href="row.filePath" target="_blank">查看</el-link></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" title="上传 / 换证" width="520px">
      <el-form label-width="90px">
        <el-form-item label="证照类型" required>
          <el-select v-model="form.certType" allow-create filterable style="width: 100%">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="证照编号" required><el-input v-model="form.certNo" /></el-form-item>
        <el-form-item label="签发日期"><el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="到期日期" required><el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="证照附件" required><UploadField v-model="form.filePath" biz-type="certificate" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import UploadField from '../../components/UploadField.vue'
import { certApi } from '../../api'
import { store } from '../../store/user'

const certs = ref([])
const dialog = ref(false)
const form = reactive({})
const types = ['营业执照', '质量管理体系认证', '环境管理体系认证', '特种设备制造许可', '排污许可证', '食品接触材料检测报告', '强制性产品认证', '道路运输经营许可']

const valid = computed(() => certs.value.filter((c) => c.certStatus === '有效').length)
const expired = computed(() => certs.value.filter((c) => c.certStatus === '已过期').length)
const soon = computed(() => certs.value.filter((c) => {
  if (!c.expireDate || c.certStatus === '已作废') return false
  const d = Math.round((new Date(c.expireDate) - new Date()) / 86400000)
  return d >= 0 && d <= 30
}).length)

async function load() {
  certs.value = await certApi.bySupplier(store.user.supplierId)
}

function openUpload() {
  Object.keys(form).forEach((k) => delete form[k])
  form.issueDate = new Date().toISOString().slice(0, 10)
  dialog.value = true
}

async function save() {
  if (!form.certType || !form.filePath) return ElMessage.warning('证照类型与附件为必填')
  await certApi.save({ ...form, supplierId: store.user.supplierId })
  ElMessage.success('已提交，等待采购专员核验')
  dialog.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.pcard { background: #fff; border: 1px solid #dce5e8; border-radius: 8px; padding: 18px 20px; }
</style>
