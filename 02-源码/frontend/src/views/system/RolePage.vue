<template>
  <div>
    <div class="page-head">
      <div><h1>角色管理</h1><p>菜单权限与数据范围分别控制；「菜单管理」与「分配菜单」不在侧栏展示</p></div>
    </div>
    <div class="cols">
      <div class="panel">
        <div class="panel-head"><h2>角色列表</h2><span class="sub">共 {{ roles.length }} 个</span></div>
        <div v-for="r in roles" :key="r.id" class="role" :class="{ on: current?.id === r.id }" @click="select(r)">
          <div class="n">{{ r.roleName }}<span class="sub2" style="margin-left: 6px">{{ r.roleCode }}</span></div>
          <div class="d">数据范围：{{ scopeText(r.dataScope) }} · {{ r.remark }}</div>
        </div>
        <div class="note">数据范围：全部数据 / 本部门 / 本部门及下级 / 仅本人。供应商联系人固定为仅本人（本企业）。</div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <h2>为「{{ current?.roleName || '—' }}」分配菜单</h2>
          <span class="sub">已选 {{ checked.length }} 项</span>
        </div>
        <el-tree
          ref="treeRef"
          :data="tree"
          show-checkbox
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          :default-checked-keys="checked"
          default-expand-all
          style="max-height: 460px; overflow: auto"
        />
        <div class="note" style="margin-top: 12px">
          保存后该角色用户重新登录即可看到新菜单；按钮权限只控制前端展示，写接口仍由后端校验角色。
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 14px">
          <el-button @click="loadMenus">重置</el-button>
          <el-button type="primary" :disabled="current?.roleCode === 'admin'" @click="save">保存权限</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '../../api'

const roles = ref([])
const current = ref(null)
const menus = ref([])
const checked = ref([])
const treeRef = ref()

const scopeText = (s) => ({ all: '全部数据', dept: '本部门', deptAndChild: '本部门及下级', self: '仅本人' }[s] || s)

// 按 parentId 组装菜单树，visible=0 的节点也展示（便于管理员配置，但不在侧栏出现）
function buildTree(list) {
  const nodes = list.map((m) => ({ ...m, name: m.name, children: [] }))
  const index = new Map(nodes.map((n) => [n.id, n]))
  const roots = []
  nodes.forEach((n) => {
    if (n.parentId && index.has(n.parentId)) index.get(n.parentId).children.push(n)
    else roots.push(n)
  })
  return roots
}

async function loadMenus() {
  menus.value = await systemApi.menus({ roleId: current.value?.id })
  checked.value = menus.value.filter((m) => m.checked).map((m) => m.id)
}

async function select(r) {
  current.value = r
  await loadMenus()
}

async function save() {
  const ids = [...treeRef.value.getCheckedKeys(), ...treeRef.value.getHalfCheckedKeys()]
  await systemApi.assign({ roleId: current.value.id, menuIds: ids })
  ElMessage.success('权限已保存')
}

onMounted(async () => {
  roles.value = await systemApi.roles()
  await select(roles.value[0])
  menus.value = await systemApi.menus({ roleId: roles.value[0].id })
  checked.value = menus.value.filter((m) => m.checked).map((m) => m.id)
  tree.value = buildTree(menus.value)
})
const tree = ref([])
</script>

<style scoped>
.cols { display: grid; grid-template-columns: 1fr 1.1fr; gap: 16px; align-items: start; }
.role { padding: 11px 13px; border: 1px solid #dce5e8; border-radius: 6px; margin-bottom: 8px; cursor: pointer; }
.role.on { border-color: #237a8b; background: rgba(35, 122, 139, 0.05); }
.role .n { font-size: 13.5px; font-weight: 600; }
.role .d { font-size: 12px; color: #8a9aa2; margin-top: 2px; }
</style>
