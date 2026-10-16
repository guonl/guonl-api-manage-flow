<template>
  <div class="editor-wrap">
    <div class="top-bar">
      <el-button @click="$router.push('/flow')"><el-icon><Back /></el-icon>返回</el-button>
      <el-input v-model="flow.flowName" style="width:260px" placeholder="流程名称">
        <template #prefix><el-icon><EditPen /></el-icon></template>
      </el-input>
      <el-switch v-model="flow.flowStatus" active-value="1" inactive-value="0" active-text="启用" />
      <span class="saved-tip">{{ savedTip }}</span>
      <div style="flex:1" />
      <el-button type="warning" plain @click="openAi"><el-icon><MagicStick /></el-icon>AI 生成流程</el-button>
      <el-button type="primary" :loading="saving" @click="save"><el-icon><Check /></el-icon>保存</el-button>
    </div>

    <div class="cols">
      <!-- 左：步骤列表 -->
      <div class="col-steps">
        <div class="col-title">
          <span>步骤（{{ steps.length }}）</span>
          <el-button size="small" type="primary" link @click="addStep">+ 添加</el-button>
        </div>
        <div class="step-list">
          <div v-for="(s, i) in steps" :key="i" class="step-item" :class="{ active: i === curIndex, disabled: !isEnabled(s) }" @click="curIndex = i">
            <div class="si-top">
              <span class="si-idx">{{ i + 1 }}</span>
              <el-tag v-if="s.stepType === 'DELAY'" size="small" type="warning" class="si-method">DELAY</el-tag>
              <el-tag v-else size="small" :type="methodTag(s.requestMethod)" class="si-method">{{ s.requestMethod || 'GET' }}</el-tag>
              <span class="si-name">{{ s.stepName || '未命名步骤' }}</span>
            </div>
            <div class="si-url">{{ s.stepType === 'DELAY' ? '等待 ' + (s.delayMsNum || s.delayMs || 0) + 'ms' : (s.requestUrl || '未配置 URL') }}</div>
            <div class="si-ops" @click.stop>
              <el-button size="small" link @click="moveStep(i, -1)" :disabled="i === 0"><el-icon><Top /></el-icon></el-button>
              <el-button size="small" link @click="moveStep(i, 1)" :disabled="i === steps.length - 1"><el-icon><Bottom /></el-icon></el-button>
              <el-button size="small" link @click="copyStep(i)"><el-icon><CopyDocument /></el-icon></el-button>
              <el-button size="small" link type="danger" @click="removeStep(i)"><el-icon><Delete /></el-icon></el-button>
            </div>
          </div>
          <div v-if="!steps.length" class="empty-tip">还没有步骤，点击「+ 添加」创建第一步</div>
        </div>
      </div>

      <!-- 中：步骤配置 -->
      <div class="col-config">
        <StepConfig :step="steps[curIndex] || null" />
      </div>

      <!-- 右：运行 -->
      <div class="col-run">
        <div class="col-title"><span>运行</span></div>
        <div class="run-form">
          <el-select v-model="runEnvId" placeholder="目标环境（默认取项目默认）" clearable size="small" style="width:100%">
            <el-option v-for="e in envs" :key="e.envId" :label="`${e.envName}（${e.baseUrl}）`" :value="e.envId" />
          </el-select>
          <el-input v-model="runVarsText" type="textarea" :rows="3" size="small" placeholder='初始变量 JSON，如 {"orderId": 123}' style="margin-top:8px; font-family:monospace" />
          <div class="run-ops">
            <el-checkbox v-model="runDebug" size="small">调试模式（失败即停）</el-checkbox>
            <el-button type="success" size="small" :loading="running" @click="doRun"><el-icon><VideoPlay /></el-icon>{{ running ? '运行中…' : '运行' }}</el-button>
          </div>
        </div>
        <div class="run-result">
          <RunPanel v-if="runId" :key="runId" :run-id="runId" @updated="onRerunUpdated" />
          <div v-else class="empty-tip" style="text-align:center; padding:20px 0">运行后在此查看每步结果</div>
        </div>
      </div>
    </div>

    <!-- AI 生成流程 -->
    <el-dialog v-model="aiVisible" title="AI 生成流程步骤" width="660px">
      <el-input v-model="aiReq" type="textarea" :rows="4"
                placeholder="用自然语言描述要编排的流程，例如：先创建一个测试项目，然后分页查询项目列表验证创建成功" />
      <div class="form-tip" style="margin:6px 0 10px">
        AI 将基于当前项目的接口清单生成步骤（自动提取变量、生成断言），应用后可继续手工调整。
      </div>
      <div style="text-align:right; margin-bottom:10px">
        <el-button type="primary" :loading="aiLoading" @click="genFlow"><el-icon><MagicStick /></el-icon>生成步骤</el-button>
      </div>
      <div v-loading="aiLoading" style="min-height:60px">
        <div v-for="(s, i) in aiSteps" :key="i" class="ai-step">
          <el-tag size="small" :type="methodTag(s.requestMethod)">{{ s.requestMethod || 'GET' }}</el-tag>
          <span class="ai-step-name">{{ s.stepName || '未命名步骤' }}</span>
          <span class="ai-step-url">{{ s.requestUrl }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="aiVisible = false">取消</el-button>
        <el-button :disabled="!aiSteps.length" @click="applyAiSteps(true)">替换全部步骤</el-button>
        <el-button type="primary" :disabled="!aiSteps.length" @click="applyAiSteps(false)">追加到末尾</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { envList, flowDetail, flowUpdate, flowRun, flowRunPage, aiGenFlow } from '../api'
import StepConfig from './StepConfig.vue'
import RunPanel from './RunPanel.vue'

const r = useRoute()
const flowId = r.params.id

const flow = reactive({ flowId: null, projectId: null, flowName: '', flowDesc: '', flowStatus: '1' })
const steps = ref([])
const curIndex = ref(0)
const savedTip = ref('')
const saving = ref(false)

const envs = ref([])
const runEnvId = ref(null)
const runVarsText = ref('')
const runDebug = ref(false)
const running = ref(false)
const runId = ref(null)

function isEnabled(s) {
  return !(s.enabledBool === false || s.enabled === 0 || s.enabled === '0')
}

function methodTag(m) {
  const t = (m || '').toUpperCase()
  if (t === 'GET') return 'success'
  if (t === 'POST') return 'warning'
  if (t === 'DELETE') return 'danger'
  return 'primary'
}

function newStep() {
  return {
    stepId: null,
    stepName: '步骤 ' + (steps.value.length + 1),
    stepType: 'HTTP',
    urlId: null,
    requestMethod: 'GET',
    requestUrl: '',
    requestHeaders: '{"Content-Type":"application/json"}',
    requestQuery: '{}',
    bodyType: 'none',
    requestBody: '',
    extractRules: '[]',
    assertions: '[]',
    onFailure: 'TERMINATE',
    delayMs: 0,
    enabled: 1
  }
}

function addStep() {
  steps.value.push(newStep())
  curIndex.value = steps.value.length - 1
}

function moveStep(i, dir) {
  const arr = steps.value
  const j = i + dir
  if (j < 0 || j >= arr.length) return
  const t = arr[i]
  arr[i] = arr[j]
  arr[j] = t
  if (curIndex.value === i) curIndex.value = j
  else if (curIndex.value === j) curIndex.value = i
}

function copyStep(i) {
  const copy = JSON.parse(JSON.stringify(steps.value[i]))
  copy.stepId = null
  copy.stepName = (copy.stepName || '步骤') + ' 副本'
  steps.value.splice(i + 1, 0, copy)
  curIndex.value = i + 1
}

async function removeStep(i) {
  await ElMessageBox.confirm(`确认删除步骤「${steps.value[i].stepName}」？`, '删除确认', { type: 'warning' })
  steps.value.splice(i, 1)
  if (curIndex.value >= steps.value.length) curIndex.value = steps.value.length - 1
}

function payload() {
  return {
    flowId: flow.flowId,
    projectId: flow.projectId,
    flowName: flow.flowName,
    flowDesc: flow.flowDesc,
    flowStatus: flow.flowStatus,
    steps: steps.value.map((s) => ({
      stepId: s.stepId || null,
      stepName: s.stepName,
      stepType: s.stepType || 'HTTP',
      urlId: s.urlId || null,
      requestMethod: s.requestMethod || 'GET',
      requestUrl: s.requestUrl || '',
      requestHeaders: s.requestHeaders || '{}',
      requestQuery: s.requestQuery || '{}',
      bodyType: s.bodyType || 'none',
      requestBody: s.requestBody || '',
      extractRules: s.extractRules || '[]',
      assertions: s.assertions || '[]',
      onFailure: s.onFailure || 'TERMINATE',
      delayMs: s.delayMsNum != null ? s.delayMsNum : Number(s.delayMs || 0),
      enabled: isEnabled(s) ? 1 : 0
    }))
  }
}

async function save() {
  if (!flow.flowName) return ElMessage.warning('请输入流程名称')
  saving.value = true
  try {
    await flowUpdate(payload())
    savedTip.value = '已保存 ' + new Date().toLocaleTimeString()
    await load(false)
    ElMessage.success('流程已保存')
  } finally {
    saving.value = false
  }
}

async function load(withSteps = true) {
  const data = await flowDetail(flowId)
  Object.assign(flow, data.flow || {})
  if (withSteps) {
    steps.value = (data.steps || []).map((s) => {
      const copy = { ...s }
      copy.enabledBool = !(Number(s.enabled) === 0)
      copy.delayMsNum = Number(s.delayMs || 0)
      return copy
    })
    curIndex.value = 0
  }
}

async function doRun() {
  if (!steps.value.length) return ElMessage.warning('请先添加步骤')
  let initVars = {}
  if (runVarsText.value && runVarsText.value.trim()) {
    try {
      initVars = JSON.parse(runVarsText.value)
    } catch (e) {
      return ElMessage.warning('初始变量不是合法 JSON')
    }
  }
  running.value = true
  try {
    await save()
    const res = await flowRun({ flowId: Number(flowId), envId: runEnvId.value, initVars, mode: runDebug.value ? 'DEBUG' : 'NORMAL' })
    runId.value = res.runId
    ElMessage.success(`执行完成：${res.runStatus}（${res.totalCostMs}ms）`)
  } finally {
    running.value = false
  }
}

function onRerunUpdated() {
  /* 单步重跑后刷新整体结果由 RunPanel 内部处理 */
}

/* ---------- AI 生成流程 ---------- */
const aiVisible = ref(false)
const aiReq = ref('')
const aiLoading = ref(false)
const aiSteps = ref([])

function openAi() {
  if (!flow.projectId) return ElMessage.warning('流程未关联项目，无法使用 AI 生成')
  aiReq.value = ''
  aiSteps.value = []
  aiVisible.value = true
}

async function genFlow() {
  if (!aiReq.value.trim()) return ElMessage.warning('请描述你想要实现的流程')
  aiLoading.value = true
  aiSteps.value = []
  try {
    const r = await aiGenFlow({ projectId: flow.projectId, requirement: aiReq.value })
    aiSteps.value = r.steps || []
    if (!aiSteps.value.length) ElMessage.warning('AI 未生成有效步骤，请调整描述后重试')
  } finally {
    aiLoading.value = false
  }
}

/** 将 AI 生成的步骤转换为编辑器步骤格式 */
function toEditorStep(s, offset) {
  const str = (v, dft) => (typeof v === 'string' ? v : JSON.stringify(v || dft))
  return {
    stepId: null,
    stepName: s.stepName || ('步骤 ' + (steps.value.length + offset + 1)),
    stepType: 'HTTP',
    urlId: s.urlId || null,
    requestMethod: s.requestMethod || 'GET',
    requestUrl: s.requestUrl || '',
    requestHeaders: str(s.requestHeaders, {}),
    requestQuery: str(s.requestQuery, {}),
    bodyType: s.bodyType || 'none',
    requestBody: s.requestBody || '',
    extractRules: str(s.extractRules, []),
    assertions: str(s.assertions, []),
    onFailure: s.onFailure || 'TERMINATE',
    delayMsNum: Number(s.delayMs || 0),
    delayMs: Number(s.delayMs || 0),
    enabled: 1,
    enabledBool: true
  }
}

function applyAiSteps(replace) {
  if (!aiSteps.value.length) return
  const mapped = aiSteps.value.map((s, i) => toEditorStep(s, i))
  if (replace) {
    steps.value = mapped
    curIndex.value = 0
  } else {
    steps.value = steps.value.concat(mapped)
    curIndex.value = steps.value.length - mapped.length
  }
  aiVisible.value = false
  ElMessage.success(`已${replace ? '替换' : '追加'} ${mapped.length} 个步骤，请检查后保存`)
}

onMounted(async () => {
  await load()
  try {
    envs.value = await envList(flow.projectId) || []
  } catch (e) {
    envs.value = []
  }
  // 展示最近一次运行结果
  try {
    const page = await flowRunPage({ flowId: Number(flowId), pageNum: 1, pageSize: 1 })
    if (page.list && page.list.length) runId.value = page.list[0].runId
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.editor-wrap { display: flex; flex-direction: column; height: calc(100vh - 110px); }
.top-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.saved-tip { color: #67c23a; font-size: 12px; }
.cols { flex: 1; display: flex; gap: 12px; min-height: 0; overflow-x: auto; }
.col-steps { width: 270px; background: var(--el-bg-color); border-radius: 8px; padding: 12px; display: flex; flex-direction: column; flex-shrink: 0; }
.col-config { flex: 1 0 460px; background: var(--el-bg-color); border-radius: 8px; padding: 12px 16px; min-width: 460px; overflow: auto; }
.col-run { width: 360px; background: var(--el-bg-color); border-radius: 8px; padding: 12px; display: flex; flex-direction: column; flex-shrink: 0; overflow: hidden; }
.col-title { display: flex; justify-content: space-between; align-items: center; font-weight: 600; margin-bottom: 10px; }
.step-list { flex: 1; overflow: auto; }
.step-item { border: 1px solid var(--el-border-color); border-radius: 6px; padding: 8px 10px; margin-bottom: 8px; cursor: pointer; position: relative; }
.step-item:hover { border-color: var(--el-color-primary); }
.step-item.active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.step-item.disabled { opacity: 0.5; }
.si-top { display: flex; align-items: center; gap: 6px; }
.si-idx { width: 18px; height: 18px; border-radius: 50%; background: #f0f2f5; color: #606266; font-size: 11px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.si-method { flex-shrink: 0; }
.si-name { flex: 1; font-size: 13px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.si-url { color: #909399; font-size: 12px; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.si-ops { display: none; position: absolute; right: 6px; top: 6px; background: var(--el-bg-color); border-radius: 4px; padding: 0 2px; }
.step-item:hover .si-ops { display: flex; }
.run-form { padding-bottom: 10px; border-bottom: 1px dashed var(--el-border-color-lighter); }
.run-ops { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.run-result { flex: 1; overflow: auto; margin-top: 10px; }
.empty-tip { color: #c0c4cc; font-size: 13px; padding: 10px 0; }
.form-tip { color: #c0c4cc; font-size: 12px; line-height: 1.4; }
.ai-step { display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-bottom: 1px dashed var(--el-border-color-lighter); font-size: 13px; }
.ai-step-name { font-weight: 500; }
.ai-step-url { color: #909399; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
