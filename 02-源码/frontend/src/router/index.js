import { createRouter, createWebHashHistory } from 'vue-router'
import { store, isSupplier } from '../store/user'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '供应风险看板' } },
      { path: 'supplier', component: () => import('../views/SupplierList.vue'), meta: { title: '供应商名录' } },
      { path: 'supplier/:id', component: () => import('../views/SupplierDetail.vue'), meta: { title: '供应商详情' } },
      { path: 'admission', component: () => import('../views/AdmissionList.vue'), meta: { title: '准入申请' } },
      { path: 'certificate', component: () => import('../views/CertificateList.vue'), meta: { title: '证照与预警' } },
      { path: 'performance', component: () => import('../views/PerformanceList.vue'), meta: { title: '绩效评价' } },
      { path: 'rectification', component: () => import('../views/RectificationList.vue'), meta: { title: '整改管理' } },
      { path: 'model', component: () => import('../views/ModelPage.vue'), meta: { title: '动态分层模型' } },
      { path: 'report', component: () => import('../views/ReportPage.vue'), meta: { title: '统计分析' } },
      { path: 'system/user', component: () => import('../views/system/UserPage.vue'), meta: { title: '用户管理' } },
      { path: 'system/role', component: () => import('../views/system/RolePage.vue'), meta: { title: '角色管理' } },
      { path: 'system/menu', component: () => import('../views/system/MenuPage.vue'), meta: { title: '菜单管理' } },
      { path: 'system/dict', component: () => import('../views/system/DictPage.vue'), meta: { title: '字典管理' } },
      { path: 'system/log', component: () => import('../views/system/LogPage.vue'), meta: { title: '操作日志' } },
      { path: 'profile', component: () => import('../views/ProfilePage.vue'), meta: { title: '个人中心' } }
    ]
  },
  {
    path: '/portal',
    component: () => import('../layout/PortalLayout.vue'),
    redirect: '/portal/home',
    children: [
      { path: 'home', component: () => import('../views/portal/PortalHome.vue'), meta: { title: '我的工作台' } },
      { path: 'company', component: () => import('../views/portal/PortalCompany.vue'), meta: { title: '我的企业' } },
      { path: 'apply', component: () => import('../views/portal/PortalApply.vue'), meta: { title: '准入申请' } },
      { path: 'certificate', component: () => import('../views/portal/PortalCertificate.vue'), meta: { title: '我的证照' } },
      { path: 'performance', component: () => import('../views/portal/PortalPerformance.vue'), meta: { title: '我的绩效' } },
      { path: 'rectification', component: () => import('../views/portal/PortalRectification.vue'), meta: { title: '整改任务' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

// 路由守卫：未登录跳登录；外部供应商账号只能进门户，内部账号不进门户
router.beforeEach((to, from, next) => {
  if (to.meta.public) return next()
  if (!store.token) return next('/login')
  if (isSupplier() && !to.path.startsWith('/portal')) return next('/portal/home')
  if (!isSupplier() && to.path.startsWith('/portal')) return next('/dashboard')
  next()
})

export default router
