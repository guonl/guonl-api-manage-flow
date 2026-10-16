<template>
  <div class="run-panel">
    <div v-if="!run" class="empty-tip">暂无运行数据</div>
    <template v-else>
      <div class="run-head">
        <el-tag :type="runTag(run.runStatus)" size="small">{{ run.runStatus }}</el-tag>
        <span class="run-meta">耗时 {{ run.totalCostMs != null ? run.totalCostMs + 'ms' : '-' }}</span>
        <span class="run-meta">{{ run.createTime }}</span>
      </div>
      <div v-if="run.errorMsg" class="run-error">{{ run.errorMsg }}</div>

      <div v-for="(s, i) in steps" :key="s.runStepId" class="step-card" :class="'st-' + (s.stepStatus || '').toLowerCase()">
        <div class="step-head" @click="toggle(i)">
          <span class="step-idx">{{ i + 1 }}</span>
          <el-tag :type="stepTag(s.stepStatus)" size="small" class="step-status">{{ s.stepStatus }}</el-tag>
          <span class="step-name">{{ s.stepName }}</span>
          <span v-if="s.responseStatus" class="step-http" :class="s.stepStatus === 'SUCCESS' ? 'ok' : 'bad'">HTTP {{ s.responseStatus }}</span>
          <span class="step-cost">{{ s.costMs != null ? s.costMs + 'ms' : '' }}</span>
          <el-button size="small" link type="primary" @click.stop="rerun(s)">重跑</el-button>
          <el-icon class="step-arrow" :class="{ open: openSet.has(i) }"><ArrowRight /></el-icon>
        </div>
        <div v-if="openSet.has(i)" class="step-body">
          <div v-if="s.errorMsg" class="step-error">{{ s.errorMsg }}</div>

          <template v-if="reqSnapshot(s)">
            <div class="sec-title">请求（变量渲染后）</div>
            <div class="req-line"><b>{{ reqSnapshot(s).method }}</b> {{ reqSnapshot(s).url }}</div>
            <div v-if="reqSnapshot(s).query && Object.keys(reqSnapshot(s).query).length" class="req-line dim">
              Query: {{ JSON.stringify(reqSnapshot(s).query) }}
            </div>
            <pre v-if="reqSnapshot(s).body" class="code-block">{{ pretty(reqSnapshot(s).body) }}</pre>
          </template>
          <div v-else-if="s.requestSnapshot" class="sec-title">等待 {{ delayMs(s) }}ms</div>

          <div v-if="s.responseBody" class="sec-title">响应</div>
          <pre v-if="s.responseBody" class="code-block">{{ pretty(s.responseBody) }}</pre>

          <template v-if="extractResult(s) && Object.keys(extractResult(s)).length">
            <div class="sec-title">提取变量</div>
            <div v-for="(v, k) in extractResult(s)" :key="k" class="kv-line">
              <span class="kv-k">{{ k }}</span> = <span class="kv-v">{{ v == null ? 'null' : v }}</span>
            </div>
          </template>

          <template v-if="assertResult(s) && assertResult(s).length">
            <div class="sec-title">断言</div>
            <div v-for="(a, j) in assertResult(s)" :key="j" class="assert-line">
              <el-tag :type="a.pass ? 'success' : 'danger'" size="small">{{ a.pass ? '通过' : '失败' }}</el-tag>
              <span class="assert-desc">{{ a.desc || a.type }} {{ a.target }} {{ a.expect != null ? '期望 ' + a.expect : '' }}</span>
              <span v-if="a.actual != null" class="assert-actual">实际 {{ a.actual }}</span>
            </div>
          </template>
        </div>
      </div>

      <div v-if="run.contextSnapshot" class="ctx-wrap">
        <div class="sec-title clickable" @click="showCtx = !showCtx">最终上下文变量 {{ showCtx ? '▲' : '▼' }}</div>
        <pre v-if="showCtx" class="code-block">{{ pretty(run.contextSnapshot) }}</pre>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { flowRunDetail, flowRerunStep } from '../api'

const props = defineProps({ runId: { type: [Number, String], required: true } })
const emit = defineEmits(['updated'])

