<template>
  <div class="page-card">
    <div class="head-bar">
      <el-button @click="$router.push('/flow')"><el-icon><Back /></el-icon>流程列表</el-button>
      <span class="title">{{ run?.flowNameSnapshot || '运行详情' }}</span>
      <span class="run-id">Run #{{ runId }}</span>
      <el-tag v-if="run" :type="run.runStatus === 'SUCCESS' ? 'success' : run.runStatus === 'FAILED' ? 'danger' : 'warning'" size="small">
        {{ run.runStatus }}
      </el-tag>
    </div>
    <RunPanel :key="refreshKey" :run-id="runId" @updated="refreshKey++" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { flowRunDetail } from '../api'
import RunPanel from './RunPanel.vue'

const r = useRoute()
const runId = computed(() => r.params.runId)
const run = ref(null)
const refreshKey = ref(0)

flowRunDetail(runId.value).then((data) => {
  run.value = data.run
}).catch(() => {})
</script>

<style scoped>
.page-card { background: var(--el-bg-color); border-radius: 8px; padding: 16px; }
.head-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.title { font-size: 16px; font-weight: 600; }
.run-id { color: #909399; font-size: 12px; }
</style>
