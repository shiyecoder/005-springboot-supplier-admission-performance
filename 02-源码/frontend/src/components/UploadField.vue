<template>
  <div class="upload-field">
    <el-upload
      :action="uploadUrl"
      :headers="headers"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
      accept=".jpg,.jpeg,.png,.pdf"
      drag
    >
      <div v-if="modelValue" class="preview">
        <img v-if="isImage" :src="modelValue" alt="附件" />
        <div v-else class="file">PDF 附件</div>
        <div class="meta">
          <div class="name">{{ fileName || modelValue.split('/').pop() }}</div>
          <div class="hint">点击可重新上传</div>
        </div>
      </div>
      <div v-else class="empty">
        <el-icon><UploadFilled /></el-icon>
        <div>点击或拖拽上传（JPG/PNG/PDF，≤20MB）</div>
      </div>
    </el-upload>
    <div v-if="tip" class="hint" style="margin-top: 6px">{{ tip }}</div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fileApi } from '../api'

// 统一上传组件：禁止手填 URL，后端返回相对路径 /uploads/...
const props = defineProps({
  modelValue: { type: String, default: '' },
  bizType: { type: String, default: 'certificate' },
  tip: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'uploaded'])
const fileName = ref('')
const uploadUrl = fileApi.uploadUrl
const headers = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('sa_token')}` }))
const isImage = computed(() => !/\.pdf$/i.test(props.modelValue || ''))

function beforeUpload(file) {
  const ok = /\.(jpg|jpeg|png|pdf)$/i.test(file.name)
  if (!ok) ElMessage.error('仅支持 jpg/png/pdf')
  if (file.size > 20 * 1024 * 1024) ElMessage.error('文件不能超过 20MB')
  return ok && file.size <= 20 * 1024 * 1024
}

function onSuccess(res) {
  if (res.code === 200) {
    fileName.value = res.data.fileName
    emit('update:modelValue', res.data.url)
    emit('uploaded', res.data)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

function onError() {
  ElMessage.error('上传失败，请重试')
}
</script>

<style scoped>
.upload-field :deep(.el-upload) { width: 100%; }
.upload-field :deep(.el-upload-dragger) {
  padding: 14px; border-radius: 8px; border-color: #c9d6da; background: #fbfdfd;
}
.preview { display: flex; align-items: center; gap: 12px; text-align: left; }
.preview img { width: 60px; height: 44px; object-fit: cover; border-radius: 4px; border: 1px solid #dce5e8; }
.preview .file {
  width: 60px; height: 44px; display: grid; place-items: center; background: #eaf1f3;
  color: #8a9aa2; font-size: 11px; border-radius: 4px;
}
.preview .name { font-size: 13px; }
.empty { color: #8a9aa2; font-size: 12.5px; }
.empty .el-icon { font-size: 22px; margin-bottom: 4px; }
</style>
