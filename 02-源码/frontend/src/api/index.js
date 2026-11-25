import request from '../utils/request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  info: () => request.get('/auth/info'),
  menus: () => request.get('/auth/menus'),
  updateProfile: (data) => request.put('/profile', data),
  changePassword: (data) => request.put('/profile/password', data),
  messages: () => request.get('/messages'),
  unread: () => request.get('/messages/unread'),
  readAll: () => request.post('/messages/read-all')
}

export const supplierApi = {
  page: (params) => request.get('/supplier/page', { params }),
  detail: (id) => request.get(`/supplier/${id}`),
  create: (data) => request.post('/supplier', data),
  update: (id, data) => request.put(`/supplier/${id}`, data),
  changeStatus: (id, data) => request.post(`/supplier/${id}/status`, data),
  remove: (id) => request.delete(`/supplier/${id}`),
  options: () => request.get('/supplier/options'),
  categories: () => request.get('/supplier/categories')
}

export const certApi = {
  page: (params) => request.get('/certificate/page', { params }),
  bySupplier: (id) => request.get(`/certificate/supplier/${id}`),
  stats: () => request.get('/certificate/stats'),
  save: (data) => request.post('/certificate', data),
  voidCert: (id, data) => request.post(`/certificate/${id}/void`, data),
  refresh: () => request.post('/certificate/refresh')
}

export const admissionApi = {
  page: (params) => request.get('/admission/page', { params }),
  detail: (id) => request.get(`/admission/${id}`),
  create: (data) => request.post('/admission', data),
  update: (id, data) => request.put(`/admission/${id}`, data),
  transition: (id, data) => request.post(`/admission/${id}/transition`, data),
  review: (id, data) => request.post(`/admission/${id}/review`, data),
  assign: (id, data) => request.post(`/admission/${id}/assign`, data)
}

export const performanceApi = {
  page: (params) => request.get('/performance/page', { params }),
  detail: (id) => request.get(`/performance/${id}`),
  create: (data) => request.post('/performance', data),
  score: (id, data) => request.post(`/performance/${id}/score`, data),
  publish: (id) => request.post(`/performance/${id}/publish`),
  voidTask: (id, data) => request.post(`/performance/${id}/void`, data)
}

export const rectApi = {
  page: (params) => request.get('/rectification/page', { params }),
  stats: () => request.get('/rectification/stats'),
  create: (data) => request.post('/rectification', data),
  measure: (id, data) => request.post(`/rectification/${id}/measure`, data),
  verify: (id, data) => request.post(`/rectification/${id}/verify`, data),
  blacklist: (id, data) => request.post(`/rectification/${id}/blacklist`, data)
}

export const modelApi = {
  rule: () => request.get('/model/rule'),
  rules: () => request.get('/model/rules'),
  saveRule: (data) => request.post('/model/rule', data),
  evaluate: (data) => request.post('/model/evaluate', data),
  snapshots: (params) => request.get('/model/snapshots', { params }),
  snapshot: (id) => request.get(`/model/snapshots/${id}`),
  adjust: (id, data) => request.post(`/model/snapshots/${id}/adjust`, data),
  stats: () => request.get('/model/stats')
}

export const dashboardApi = { overview: () => request.get('/dashboard') }

export const systemApi = {
  users: (params) => request.get('/system/users', { params }),
  saveUser: (data) => request.post('/system/users', data),
  toggle: (id, data) => request.post(`/system/users/${id}/status`, data),
  reset: (id) => request.post(`/system/users/${id}/reset`),
  roles: () => request.get('/system/roles'),
  orgs: () => request.get('/system/orgs'),
  menus: (params) => request.get('/system/menus', { params }),
  assign: (data) => request.post('/system/menus/assign', data),
  dicts: (params) => request.get('/system/dicts', { params }),
  logs: (params) => request.get('/system/logs', { params })
}

export const fileApi = {
  uploadUrl: '/api/file/upload'
}
