import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './assets/global.css'
import { formatTime, formatDate, timeAgo } from './utils/format'

const app = createApp(App)

// 图标全量注册，菜单图标按名称动态渲染
for (const [key, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, comp)
}

// 全局时间格式化：全系统统一调用，禁止各页面自己拼日期
app.config.globalProperties.$time = formatTime
app.config.globalProperties.$date = formatDate
app.config.globalProperties.$ago = timeAgo
app.provide('$time', formatTime)
app.provide('$date', formatDate)

app.use(ElementPlus, { locale: zhCn, size: 'default' })
app.use(router)
app.mount('#app')
