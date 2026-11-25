<template>
  <span class="status-tag" :class="tone">{{ text }}</span>
</template>

<script setup>
import { computed } from 'vue'

// 状态双编码：颜色 + 中文标签。不允许只靠颜色表达状态。
const props = defineProps({ text: { type: String, default: '' } })

const GREEN = ['合格', '已准入', '已完成', '已发布', '有效', '已关闭', '启用', '低', '通过']
const ORANGE = ['准入审核', '现场评估', '待审批', '评分中', '待评分', '待验证', '待提交措施', '限期整改', '30 天内到期', '中', '整改中', '评分中']
const RED = ['已驳回', '黑名单', '暂停', '已过期', '已逾期', '高', '已作废', '已转黑名单', '不通过']
const GREY = ['待提交', '草稿', '已退出', '潜在', '停用', '—']

const tone = computed(() => {
  const t = props.text || ''
  if (GREEN.includes(t)) return 'ok'
  if (RED.includes(t)) return 'bad'
  if (ORANGE.includes(t)) return 'warn'
  if (GREY.includes(t)) return 'mute'
  return 'info'
})
</script>

<style scoped>
.status-tag {
  display: inline-flex; align-items: center; height: 22px; padding: 0 8px;
  border-radius: 4px; font-size: 12px; white-space: nowrap;
}
.ok { background: rgba(45, 122, 88, 0.1); color: #2d7a58; }
.warn { background: rgba(169, 112, 31, 0.12); color: #a9701f; }
.bad { background: rgba(176, 48, 53, 0.1); color: #b03035; }
.info { background: rgba(35, 122, 139, 0.1); color: #237a8b; }
.mute { background: #edf2f4; color: #5b6e77; }
</style>
