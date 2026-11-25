<template>
  <div>
    <div class="page-head">
      <div><h1>我的绩效</h1><p>评价结果发布后可见；如对结果有异议，可联系采购专员申请复核</p></div>
    </div>

    <div class="pcard">
      <div class="panel-head"><h2>评价记录</h2><span class="sub">共 {{ total }} 条</span></div>
      <el-table :data="rows" @row-click="openDetail" style="width: 100%">
        <el-table-column label="任务编号" width="160"><template #default="{ row }"><span class="mono">{{ row.taskNo }}</span></template></el-table-column>
        <el-table-column prop="period" label="周期" width="120" />
        <el-table-column prop="periodType" label="类型" width="90" />
        <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag :text="row.status" /></template></el-table-column>
        <el-table-column label="综合得分" width="110"><template #default="{ row }"><span class="num">{{ row.avgScore ?? '—' }}</span></template></el-table-column>
        <el-table-column label="等级" width="80"><template #default="{ row }"><LevelBadge :level="row.finalLevel" /></template></el-table-column>
        <el-table-column label="发布时间" width="170"><template #default="{ row }">{{ $time(row.publishedTime) }}</template></el-table-column>
        <el-table-column prop="deadline" label="评分截止" width="120" />
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="load" @size-change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer" size="620px" :title="detail?.task?.taskNo || '评价详情'">
      <div v-if="detail">
        <div class="pcard" style="margin: 0 0 14px">
          <div class="row4">
            <div><div class="sub2">周期</div><div class="big">{{ detail.task.period }}</div></div>
            <div><div class="sub2">综合得分</div><div class="big num">{{ detail.task.avgScore ?? '—' }}</div></div>
            <div><div class="sub2">等级</div><LevelBadge :level="detail.task.finalLevel" /></div>
            <div><div class="sub2">状态</div><StatusTag :text="detail.task.status" /></div>
          </div>
        </div>
        <div class="pcard" style="margin: 0">
          <div class="panel-head"><h2>评分明细</h2><span class="sub">多人评分的均分</span></div>
          <el-table :data="detail.scores" size="small">
            <el-table-column prop="reviewerName" label="评分人" width="90" />
            <el-table-column prop="qualityScore" label="质量" width="70" />
            <el-table-column prop="deliveryScore" label="交付" width="70" />
            <el-table-column prop="responseScore" label="响应" width="70" />
            <el-table-column prop="priceScore" label="价格" width="70" />
            <el-table-column prop="complaintScore" label="投诉" width="70" />
            <el-table-column prop="totalScore" label="总分" width="70" />
            <el-table-column prop="comment" label="说明" min-width="140" show-overflow-tooltip />
          </el-table>
          <div class="note" style="margin-top: 12px">
            评分发布后不可修改。若发现数据有误，请提供凭证联系采购专员，由其作废并重新发起评价。
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import StatusTag from '../../components/StatusTag.vue'
import LevelBadge from '../../components/LevelBadge.vue'
import { performanceApi } from '../../api'

const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const drawer = ref(false)
const detail = ref(null)

async function load() {
  const res = await performanceApi.page(query)
  rows.value = res.records
  total.value = res.total
}

async function openDetail(row) {
  detail.value = await performanceApi.detail(row.id)
  drawer.value = true
}

onMounted(load)
</script>

<style scoped>
.pcard { background: #fff; border: 1px solid #dce5e8; border-radius: 8px; padding: 18px 20px; margin-bottom: 16px; }
.row4 { display: flex; gap: 32px; align-items: center; }
.row4 .big { font-size: 16px; font-weight: 600; }
</style>
