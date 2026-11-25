<template>
  <div class="admin">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">供</div>
        <div class="txt"><b title="供应商准入与绩效评价管理系统">供应商准入与绩效评价</b><small>SUPPLIER GOVERNANCE</small></div>
      </div>
      <nav class="menu">
        <template v-for="m in menuTree" :key="m.path || m.name">
          <router-link v-if="!m.children?.length" :to="m.path" :class="{ on: isActive(m.path) }">
            <el-icon><component :is="iconOf(m.icon)" /></el-icon>
            <span>{{ m.name }}</span>
          </router-link>
          <div v-else class="group">
            <div class="group-title" :class="{ on: m.children.some((c) => isActive(c.path)) }" @click="toggle(m.name)">
              <el-icon><component :is="iconOf(m.icon)" /></el-icon>
              <span>{{ m.name }}</span>
              <el-icon class="arrow" :class="{ open: opened[m.name] }"><ArrowRight /></el-icon>
            </div>
            <div v-show="opened[m.name]" class="sub">
              <router-link v-for="c in m.children" :key="c.path" :to="c.path" :class="{ on: isActive(c.path) }">
                {{ c.name }}
              </router-link>
            </div>
          </div>
        </template>
      </nav>
      <div class="foot">v1.0 · 内部管理端</div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="crumb">供应链管理 / <b>{{ route.meta.title || '工作台' }}</b></div>
        <div class="spacer"></div>
        <el-popover placement="bottom-end" width="330" trigger="click" @show="loadMessages">
          <template #reference>
            <div class="icon-btn"><el-icon><Bell /></el-icon><i v-if="store.unread" class="dot" /></div>
          </template>
          <div class="msg-head"><b>站内消息</b><a href="javascript:;" @click="readAll">全部已读</a></div>
          <div v-if="!messages.length" class="empty">暂无消息</div>
          <div v-for="msg in messages.slice(0, 6)" :key="msg.id" class="msg" @click="goMsg(msg)">
            <div class="t">{{ msg.title }}<span class="sub2">{{ $time(msg.createTime) }}</span></div>
            <div class="c">{{ msg.content }}</div>
          </div>
        </el-popover>
        <el-tooltip content="帮助：审批与评分规则见需求文档" placement="bottom">
          <div class="icon-btn"><el-icon><QuestionFilled /></el-icon></div>
        </el-tooltip>
        <el-dropdown @command="onCommand">
          <div class="who">
            <img :src="store.user?.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
            <div><div class="n">{{ store.user?.realName || '用户' }}</div><div class="r">{{ store.user?.roleName || '' }}</div></div>
            <el-icon class="down"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <main class="content"><router-view /></main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { store, loadInfo, logout } from '../store/user'
import { authApi } from '../api'

const route = useRoute()
const router = useRouter()
const messages = ref([])
const opened = reactive({ 系统管理: true })

const ICONS = {
  'i-dashboard': 'DataBoard', 'i-supplier': 'OfficeBuilding', 'i-apply': 'Document',
  'i-cert': 'Files', 'i-performance': 'TrendCharts', 'i-rectify': 'Warning',
  'i-model': 'Share', 'i-report': 'PieChart', 'i-system': 'Setting', 'i-user': 'User'
}
const iconOf = (icon) => ICONS[icon] || 'Menu'

// 菜单来自后端，按角色动态返回；visible=0 的菜单（菜单管理、分配菜单）不会出现在这里
const menuTree = computed(() => {
  const list = store.menus || []
  return list.map((m) => ({
    ...m,
    children: (m.children || []).filter((c) => c.path)
  }))
})

const isActive = (path) => path && (route.path === path || route.path.startsWith(path + '/'))
const toggle = (name) => { opened[name] = !opened[name] }

async function loadMessages() {
  messages.value = await authApi.messages()
  store.unread = messages.value.filter((m) => m.readFlag === 0).length
}

async function readAll() {
  await authApi.readAll()
  messages.value = messages.value.map((m) => ({ ...m, readFlag: 1 }))
  store.unread = 0
}

