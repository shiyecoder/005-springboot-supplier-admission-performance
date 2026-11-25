<template>
  <div class="portal">
    <header class="ptop">
      <div class="b"><span class="lg">供</span>供应商服务门户</div>
      <nav class="pnav">
        <router-link v-for="m in navs" :key="m.path" :to="m.path" :class="{ on: route.path === m.path }">
          {{ m.name }}
        </router-link>
      </nav>
      <el-dropdown @command="onCommand">
        <div class="who">
          <img :src="store.user?.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
          <div>
            <div class="n">{{ store.user?.realName }}</div>
            <div class="c">{{ store.user?.supplierName }}</div>
          </div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="company">我的企业</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>
    <div class="pwrap"><router-view /></div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { store, loadInfo, logout } from '../store/user'

const route = useRoute()
const router = useRouter()

const navs = computed(() => {
  const list = (store.menus || []).map((m) => ({ name: m.name, path: m.path }))
  return list.length ? list : [
    { name: '我的工作台', path: '/portal/home' },
    { name: '准入申请', path: '/portal/apply' },
    { name: '我的证照', path: '/portal/certificate' }
  ]
})

function onCommand(cmd) {
  if (cmd === 'company') router.push('/portal/company')
  if (cmd === 'logout') {
    logout()
    router.push('/login')
  }
}

onMounted(loadInfo)
</script>

<style scoped>
.portal { min-height: 100vh; background: #eef4f5; }
.ptop {
  height: 64px; background: #237a8b; display: flex; align-items: center; gap: 20px; padding: 0 32px; color: #fff;
}
.ptop .b { font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 10px; white-space: nowrap; }
.ptop .lg {
  width: 26px; height: 26px; border-radius: 6px; background: rgba(255, 255, 255, 0.2);
  display: grid; place-items: center; font-size: 13px;
}
.pnav { display: flex; gap: 4px; margin-left: 16px; flex: 1; overflow: auto; }
.pnav a {
  color: rgba(255, 255, 255, 0.82); font-size: 13.5px; padding: 8px 14px; border-radius: 6px;
  text-decoration: none; white-space: nowrap;
}
.pnav a.on, .pnav a:hover { background: rgba(255, 255, 255, 0.16); color: #fff; }
.who { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #fff; }
.who img { width: 28px; height: 28px; border-radius: 50%; object-fit: cover; background: rgba(255, 255, 255, 0.2); }
.who .n { font-size: 13px; line-height: 15px; }
.who .c { font-size: 11px; color: rgba(255, 255, 255, 0.7); line-height: 14px; }
.pwrap { max-width: 1160px; margin: 0 auto; padding: 24px 32px 40px; }
</style>
