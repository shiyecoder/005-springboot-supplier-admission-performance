<template>
  <div>
    <div class="page-head">
      <div><h1>菜单管理</h1><p>菜单编码、路径与按钮权限 · 本页仅管理员可见，侧边栏不展示</p></div>
    </div>
    <div class="panel">
      <div class="note">
        菜单管理用于维护导航与按钮权限码。为避免误操作打乱导航，本页面与「分配菜单」不在侧边栏出现，
        仅通过角色管理页的分配入口调整；新增菜单需同时配置角色权限后才会生效。
      </div>
    </div>
    <div class="panel">
      <div class="panel-head"><h2>菜单清单</h2><span class="sub">缩进表示层级</span></div>
      <el-table :data="rows" style="width: 100%">
        <el-table-column label="菜单名称" min-width="220">
          <template #default="{ row }">
            <span :style="{ paddingLeft: (level(row) * 18) + 'px' }">
              <el-icon v-if="row.type === 'menu'" style="vertical-align: -2px; margin-right: 4px"><Menu /></el-icon>
              {{ row.parentId ? '└ ' : '' }}{{ row.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">{{ row.type === 'menu' ? '导航' : '按钮' }}</template>
        </el-table-column>
        <el-table-column prop="perms" label="权限标识" width="180">
          <template #default="{ row }"><span class="mono">{{ row.perms || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="侧栏展示" width="100">
          <template #default="{ row }">{{ row.visible === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="id" label="菜单 ID" width="90" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { systemApi } from '../../api'

const rows = ref([])
const parents = ref(new Set())
const level = (row) => (row.parentId ? 1 : 0)

onMounted(async () => {
  rows.value = await systemApi.menus({})
  parents.value = new Set(rows.value.filter((r) => !r.parentId).map((r) => r.id))
})
</script>
