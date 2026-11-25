<template>
  <div>
    <div class="page-head">
      <div><h1>我的企业</h1><p>主档信息由采购专员审核后生效；变更需提交申请并说明原因</p></div>
      <div class="right"><el-button type="primary" @click="openChange">提交信息变更</el-button></div>
    </div>

    <div class="pcard">
      <div class="panel-head"><h2>企业主档</h2><StatusTag v-if="company.supplierStatus" :text="company.supplierStatus" /><div class="right"><LevelBadge :level="company.level" /></div></div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="企业名称">{{ company.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="供应商编码"><span class="mono">{{ company.supplierCode }}</span></el-descriptions-item>
        <el-descriptions-item label="统一信用代码"><span class="mono">{{ company.creditCode }}</span></el-descriptions-item>
        <el-descriptions-item label="供应类别">{{ company.categoryName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ company.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ company.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="电子邮箱">{{ company.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="注册地址">{{ company.address || '—' }}</el-descriptions-item>
        <el-descriptions-item label="准入日期">{{ $date(company.admissionDate) }}</el-descriptions-item>
        <el-descriptions-item label="资质到期">{{ $date(company.certExpireDate) }}</el-descriptions-item>
        <el-descriptions-item label="年产能">{{ company.annualCapacity || '—' }}</el-descriptions-item>
        <el-descriptions-item label="主要客户">{{ company.mainCustomers || '—' }}</el-descriptions-item>
        <el-descriptions-item label="产能说明" :span="2">{{ company.capacityDesc || '—' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="pcard">
      <div class="panel-head"><h2>信息变更说明</h2></div>
      <div class="warnbox">
        企业名称、统一信用代码、供应类目的变更需由采购专员审核，涉及证照的变更请先在「我的证照」上传新证。
        以下可自助修改的信息提交后立即生效并记录操作日志。
      </div>
    </div>

    <el-dialog v-model="dialog" title="提交信息变更" width="520px">
      <el-form label-width="90px">
        <el-form-item label="联系人"><el-input v-model="changeForm.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="changeForm.contactPhone" /></el-form-item>
        <el-form-item label="电子邮箱"><el-input v-model="changeForm.email" /></el-form-item>
        <el-form-item label="注册地址"><el-input v-model="changeForm.address" /></el-form-item>
        <el-form-item label="主要客户"><el-input v-model="changeForm.mainCustomers" /></el-form-item>
        <el-form-item label="变更原因"><el-input v-model="changeForm.reason" type="textarea" :rows="3" placeholder="请说明变更原因，不少于 5 个字" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submitChange">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import LevelBadge from '../../components/LevelBadge.vue'
import { supplierApi } from '../../api'
import { store } from '../../store/user'

const company = ref({})
const dialog = ref(false)
const changeForm = reactive({ contactName: '', contactPhone: '', email: '', address: '', mainCustomers: '', reason: '' })

async function load() {
  const detail = await supplierApi.detail(store.user.supplierId)
  company.value = detail.supplier
}

function openChange() {
  Object.assign(changeForm, {
    contactName: company.value.contactName, contactPhone: company.value.contactPhone,
    email: company.value.email, address: company.value.address,
    mainCustomers: company.value.mainCustomers, reason: ''
  })
  dialog.value = true
}

async function submitChange() {
  if (changeForm.reason.trim().length < 5) return ElMessage.warning('请填写不少于 5 个字的变更原因')
  const payload = { ...company.value, ...changeForm, version: company.value.version }
  delete payload.reason
  await supplierApi.update(company.value.id, payload)
  ElMessage.success('变更已提交并生效，操作已留痕')
  dialog.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.pcard { background: #fff; border: 1px solid #dce5e8; border-radius: 8px; padding: 18px 20px; margin-bottom: 16px; }
</style>