const run = ref(null)
const steps = ref([])
const openSet = new Set([0])
const showCtx = ref(false)

function runTag(s) {
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'RUNNING') return 'warning'
  return 'info'
}
function stepTag(s) {
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'SKIPPED') return 'info'
  return 'warning'
}

const parsedCache = new Map()
function tryParse(key, str) {
  if (!str) return null
  if (!parsedCache.has(key)) {
    try {
      parsedCache.set(key, JSON.parse(str))
    } catch (e) {
      parsedCache.set(key, null)
    }
  }
  return parsedCache.get(key)
}
const reqSnapshot = (s) => tryParse('req' + s.runStepId, s.requestSnapshot)
const extractResult = (s) => tryParse('ext' + s.runStepId, s.extractResult)
const assertResult = (s) => tryParse('ast' + s.runStepId, s.assertResult)
const delayMs = (s) => {
  const r = reqSnapshot(s)
  return r && r.delayMs != null ? r.delayMs : ''
}

function pretty(str) {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch (e) {
    return str
  }
}

function toggle(i) {
  if (openSet.has(i)) openSet.delete(i)
  else openSet.add(i)
}

async function load() {
  const data = await flowRunDetail(props.runId)
  run.value = data.run
  steps.value = data.steps || []
  parsedCache.clear()
  if (!openSet.size && steps.value.length) openSet.add(0)
}

async function rerun(s) {
  await flowRerunStep(props.runId, s.runStepId)
  ElMessage.success('单步重跑完成')
  await load()
  emit('updated')
}

load()
</script>

<style scoped>
.run-panel { font-size: 13px; }
.empty-tip { color: #c0c4cc; padding: 20px 0; text-align: center; }
.run-head { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.run-meta { color: #909399; font-size: 12px; }
.run-error { background: #fef0f0; color: #f56c6c; border-radius: 4px; padding: 8px 12px; margin-bottom: 10px; font-size: 12px; }
.step-card { border: 1px solid var(--el-border-color); border-radius: 6px; margin-bottom: 8px; overflow: hidden; }
.step-card.st-success { border-left: 3px solid #67c23a; }
.step-card.st-failed { border-left: 3px solid #f56c6c; }
.step-card.st-skipped { border-left: 3px solid #c0c4cc; }
.step-head { display: flex; align-items: center; gap: 8px; padding: 8px 12px; cursor: pointer; }
.step-idx { width: 20px; height: 20px; border-radius: 50%; background: #f0f2f5; color: #606266; font-size: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.step-name { flex: 1; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.step-http { font-size: 12px; }
.step-http.ok { color: #67c23a; }
.step-http.bad { color: #f56c6c; }
.step-cost { color: #909399; font-size: 12px; }
.step-arrow { transition: transform 0.2s; color: #c0c4cc; }
.step-arrow.open { transform: rotate(90deg); }
.step-body { padding: 4px 14px 12px 40px; border-top: 1px dashed var(--el-border-color-lighter); }
.step-error { background: #fef0f0; color: #f56c6c; border-radius: 4px; padding: 6px 10px; margin: 8px 0; font-size: 12px; }
.sec-title { color: #909399; font-size: 12px; margin: 10px 0 4px; font-weight: 600; }
.sec-title.clickable { cursor: pointer; }
.code-block { background: var(--el-fill-color-light); border: 1px solid var(--el-border-color-lighter); border-radius: 4px; padding: 8px 10px; font-size: 12px; line-height: 1.5; max-height: 300px; overflow: auto; margin: 0; white-space: pre-wrap; word-break: break-all; }
.req-line { font-size: 12px; margin: 2px 0; word-break: break-all; }
.req-line.dim { color: #909399; }
.kv-line { font-size: 12px; margin: 2px 0; word-break: break-all; }
.kv-k { color: var(--el-color-primary); font-weight: 600; }
.kv-v { color: #67c23a; }
.assert-line { display: flex; align-items: center; gap: 8px; font-size: 12px; margin: 4px 0; }
.assert-desc { flex: 1; }
.assert-actual { color: #909399; max-width: 40%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
