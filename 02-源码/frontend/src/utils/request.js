import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一请求实例：携带 JWT，按业务码分流，401 跳登录
const request = axios.create({ baseURL: '/api', timeout: 20000 })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('sa_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code === 200) return body.data
    if (body && body.code === 401) {
      localStorage.removeItem('sa_token')
      ElMessage.warning(body.message || '登录已失效，请重新登录')
      if (location.hash !== '#/login') location.hash = '#/login'
      return Promise.reject(new Error(body.message))
    }
    ElMessage.error(body?.message || '请求失败')
    return Promise.reject(new Error(body?.message || '请求失败'))
  },
  (err) => {
    ElMessage.error(err.response?.data?.message || '网络异常，请稍后重试')
    return Promise.reject(err)
  }
)

export default request
