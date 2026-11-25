<template>
  <div>
    <div class="page-head">
      <div><h1>整改任务</h1><p>收到整改任务后请在截止日期前提交措施，采购专员验证通过后关闭</p></div>
    </div>

    <div class="pcard">
      <div class="panel-head"><h2>整改清单</h2><span class="sub">共 {{ total }} 条</span></div>
      <el-table :data="rows" style="width: 100%">
        <el-table-column label="整改编号" width="150"><template #default="{ row }"><span class="mono">{{ row.rectificationNo }}</span></template></el-table-column>
        <el-table-column prop="source" label="来源" width="110" />
        <el-table-column prop="issueDesc" label="问题描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag :text="row.status" /></template></el-table-column>
        <el-table-column label="截止日期" width="120"><template #default="{ row }">{{ $date(row.deadline) }}</template></el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="['待提交措施', '已逾期'].includes(row.status)" link type="primary" @click="openSubmit(row)">提交措施</el-button>
            <el-button v-else link type="primary" @click="openView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="load" @size-change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="dialogTitle" width="560px">
      <div v-if="current" class="pcard" style="margin: 0 0 12px; background: #fbfdfd">
        <div class="sub2">问题描述</div>
        <div style="font-size: 13px; margin-bottom: 8px">{{ current.issueDesc }}</div>
        <div class="sub2">当前措施</div>
        <div style="font-size: 13px">{{ current.measure || '尚未提交' }}</div>
        <div v-if="current.verifyComment" style="margin-top: 8px">
          <div class="sub2">验证意见</div>
          <div style="font-size: 13px">{{ current.verifyComment }}</div>
        </div>
      </div>
      <el-input v-if="editable" v-model="measure" type="textarea" :rows="4" placeholder="请说明具体措施、责任人与完成时间（不少于 5 个字）" />
      <template #footer>
        <el-button @click="dialog = false">关闭</el-button>
        <el-button v-if="editable" type="primary" @click="submit">提交措施</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { rectApi } from '../../api'

const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const dialog = ref(false)
const current = ref(null)
const measure = ref('')
const editable = ref(false)
const dialogTitle = computed(() => (editable.value ? '提交整改措施' : '整改详情'))

async function load() {
  const res = await rectApi.page(query)
  rows.value = res.records
  total.value = res.total
}

function openSubmit(row) {
  current.value = row
  measure.value = row.measure || ''
  editable.value = true
  dialog.value = true
}

function openView(row) {
  current.value = row
  editable.value = false
  dialog.value = true
}

async function submit() {
  try {
    await rectApi.measure(current.value.id, { measure: measure.value })
    ElMessage.success('措施已提交，等待采购专员验证')
    dialog.value = false
    load()
  } catch (e) { /* 已提示 */ }
}

onMounted(load)
</script>

<style scoped>
.pcard { background: #fff; border: 1px solid #dce5e8; border-radius: 8px; padding: 18px 20px; margin-bottom: 16px; }
</style>
