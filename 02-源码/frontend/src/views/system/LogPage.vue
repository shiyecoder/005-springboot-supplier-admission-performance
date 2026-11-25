<template>
  <div>
    <div class="page-head">
      <div><h1>操作日志</h1><p>登录、流转、审批、人工修正等关键动作留痕，普通用户不可修改</p></div>
    </div>
    <div class="panel">
      <div class="filter-row">
        <el-input v-model="query.kw" placeholder="账号 / 业务编号" clearable @keyup.enter="search" />
        <el-select v-model="query.module" placeholder="模块" clearable>
          <el-option v-for="m in ['认证', '准入', '供应商', '证照', '绩效', '整改', '分层模型', '系统', '个人中心']" :key="m" :label="m" :value="m" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>
    <div class="panel">
      <div class="panel-head"><h2>日志明细</h2><div class="right"><el-tag type="info" effect="plain">共 {{ total }} 条</el-tag></div></div>
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="时间" width="160"><template #default="{ row }">{{ $time(row.createTime) }}</template></el-table-column>
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="动作" width="130" />
        <el-table-column label="业务编号" width="170"><template #default="{ row }"><span class="mono">{{ row.bizNo || '—' }}</span></template></el-table-column>
        <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="120" />
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="load" @size-change="search" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { systemApi } from '../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, kw: '', module: '' })

async function load() {
  loading.value = true
  try {
    const res = await systemApi.logs(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }
const reset = () => { Object.assign(query, { page: 1, kw: '', module: '' }); load() }
onMounted(load)
</script>
