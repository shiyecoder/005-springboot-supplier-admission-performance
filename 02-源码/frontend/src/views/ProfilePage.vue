<template>
  <div>
    <div class="page-head"><div><h1>个人中心</h1><p>修改资料、密码与头像；头像上传后右上角同步刷新</p></div></div>
    <div class="cols">
      <div class="panel">
        <div class="avatar-box">
          <img class="av" :src="form.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
          <div class="n">{{ form.realName }}</div>
          <div class="r">{{ user.roleName }} · {{ user.orgName || user.supplierName || '—' }}</div>
          <div style="margin-top: 12px">
            <UploadField v-model="form.avatar" biz-type="avatar" tip="JPG/PNG，建议 1:1，上传后立即同步右上角" />
          </div>
        </div>
        <div class="side">
          <a class="on">基本资料</a>
          <a @click="tab = 'pwd'">修改密码</a>
          <a @click="tab = 'msg'">我的消息</a>
        </div>
      </div>

      <div>
        <div v-show="tab === 'info'" class="panel">
          <div class="panel-head"><h2>基本资料</h2><span class="sub">手机号与邮箱用于接收站内提醒</span></div>
          <el-form label-width="90px" style="max-width: 520px">
            <el-form-item label="账号"><el-input :model-value="user.username" disabled /></el-form-item>
            <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
            <el-form-item label="电子邮箱"><el-input v-model="form.email" /></el-form-item>
            <el-form-item label="所属组织"><el-input :model-value="user.orgName || user.supplierName || '—'" disabled /></el-form-item>
            <el-form-item label="角色"><el-input :model-value="user.roleName" disabled /></el-form-item>
          </el-form>
          <div style="display: flex; justify-content: flex-end"><el-button type="primary" @click="saveProfile">保存资料</el-button></div>
        </div>

        <div v-show="tab === 'pwd'" class="panel">
          <div class="panel-head"><h2>修改密码</h2><span class="sub">密码使用 BCrypt 保存，接口不返回密码字段</span></div>
          <el-form ref="pwdRef" :model="pwd" :rules="pwdRules" label-width="90px" style="max-width: 460px">
            <el-form-item label="原密码" prop="oldPassword"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
            <el-form-item label="确认密码" prop="confirm"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
          </el-form>
          <div class="note" style="max-width: 520px">连续 5 次登录失败将锁定账号 10 分钟；修改密码后其他会话失效，需重新登录。</div>
          <div style="margin-top: 14px"><el-button type="primary" @click="savePassword">确认修改</el-button></div>
        </div>

        <div v-show="tab === 'msg'" class="panel">
          <div class="panel-head">
            <h2>我的消息</h2>
            <div class="right"><el-button size="small" @click="readAll">全部已读</el-button></div>
          </div>
          <div v-if="!messages.length" class="empty">暂无消息</div>
          <div v-for="m in messages" :key="m.id" class="msg">
            <div class="t"><b>{{ m.title }}</b><span class="sub2">{{ $time(m.createTime) }}</span></div>
            <div class="c">{{ m.content }}</div>
            <div class="sub2">{{ m.bizNo }} · <el-link type="primary" :href="'#' + (m.url || '/dashboard')">查看详情</el-link></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import UploadField from '../components/UploadField.vue'
import { authApi } from '../api'
import { store, loadInfo } from '../store/user'

const tab = ref('info')
const user = computed(() => store.user || {})
const form = reactive({ realName: '', phone: '', email: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRef = ref()
const messages = ref([])

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '至少 6 位', trigger: 'blur' }],
  confirm: [{
    validator: (r, v, cb) => (v === pwd.newPassword ? cb() : cb(new Error('两次输入不一致'))),
    trigger: 'blur'
  }]
}

async function saveProfile() {
  await authApi.updateProfile(form)
  ElMessage.success('资料已保存')
  await loadInfo()
}

async function savePassword() {
  await pwdRef.value.validate()
  await authApi.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改，请重新登录')
  pwd.oldPassword = pwd.newPassword = pwd.confirm = ''
}

async function loadMessages() {
  messages.value = await authApi.messages()
}

async function readAll() {
  await authApi.readAll()
  store.unread = 0
  loadMessages()
}

onMounted(async () => {
  const info = await authApi.info()
  Object.assign(form, { realName: info.realName, phone: info.phone, email: info.email, avatar: info.avatar })
  loadMessages()
})
</script>

<style scoped>
.cols { display: grid; grid-template-columns: 330px 1fr; gap: 16px; align-items: start; }
.avatar-box { text-align: center; padding-bottom: 14px; border-bottom: 1px solid #dce5e8; margin-bottom: 12px; }
.avatar-box .av {
  width: 88px; height: 88px; border-radius: 50%; object-fit: cover; display: block; margin: 0 auto 10px;
  border: 3px solid #fff; box-shadow: 0 0 0 1px #dce5e8;
}
.avatar-box .n { font-size: 16px; font-weight: 600; }
.avatar-box .r { font-size: 12.5px; color: #8a9aa2; margin-top: 2px; }
.side a { display: block; padding: 8px 12px; border-radius: 6px; font-size: 13.5px; color: #5b6e77; cursor: pointer; }
.side a.on, .side a:hover { background: rgba(35, 122, 139, 0.1); color: #237a8b; font-weight: 600; }
.empty { padding: 24px 0; text-align: center; color: #8a9aa2; }
.msg { padding: 10px 0; border-bottom: 1px solid #eef2f4; }
.msg .t { display: flex; justify-content: space-between; font-size: 13px; }
.msg .c { font-size: 12.5px; color: #5b6e77; margin: 3px 0; }
</style>
