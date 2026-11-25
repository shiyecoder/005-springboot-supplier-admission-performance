<template>
  <div>
    <div class="page-head">
      <div><h1>用户管理</h1><p>账号、角色与所属组织 · 供应商联系人绑定企业后只能看到本企业数据</p></div>
      <div class="right"><el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button></div>
    </div>

    <div class="panel">
      <div class="filter-row">
        <el-input v-model="query.kw" placeholder="账号 / 姓名" clearable @keyup.enter="search" />
        <el-select v-model="query.roleCode" placeholder="角色" clearable>
          <el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head"><h2>账号列表</h2><div class="right"><el-tag type="info" effect="plain">共 {{ total }} 个</el-tag></div></div>
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="账号" width="140"><template #default="{ row }"><span class="mono">{{ row.username }}</span></template></el-table-column>
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="roleName" label="角色" width="150" />
        <el-table-column prop="orgName" label="所属组织" width="130" />
        <el-table-column prop="phone" label="联系方式" width="140" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :text="row.status === 1 ? '启用' : '停用'" /></template></el-table-column>
        <el-table-column label="最近登录" width="160"><template #default="{ row }">{{ $time(row.lastLogin) }}</template></el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="resetPwd(row)">重置密码</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'primary'" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="load" @size-change="search" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑用户' : '新增用户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="form.roleCode" style="width: 100%">
            <el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属组织">
          <el-select v-model="form.orgId" style="width: 100%" clearable>
            <el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.roleCode === 'supplier'" label="关联企业">
          <el-select v-model="form.supplierId" filterable style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item :label="form.id ? '重置密码' : '初始密码'">
          <el-input v-model="form.password" placeholder="留空则默认 123456" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import StatusTag from '../../components/StatusTag.vue'
import { systemApi, supplierApi } from '../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const roles = ref([])
const orgs = ref([])
const suppliers = ref([])
const query = reactive({ page: 1, size: 10, kw: '', roleCode: '' })
const dialog = ref(false)
const formRef = ref()
const form = reactive({})

const rules = {
  username: [{ required: true, message: '必填', trigger: 'blur' }],
  realName: [{ required: true, message: '必填', trigger: 'blur' }],
  roleCode: [{ required: true, message: '必选', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const res = await systemApi.users(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }
const reset = () => { Object.assign(query, { page: 1, kw: '', roleCode: '' }); load() }

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { status: 1, password: '123456' })
  dialog.value = true
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { ...row, roleCode: (row.roleCode || '').split(',')[0], password: '' })
  dialog.value = true
}

async function save() {
  await formRef.value.validate()
  await systemApi.saveUser(form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function toggle(row) {
  await systemApi.toggle(row.id, { status: row.status === 1 ? 0 : 1 })
  ElMessage.success('状态已更新')
  load()
}

async function resetPwd(row) {
  try {
    await ElMessageBox.confirm(`确认将 ${row.username} 的密码重置为 123456？`, '重置密码', { type: 'warning' })
    await systemApi.reset(row.id)
    ElMessage.success('已重置')
  } catch (e) { /* 取消 */ }
}

onMounted(async () => {
  roles.value = await systemApi.roles()
  orgs.value = await systemApi.orgs()
  suppliers.value = await supplierApi.options()
  load()
})
</script>
