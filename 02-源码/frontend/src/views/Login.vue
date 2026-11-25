<template>
  <div class="login">
    <section class="left" :style="{ backgroundImage: `url(${bg})` }">
      <div class="mask"></div>
      <div class="brand"><span class="lg">供</span>供应商准入与绩效评价管理系统</div>
      <div class="hero">
        <h1>从准入到分层<br /><em>每一次评级都可核对</em></h1>
        <p>统一受理潜在供应商申请，串联形式审查、专业评审、合格名录与周期性绩效评价。等级变化保留输入快照、规则版本与人工调整原因。</p>
        <div class="chain">
          <span>草稿</span><i>→</i><span>形式审查</span><i>→</i><span>专业评审</span><i>→</i><span>待审批</span><i>→</i><span>已准入</span>
        </div>
      </div>
      <div class="foot">内部系统 · 请使用企业账号登录 · 连续 5 次失败将锁定 10 分钟</div>
    </section>

    <section class="right">
      <div class="pane">
        <h2>{{ mode === 'login' ? '账号登录' : '供应商注册' }}</h2>
        <p class="sub">
          {{ mode === 'login' ? '内部用户与供应商联系人使用同一入口，按角色进入不同工作台' : '注册后自动创建潜在供应商主档，账号用于提交准入申请' }}
        </p>

        <div class="tabs">
          <div :class="{ on: mode === 'login' }" @click="mode = 'login'">密码登录</div>
          <div :class="{ on: mode === 'register' }" @click="mode = 'register'">供应商注册</div>
        </div>

        <el-form v-if="mode === 'login'" ref="loginRef" :model="loginForm" :rules="loginRules" @keyup.enter="doLogin">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入账号" size="large">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" size="large">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <div class="row"><el-checkbox v-model="remember">记住账号</el-checkbox><span class="hint">忘记密码请联系管理员</span></div>
          <el-button class="submit" type="primary" size="large" :loading="loading" @click="doLogin">登 录</el-button>
        </el-form>

        <el-form v-else ref="regRef" :model="regForm" :rules="regRules" label-position="top">
          <el-form-item label="企业名称" prop="supplierName">
            <el-input v-model="regForm.supplierName" placeholder="与营业执照一致" />
          </el-form-item>
          <el-form-item label="统一信用代码" prop="creditCode">
            <el-input v-model="regForm.creditCode" placeholder="18 位，系统校验唯一，同一代码不允许重复准入" />
          </el-form-item>
          <el-form-item label="供应类别" prop="categoryCode">
            <el-select v-model="regForm.categoryCode" placeholder="请选择" style="width: 100%">
              <el-option v-for="c in categories" :key="c.categoryCode" :label="c.categoryName" :value="c.categoryCode" />
            </el-select>
          </el-form-item>
          <div class="two">
            <el-form-item label="联系人" prop="realName"><el-input v-model="regForm.realName" /></el-form-item>
            <el-form-item label="联系电话" prop="phone"><el-input v-model="regForm.phone" /></el-form-item>
          </div>
          <div class="two">
            <el-form-item label="登录账号" prop="username"><el-input v-model="regForm.username" /></el-form-item>
            <el-form-item label="登录密码" prop="password"><el-input v-model="regForm.password" type="password" show-password /></el-form-item>
          </div>
          <el-button class="submit" type="primary" size="large" :loading="loading" @click="doRegister">提交注册</el-button>
        </el-form>

        <div class="tip">
          <b>演示账号</b>　admin / admin123（采购管理员）　·　buyer01 / 123456（采购专员）<br />
          reviewer01 / 123456（评审人员）　·　sup001 / 123456（供应商联系人）
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import bg from '../assets/login-bg.jpg'
import { login } from '../store/user'
import { authApi, supplierApi } from '../api'

const router = useRouter()
const mode = ref('login')
const loading = ref(false)
const remember = ref(true)
const loginRef = ref()
const regRef = ref()
const categories = ref([])