function goMsg(msg) {
  if (msg.url) router.push(msg.url.replace('/admission', '/admission').replace('/portal/performance', '/portal/performance'))
}

function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'logout') {
    logout()
    router.push('/login')
  }
}

onMounted(async () => {
  await loadInfo()
  try {
    store.unread = await authApi.unread()
  } catch (e) { /* 忽略：消息角标失败不影响主流程 */ }
})
</script>

<style scoped>
.admin { display: flex; min-height: 100vh; }
.sidebar {
  width: 220px; flex: none; background: #15344a; color: #fff; position: fixed; inset: 0 auto 0 0;
  display: flex; flex-direction: column; z-index: 20;
}
.brand {
  height: 56px; display: flex; align-items: center; gap: 9px; padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.brand .logo {
  width: 26px; height: 26px; border-radius: 6px; display: grid; place-items: center;
  background: linear-gradient(135deg, #237a8b, #2d5b6b); font-size: 13px; font-weight: 700;
}
.brand b { font-size: 13px; display: block; line-height: 16px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 148px; }
.brand small { font-size: 9px; color: rgba(255, 255, 255, 0.45); letter-spacing: 1px; }
.menu { flex: 1; overflow: auto; padding: 10px 0; }
.menu a, .group-title {
  display: flex; align-items: center; gap: 10px; height: 40px; padding: 0 16px;
  color: rgba(255, 255, 255, 0.78); font-size: 13.5px; text-decoration: none; cursor: pointer;
  border-left: 3px solid transparent;
}
.menu a:hover, .group-title:hover { background: rgba(255, 255, 255, 0.06); color: #fff; }
.menu a.on, .group-title.on {
  background: rgba(35, 122, 139, 0.28); color: #fff; border-left-color: #3fa3b6; font-weight: 500;
}
.group-title .arrow { margin-left: auto; font-size: 12px; transition: transform 0.2s; }
.group-title .arrow.open { transform: rotate(90deg); }
.sub a { padding-left: 44px; height: 36px; font-size: 13px; }
.foot { padding: 12px 16px; font-size: 11px; color: rgba(255, 255, 255, 0.35); border-top: 1px solid rgba(255, 255, 255, 0.08); }
.main { margin-left: 220px; flex: 1; min-width: 0; display: flex; flex-direction: column; }
.topbar {
  height: 56px; background: #fff; border-bottom: 1px solid #dce5e8; display: flex; align-items: center;
  gap: 12px; padding: 0 24px; position: sticky; top: 0; z-index: 15;
}
.crumb { font-size: 12px; color: #5b6e77; }
.crumb b { color: #1c2b33; font-weight: 500; }
.spacer { flex: 1; }
.icon-btn {
  width: 32px; height: 32px; border-radius: 6px; display: grid; place-items: center;
  color: #5b6e77; cursor: pointer; position: relative;
}
.icon-btn:hover { background: #eaf1f3; }
.icon-btn .dot {
  position: absolute; top: 6px; right: 7px; width: 7px; height: 7px; border-radius: 50%;
  background: #b03035; border: 1.5px solid #fff;
}
.who { display: flex; align-items: center; gap: 8px; padding-left: 12px; border-left: 1px solid #dce5e8; cursor: pointer; }
.who img { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; background: #eaf1f3; }
.who .n { font-size: 13px; line-height: 16px; }
.who .r { font-size: 11px; color: #8a9aa2; line-height: 14px; }
.who .down { font-size: 12px; color: #8a9aa2; }
.content { padding: 20px 24px; flex: 1; }
.msg-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 13px; }
.msg-head a { color: #237a8b; font-size: 12px; text-decoration: none; }
.msg { padding: 8px 0; border-top: 1px solid #eef2f4; cursor: pointer; }
.msg:hover { background: #f7fbfc; }
.msg .t { font-size: 12.5px; display: flex; justify-content: space-between; }
.msg .c { font-size: 12px; color: #5b6e77; margin-top: 2px; }
.empty { padding: 20px 0; text-align: center; color: #8a9aa2; font-size: 12.5px; }
</style>
