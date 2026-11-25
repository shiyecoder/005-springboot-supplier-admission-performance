import { reactive } from 'vue'
import { authApi } from '../api'

// 轻量全局状态：登录态、用户信息、动态菜单
export const store = reactive({
  token: localStorage.getItem('sa_token') || '',
  user: JSON.parse(localStorage.getItem('sa_user') || 'null'),
  menus: JSON.parse(localStorage.getItem('sa_menus') || '[]'),
  unread: 0
})

export async function login(payload) {
  const data = await authApi.login(payload)
  store.token = data.token
  store.user = data.user
  store.menus = data.menus || []
  localStorage.setItem('sa_token', data.token)
  localStorage.setItem('sa_user', JSON.stringify(data.user))
  localStorage.setItem('sa_menus', JSON.stringify(data.menus || []))
  return data.user
}

export async function loadInfo() {
  if (!store.token) return null
  const user = await authApi.info()
  store.user = user
  localStorage.setItem('sa_user', JSON.stringify(user))
  const menus = await authApi.menus()
  store.menus = menus || []
  localStorage.setItem('sa_menus', JSON.stringify(menus || []))
  return user
}

export function logout() {
  store.token = ''
  store.user = null
  store.menus = []
  localStorage.removeItem('sa_token')
  localStorage.removeItem('sa_user')
  localStorage.removeItem('sa_menus')
}

export function isSupplier() {
  return store.user?.roleCode === 'supplier'
}

// 按钮权限：只有分配了对应按钮权限的角色才显示
export function hasPerm(perm) {
  const role = store.user?.roleCode
  if (role === 'admin') return true
  const map = {
    buyer: ['admission:reject', 'admission:score', 'model:rule', 'model:adjust', 'common:export'],
    reviewer: ['admission:score'],
    supplier: []
  }
  return (map[role] || []).includes(perm)
}