const loginForm = reactive({ username: 'admin', password: 'admin123' })
const regForm = reactive({
  supplierName: '', creditCode: '', categoryCode: 'RAW', realName: '', phone: '',
  username: '', password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const regRules = {
  supplierName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一信用代码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }]
}

async function doLogin() {
  await loginRef.value.validate()
  loading.value = true
  try {
    const user = await login(loginForm)
    ElMessage.success(`欢迎回来，${user.realName}`)
    router.push(user.roleCode === 'supplier' ? '/portal/home' : '/dashboard')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  await regRef.value.validate()
  loading.value = true
  try {
    await authApi.register(regForm)
    ElMessage.success('注册成功，请使用新账号登录')
    mode.value = 'login'
    loginForm.username = regForm.username
    loginForm.password = ''
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  categories.value = await supplierApi.categories()
})
</script>

<style scoped>
.login { display: grid; grid-template-columns: 1.05fr 0.95fr; height: 100vh; overflow: hidden; }
.left {
  position: relative; color: #fff; padding: 48px 60px; display: flex; flex-direction: column;
  background-size: cover; background-position: center;
}
.left .mask {
  position: absolute; inset: 0;
  background: linear-gradient(120deg, rgba(11, 36, 51, 0.94) 0%, rgba(20, 62, 79, 0.82) 45%, rgba(24, 84, 100, 0.6) 100%);
}
.brand { position: relative; z-index: 1; display: flex; align-items: center; gap: 11px; font-size: 15px; font-weight: 600; }
.brand .lg {
  width: 34px; height: 34px; border-radius: 8px; display: grid; place-items: center; font-size: 15px;
  background: linear-gradient(135deg, #3fa3b6, #237a8b);
}
.hero { position: relative; z-index: 1; margin: auto 0; max-width: 520px; }
.hero h1 { font-size: 37px; line-height: 52px; margin: 0; font-weight: 600; letter-spacing: 0.5px; }
.hero h1 em { font-style: normal; color: #7fd0de; }
.hero p { margin-top: 18px; font-size: 13.5px; line-height: 26px; color: rgba(255, 255, 255, 0.72); }
.chain { display: flex; align-items: center; gap: 8px; margin-top: 30px; flex-wrap: wrap; }
.chain span {
  font-size: 12.5px; padding: 6px 12px; border: 1px solid rgba(127, 208, 222, 0.35);
  border-radius: 20px; color: #bfe7ee; background: rgba(35, 122, 139, 0.18);
}
.chain i { color: rgba(255, 255, 255, 0.3); font-style: normal; font-size: 12px; }
.foot { position: relative; z-index: 1; font-size: 12px; color: rgba(255, 255, 255, 0.45); }
.right { background: #f3f7f8; display: grid; place-items: center; padding: 32px; overflow: auto; }
.pane { width: 400px; }
.pane h2 { font-size: 24px; margin: 0; color: #15344a; }
.pane .sub { font-size: 12.5px; color: #5b6e77; margin: 6px 0 0; line-height: 20px; }
.tabs { display: flex; gap: 26px; margin: 22px 0 18px; border-bottom: 1px solid #dce5e8; }
.tabs div {
  padding-bottom: 10px; font-size: 14px; color: #5b6e77; cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.tabs div.on { color: #15344a; font-weight: 600; border-bottom-color: #15344a; }
.row { display: flex; align-items: center; justify-content: space-between; margin: 2px 0 16px; }
.submit { width: 100%; height: 42px; font-size: 15px; letter-spacing: 2px; margin-top: 4px; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.tip {
  margin-top: 18px; font-size: 12px; color: #8a9aa2; line-height: 20px;
  background: #fff; border: 1px dashed #dce5e8; border-radius: 6px; padding: 10px 12px;
}
.tip b { color: #5b6e77; font-weight: 500; }
:deep(.el-form-item) { margin-bottom: 16px; }
:deep(.el-form-item__label) { padding-bottom: 2px; font-size: 12.5px; }
</style>
