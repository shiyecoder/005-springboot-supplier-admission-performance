<template>
  <div>
    <div class="page-head">
      <div><h1>字典管理</h1><p>下拉框一律取字典，禁止前端提交未定义的值</p></div>
    </div>
    <div class="panel">
      <div class="filter-row">
        <el-select v-model="type" placeholder="字典类型" clearable style="width: 220px">
          <el-option v-for="t in types" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>
    </div>
    <div v-for="(items, key) in groups" :key="key" class="panel">
      <div class="panel-head"><h2>{{ label(key) }}</h2><span class="sub">{{ key }} · {{ items.length }} 项</span></div>
      <div class="tags">
        <span v-for="it in items" :key="it.code" class="dtag">{{ it.label }}<em>{{ it.code }}</em></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { systemApi } from '../../api'

const types = ['supplier_status', 'risk_level', 'level', 'period_type', 'cert_type', 'admission_status', 'rect_source']
const LABEL = {
  supplier_status: '合作状态', risk_level: '风险等级', level: '供应商等级',
  period_type: '评价周期类型', cert_type: '证照类型', admission_status: '准入状态', rect_source: '整改来源'
}
const type = ref('')
const groups = ref({})
const label = (k) => LABEL[k] || k

async function load() {
  groups.value = await systemApi.dicts({ type: type.value || undefined })
}
onMounted(load)
</script>

<style scoped>
.tags { display: flex; flex-wrap: wrap; gap: 8px; }
.dtag {
  display: inline-flex; align-items: center; gap: 6px; padding: 5px 10px; border-radius: 4px;
  background: #f3f7f8; border: 1px solid #dce5e8; font-size: 12.5px;
}
.dtag em { font-style: normal; color: #8a9aa2; font-size: 11.5px; }
</style>
