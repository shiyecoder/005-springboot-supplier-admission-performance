<template>
  <div v-if="supplier">
    <div class="page-head">
      <div><h1>供应商详情</h1><p>{{ supplier.supplierName }} · 主档 / 证照 / 关联单据 / 计算留痕</p></div>
      <div class="right">
        <el-button @click="$router.push('/supplier')">返回名录</el-button>
        <el-button type="primary" @click="evaluate">发起分层计算</el-button>
      </div>
    </div>

    <div class="detail-top">
      <div class="no">{{ supplier.supplierCode }}</div>
      <StatusTag :text="supplier.supplierStatus" />
      <LevelBadge :level="supplier.level" />
      <div class="meta">
        <div>责任人<b>{{ supplier.ownerName || '—' }}</b></div>
        <div>创建时间<b>{{ $time(supplier.createTime) }}</b></div>
        <div>最近更新<b>{{ $time(supplier.updateTime) }}</b></div>
        <div>版本号<b class="num">{{ supplier.version }}</b></div>
      </div>
      <div class="right"><img class="cover" :src="supplier.coverImage || '/uploads/seed/supplier/01.jpg'" alt="厂区" /></div>
    </div>

    <el-tabs v-model="tab" class="detail-tabs">
      <el-tab-pane label="基础信息" name="base">
        <div class="panel">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="企业名称">{{ supplier.supplierName }}</el-descriptions-item>
            <el-descriptions-item label="统一信用代码"><span class="mono">{{ supplier.creditCode }}</span></el-descriptions-item>
            <el-descriptions-item label="供应类别">{{ supplier.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ supplier.contactName }} · {{ supplier.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ supplier.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="风险等级"><StatusTag :text="supplier.riskLevel" /></el-descriptions-item>
            <el-descriptions-item label="准入日期">{{ $date(supplier.admissionDate) }}</el-descriptions-item>
            <el-descriptions-item label="资质到期">{{ $date(supplier.certExpireDate) }}</el-descriptions-item>
            <el-descriptions-item label="年产能">{{ supplier.annualCapacity || '—' }}</el-descriptions-item>
            <el-descriptions-item label="注册地址" :span="2">{{ supplier.address || '—' }}</el-descriptions-item>
            <el-descriptions-item label="主要客户">{{ supplier.mainCustomers || '—' }}</el-descriptions-item>
            <el-descriptions-item label="产能说明" :span="3">{{ supplier.capacityDesc || '—' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-tab-pane>

      <el-tab-pane label="证照与附件" name="cert">
        <div class="panel">
          <div class="panel-head">
            <h2>证照清单</h2>
            <span class="sub">共 {{ certificates.length }} 份 · 过期证照将暂停新增合作类目</span>
          </div>
          <el-table :data="certificates" style="width: 100%">
            <el-table-column prop="certType" label="证照类型" min-width="150" />
            <el-table-column prop="certNo" label="证照编号" min-width="170">
              <template #default="{ row }"><span class="mono">{{ row.certNo }}</span></template>
            </el-table-column>
            <el-table-column label="签发日期" width="110"><template #default="{ row }">{{ $date(row.issueDate) }}</template></el-table-column>
            <el-table-column label="到期日期" width="110"><template #default="{ row }">{{ $date(row.expireDate) }}</template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :text="row.certStatus" /></template></el-table-column>
            <el-table-column prop="versionNo" label="版本" width="70" />
            <el-table-column label="附件" width="100">
              <template #default="{ row }">
                <el-link type="primary" :href="row.filePath" target="_blank">查看</el-link>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="关联单据" name="docs">
        <div class="panel">
          <div class="panel-head"><h2>准入申请</h2></div>
          <el-table :data="applications" style="width: 100%">
            <el-table-column prop="applicationNo" label="申请编号" width="160"><template #default="{ row }"><span class="mono">{{ row.applicationNo }}</span></template></el-table-column>
            <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><StatusTag :text="row.status" /></template></el-table-column>
            <el-table-column label="提交时间" width="160"><template #default="{ row }">{{ $time(row.submitTime) }}</template></el-table-column>
            <el-table-column label="更新时间"><template #default="{ row }">{{ $time(row.updateTime) }}</template></el-table-column>
          </el-table>
        </div>
        <div class="panel">
          <div class="panel-head"><h2>绩效评价</h2></div>
          <el-table :data="tasks" style="width: 100%">
            <el-table-column prop="taskNo" label="任务编号" width="160"><template #default="{ row }"><span class="mono">{{ row.taskNo }}</span></template></el-table-column>
            <el-table-column prop="period" label="周期" width="110" />
            <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><StatusTag :text="row.status" /></template></el-table-column>
            <el-table-column prop="avgScore" label="综合得分" width="100" />
            <el-table-column label="等级" width="70"><template #default="{ row }"><LevelBadge :level="row.finalLevel" /></template></el-table-column>
            <el-table-column label="发布时间"><template #default="{ row }">{{ $time(row.publishedTime) }}</template></el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="等级变化" name="model">
        <div class="panel">
          <div class="panel-head"><h2>分层模型计算记录</h2><span class="sub">最近 12 次 · 点击行查看输入快照与解释</span></div>
          <el-table :data="snapshots" style="width: 100%">
            <el-table-column prop="period" label="周期" width="110" />
            <el-table-column prop="ruleVersion" label="规则版本" width="110" />
            <el-table-column prop="totalScore" label="综合得分" width="100" />
            <el-table-column label="原等级" width="80"><template #default="{ row }"><LevelBadge :level="row.resultLevel" /></template></el-table-column>
            <el-table-column label="最终等级" width="90"><template #default="{ row }"><LevelBadge :level="row.finalLevel" /></template></el-table-column>
            <el-table-column prop="adjustReason" label="人工修正原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="计算时间" width="160"><template #default="{ row }">{{ $time(row.createTime) }}</template></el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import LevelBadge from '../components/LevelBadge.vue'
import { supplierApi, modelApi } from '../api'

const route = useRoute()
const router = useRouter()
const tab = ref('base')
const data = ref({})
const snapshots = ref([])

const supplier = computed(() => data.value.supplier)
const certificates = computed(() => data.value.certificates || [])
const applications = computed(() => data.value.applications || [])
const tasks = computed(() => data.value.tasks || [])

async function load() {
  data.value = await supplierApi.detail(route.params.id)
  snapshots.value = (await modelApi.snapshots({ supplierId: route.params.id, page: 1, size: 12 })).records
}

async function evaluate() {
  const res = await modelApi.evaluate({ supplierId: Number(route.params.id), period: '' })
  ElMessage.success(`计算完成：${res.snapshot.totalScore} 分，等级 ${res.snapshot.resultLevel}`)
  await load()
  tab.value = 'model'
}

onMounted(load)
</script>

<style scoped>
.detail-top {
  background: #fff; border: 1px solid #dce5e8; border-radius: 8px; padding: 14px 18px;
  display: flex; align-items: center; gap: 16px; margin-bottom: 8px;
}
.detail-top .no { font-size: 17px; font-weight: 600; font-family: ui-monospace, Menlo, monospace; }
.detail-top .meta { display: flex; gap: 22px; font-size: 12.5px; color: #5b6e77; }
.detail-top .meta b { display: block; font-weight: 500; color: #1c2b33; font-size: 13px; }
.detail-top .right { margin-left: auto; }
.cover { width: 108px; height: 74px; object-fit: cover; border-radius: 6px; border: 1px solid #dce5e8; }
.detail-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
</style>
