<template>
  <div>
    <div class="page-head">
      <div><h1>准入申请</h1><p>按类目要求上传证照，系统校验必填材料与有效期；可先保存草稿再提交</p></div>
      <div class="right">
        <el-button v-if="!editing" type="primary" @click="startNew">新建申请</el-button>
        <el-button v-else @click="editing = false">返回列表</el-button>
      </div>
    </div>

    <div v-if="!editing">
      <div class="pcard">
        <div class="panel-head"><h2>我的申请</h2><span class="sub">最近 10 条</span></div>
        <el-table :data="apps" style="width: 100%">
          <el-table-column label="申请编号" width="160"><template #default="{ row }"><span class="mono">{{ row.applicationNo }}</span></template></el-table-column>
          <el-table-column prop="categoryName" label="供应类别" width="120" />
          <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag :text="row.status" /></template></el-table-column>
          <el-table-column prop="currentNode" label="当前节点" min-width="150" />
          <el-table-column label="提交时间" width="160"><template #default="{ row }">{{ $time(row.submitTime) }}</template></el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button v-if="row.status === '待提交'" link type="primary" @click="openDraft(row)">继续填写</el-button>
              <el-button v-if="row.status === '待提交'" link type="primary" @click="submitApp(row)">提交</el-button>
              <el-button v-else link type="primary" @click="showDetail(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div v-else>
      <div class="pcard">
        <div class="panel-head"><h2>企业基本信息</h2><span class="sub">带 * 为必填</span></div>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
          <el-row :gutter="16">
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
            <el-col :span="12"><el-form-item label="联系电话" prop="contactPhone"><el-input v-model="form.contactPhone" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="年产能"><el-input v-model="form.annualCapacity" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="注册地址"><el-input v-model="form.address" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="主要客户"><el-input v-model="form.mainCustomers" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="产能说明"><el-input v-model="form.capacityDesc" type="textarea" :rows="3" /></el-form-item></el-col>
          </el-row>
        </el-form>
      </div>

      <div class="pcard">
        <div class="panel-head"><h2>资质材料</h2><span class="sub">上传后自动登记到「我的证照」，无需手填路径</span></div>
        <el-row :gutter="16">
          <el-col v-for="t in uploadTypes" :key="t" :span="12">
            <div class="up-item">
              <div class="up-h">
                <b>{{ t }}</b>
                <span v-if="uploaded[t]" class="tag-ok">已上传</span>
                <span v-else class="tag-warn">待上传</span>
              </div>
              <UploadField
                :model-value="uploaded[t] || ''"
                biz-type="certificate"
                :tip="`上传后点击右侧「登记」写入证照清单`"
                @uploaded="(f) => (pending[t] = f.url)"
              />
              <div style="display: flex; justify-content: flex-end; margin-top: 6px">
                <el-button size="small" :disabled="!pending[t]" @click="registerCert(t)">登记证照</el-button>
              </div>
            </div>
          </el-col>
        </el-row>
        <div class="warnbox" style="margin-top: 8px">提交前请确认材料齐全且证照在有效期内，材料缺失会被形式审查退回。</div>
      </div>

      <div style="display: flex; justify-content: flex-end; gap: 10px">
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" @click="createAndSubmit">提交申请</el-button>
      </div>
    </div>

    <el-drawer v-model="drawer" size="560px" title="申请详情">
      <div v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="申请编号"><span class="mono">{{ current.applicationNo }}</span></el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :text="current.status" /></el-descriptions-item>
          <el-descriptions-item label="当前节点" :span="2">{{ current.currentNode }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ $time(current.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ $time(current.deadline) }}</el-descriptions-item>
          <el-descriptions-item v-if="current.rejectReason" label="驳回原因" :span="2">
            <span style="color:#b03035">{{ current.rejectReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import UploadField from '../../components/UploadField.vue'
import { admissionApi, certApi, supplierApi } from '../../api'
import { store } from '../../store/user'

const apps = ref([])
const categories = ref([])
const editing = ref(false)
const formRef = ref()
const form = reactive({})
const pending = reactive({})
const uploaded = reactive({})
const drawer = ref(false)
const current = ref({})
const draftId = ref(null)
const uploadTypes = ['营业执照', '质量管理体系认证', '环境管理体系认证', '排污许可证', '审计报告', '现场照片']

const rules = {
  supplierName: [{ required: true, message: '必填', trigger: 'blur' }],
  creditCode: [{ required: true, message: '必填', trigger: 'blur' }],
  categoryId: [{ required: true, message: '必选', trigger: 'change' }],
  contactName: [{ required: true, message: '必填', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '必填', trigger: 'blur' }]
}

async function load() {
  apps.value = (await admissionApi.page({ page: 1, size: 10 })).records
  categories.value = await supplierApi.categories()
}

async function startNew() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.keys(pending).forEach((k) => delete pending[k])
  Object.keys(uploaded).forEach((k) => delete uploaded[k])
  draftId.value = null
  const detail = await supplierApi.detail(store.user.supplierId)
  const s = detail.supplier
  Object.assign(form, {
    supplierId: s.id, supplierName: s.supplierName, creditCode: s.creditCode, categoryId: s.categoryId,
    contactName: s.contactName, contactPhone: s.contactPhone, address: s.address,
    annualCapacity: s.annualCapacity, mainCustomers: s.mainCustomers, capacityDesc: s.capacityDesc
  })
  detail.certificates.filter((c) => c.certStatus !== '已作废').forEach((c) => { uploaded[c.certType] = c.filePath })
  editing.value = true
}

async function openDraft(row) {
  await startNew()
  Object.assign(form, {
    supplierName: row.supplierName, creditCode: row.creditCode, categoryId: row.categoryId,
    contactName: row.contactName, contactPhone: row.contactPhone, address: row.address,
    annualCapacity: row.annualCapacity, mainCustomers: row.mainCustomers, capacityDesc: row.capacityDesc
  })
  draftId.value = row.id
}

async function registerCert(type) {
  if (!pending[type]) return
  await certApi.save({
    supplierId: store.user.supplierId, certType: type,
    certNo: 'AUTO-' + Date.now(), filePath: pending[type],
    issueDate: new Date().toISOString().slice(0, 10),
    expireDate: new Date(Date.now() + 365 * 2 * 86400000).toISOString().slice(0, 10)
  })
  uploaded[type] = pending[type]
  delete pending[type]
  ElMessage.success(`${type} 已登记，可在「我的证照」补全编号与有效期`)
}

async function saveDraft() {
  await formRef.value.validate()
  if (draftId.value) {
    await admissionApi.update(draftId.value, form)
  } else {
    draftId.value = await admissionApi.create(form)
  }
  ElMessage.success('草稿已保存')
  editing.value = false
  load()
}

async function createAndSubmit() {
  await formRef.value.validate()
  try {
    if (!draftId.value) draftId.value = await admissionApi.create(form)
    await admissionApi.transition(draftId.value, { targetStatus: '准入审核', reason: '供应商提交准入申请' })
    ElMessage.success('已提交，等待采购专员形式审查')
    editing.value = false
    draftId.value = null
    load()
  } catch (e) { /* 拦截器已提示材料不齐等原因 */ }
}

async function submitApp(row) {
  try {
    await admissionApi.transition(row.id, { targetStatus: '准入审核', reason: '供应商提交准入申请' })
    ElMessage.success('已提交')
    load()
  } catch (e) { /* 已提示 */ }
}

function showDetail(row) {
  current.value = row
  drawer.value = true
}

onMounted(load)
</script>

<style scoped>
.pcard { background: #fff; border: 1px solid #dce5e8; border-radius: 8px; padding: 18px 20px; margin-bottom: 16px; }
.up-item { border: 1px solid #e8eef0; border-radius: 8px; padding: 10px 12px; margin-bottom: 12px; background: #fbfdfd; }
.up-h { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 13px; }
.tag-ok { font-size: 11.5px; color: #2d7a58; background: rgba(45, 122, 88, 0.1); padding: 1px 7px; border-radius: 4px; }
.tag-warn { font-size: 11.5px; color: #a9701f; background: rgba(169, 112, 31, 0.12); padding: 1px 7px; border-radius: 4px; }
</style>
