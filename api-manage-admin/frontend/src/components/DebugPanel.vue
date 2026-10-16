<template>
  <div class="debug-panel">
    <!-- 环境与请求栏 -->
    <div class="req-bar">
      <el-select v-model="envId" size="small" style="width:150px" placeholder="环境">
        <el-option v-for="e in envs" :key="e.envId" :label="e.envName" :value="e.envId">
          <span>{{ e.envName }}</span>
          <span class="env-url">{{ e.baseUrl }}</span>
        </el-option>
      </el-select>
      <el-select v-model="form.method" size="small" class="method-select">
        <el-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :label="m" :value="m" />
      </el-select>
      <el-input v-model="form.url" size="small" placeholder="/path 或完整 URL" @keyup.enter="send" />
      <el-button size="small" type="primary" :loading="sending" @click="send">发送</el-button>
      <el-button size="small" @click="historyVisible = true">历史</el-button>
      <el-button size="small" text title="清除本地草稿，恢复为接口定义默认值" @click="resetTemplate">重置</el-button>
    </div>

    <!-- 请求配置 -->
    <el-tabs v-model="reqTab" class="req-tabs">
      <el-tab-pane label="Params" name="params">
        <kv-table v-model="form.query" />
      </el-tab-pane>
      <el-tab-pane label="Headers" name="headers">
        <kv-table v-model="form.headers" />
      </el-tab-pane>
      <el-tab-pane name="body">
        <template #label>Body<span v-if="form.bodyType !== 'none'" class="body-badge">{{ form.bodyType }}</span></template>
        <div class="body-toolbar">
          <el-radio-group v-model="form.bodyType" size="small">
            <el-radio-button value="none">none</el-radio-button>
            <el-radio-button value="json">json</el-radio-button>
            <el-radio-button value="x-www-form-urlencoded">x-www</el-radio-button>
            <el-radio-button value="form-data">form-data</el-radio-button>
          </el-radio-group>
          <div>
            <el-button v-if="form.bodyType === 'json'" size="small" text type="primary" @click="formatBody">格式化</el-button>
            <el-button v-if="form.bodyType === 'json'" size="small" text type="primary" :loading="genLoading" @click="genTestData">
              <el-icon><MagicStick /></el-icon>AI 生成数据
            </el-button>
          </div>
        </div>
        <el-input v-if="form.bodyType !== 'none'" v-model="form.body" type="textarea" :rows="8"
                  class="body-input" placeholder='{"key": "value"}' />
      </el-tab-pane>
    </el-tabs>

    <!-- 响应区 -->
    <div v-if="resp" class="resp-area">
      <div class="resp-head">
        <span class="status-badge" :class="statusClass">{{ resp.status || '异常' }}</span>
        <span class="resp-meta">{{ resp.costMs }}ms</span>
        <span class="resp-meta">{{ fmtSize(resp.size) }}</span>
        <el-tag v-if="!resp.success" type="danger" size="small">{{ resp.errorMsg || '请求失败' }}</el-tag>
        <div style="flex:1" />
        <el-button size="small" text type="primary" :loading="analyzing" @click="analyzeResp">
          <el-icon><MagicStick /></el-icon>AI 分析
        </el-button>
        <el-button size="small" text @click="saveAsDoc">存为文档示例</el-button>
      </div>
      <el-tabs v-model="respTab">
        <el-tab-pane label="Pretty" name="pretty">
          <pre class="json-view">{{ prettyBody }}</pre>
        </el-tab-pane>
        <el-tab-pane label="Raw" name="raw">
          <pre class="json-view">{{ resp.body || '(空)' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="Headers" name="headers">
          <el-table :data="respHeaderRows" size="small" border>
            <el-table-column prop="key" label="Header" width="220" />
            <el-table-column prop="value" label="Value" min-width="300" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
    <el-empty v-else description="点击「发送」查看响应" :image-size="70" />

    <!-- 调用历史 -->
    <el-dialog v-model="historyVisible" title="调用历史" width="760px">
      <el-table :data="records" size="small" border max-height="420" @row-click="loadRecord">
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column prop="requestMethod" label="方法" width="80">
          <template #default="{ row }"><span class="method-tag" :class="`m-${row.requestMethod?.toLowerCase()}`">{{ row.requestMethod }}</span></template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="URL" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">{{ row.responseStatus || 'ERR' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时" width="80"><template #default="{ row }">{{ row.costMs }}ms</template></el-table-column>
        <el-table-column prop="operator" label="操作人" width="90" />
      </el-table>
      <el-pagination v-model:current-page="page.pageNum" :page-size="page.pageSize" :total="page.total"
                     layout="total, prev, pager, next" size="small" style="margin-top:10px" @current-change="loadHistory" />
    </el-dialog>

    <!-- AI 分析结果 -->
    <el-dialog v-model="aiVisible" title="AI 响应分析" width="680px">
      <div v-loading="analyzing" style="min-height:80px">
        <div v-if="aiResult" class="md" v-html="renderMd(aiResult)"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { debugSend, debugTemplate, debugRecords, apiSaveDoc, envList, aiAnalyze, aiGenData } from '../api'
import { renderMd } from '../utils/md'
import KvTable from './KvTable.vue'

const props = defineProps({ urlId: { type: Number, required: true }, projectId: { type: Number, required: true } })

const envs = ref([])
const envId = ref(null)
const sending = ref(false)
const reqTab = ref('params')
const respTab = ref('pretty')
const resp = ref(null)
const historyVisible = ref(false)
const records = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const form = reactive({ method: 'GET', url: '', headers: [], query: [], bodyType: 'none', body: '' })

// 调试草稿本地持久化：手动调整的 params/headers/body 编辑即存，切换接口或刷新后自动恢复
const restoring = ref(false)
watch(form, () => {
  if (restoring.value) return
  const { method, url, headers, query, bodyType, body } = form
  localStorage.setItem(`debug_form_${props.urlId}`, JSON.stringify({ method, url, headers, query, bodyType, body }))
}, { deep: true })

onMounted(async () => {
  envs.value = await envList(props.projectId)
  const saved = localStorage.getItem(`debug_env_${props.projectId}`)
  envId.value = envs.value.find(e => e.envId === Number(saved))?.envId
        || envs.value.find(e => e.isDefault === 1)?.envId
        || envs.value[0]?.envId
  loadTemplate()
})

watch(() => props.urlId, () => { resp.value = null; loadTemplate() })
watch(envId, v => v && localStorage.setItem(`debug_env_${props.projectId}`, v))

async function loadTemplate() {
  restoring.value = true
  try {
    // 优先恢复本地草稿（最近一次手动编辑，含未发送的修改）
    const draft = localStorage.getItem(`debug_form_${props.urlId}`)
    if (draft && restoreDraft(draft)) return
    const d = await debugTemplate(props.urlId)
    const api = d.api
    const last = d.lastRecord
    if (last) {
      form.method = last.requestMethod
      form.url = last.requestUrl
      form.bodyType = last.bodyType || 'none'
      form.body = last.requestBody || ''
      form.headers = parseKv(last.requestHeaders)
      form.query = parseKv(last.requestQuery)
      stripBase(form)
    } else {
      form.method = api.methodType || 'GET'
      form.url = api.requestUrl || ''
      form.bodyType = mapBodyType(api.methodContentType)
      form.body = form.bodyType === 'json' ? skeletonJson(api.requestParamMap) : ''
      form.headers = []
      form.query = form.bodyType === 'none' ? skeletonKv(api.requestParamMap) : []
    }
  } finally {
    restoring.value = false
  }
}

/** 恢复本地草稿，格式非法时返回 false 走接口模板 */
function restoreDraft(raw) {
  try {
    const s = JSON.parse(raw)
    if (!s || typeof s !== 'object') return false
    form.method = s.method || 'GET'
    form.url = s.url || ''
    form.bodyType = s.bodyType || 'none'
    form.body = s.body || ''
    const rows = arr => Array.isArray(arr) ? arr.filter(r => r && r.key).map(r => ({ key: r.key, value: String(r.value ?? ''), enabled: r.enabled !== false })) : []
    form.headers = rows(s.headers)
    form.query = rows(s.query)
    return true
  } catch (e) {
    return false
  }
}

/** 清除草稿并恢复为接口定义默认值 */
async function resetTemplate() {
  localStorage.removeItem(`debug_form_${props.urlId}`)
  await loadTemplate()
  ElMessage.success('已恢复为接口默认值')
}

/** 绝对 URL 去掉环境前缀便于编辑 */
function stripBase(form) {
  const env = envs.value.find(e => e.envId === envId.value)
  if (env && form.url.startsWith(env.baseUrl)) {
    form.url = form.url.slice(env.baseUrl.length) || '/'
  } else if (env && form.url.startsWith(env.baseUrl.replaceAll('/+$', ''))) {
    form.url = form.url.slice(env.baseUrl.replaceAll('/+$', '').length) || '/'
  }
}

function parseKv(json) {
  try {
    const obj = JSON.parse(json || 'null')
    if (!obj) return []
    // 后端记录为数组 [{key,value,enabled}]，兼容旧对象格式 {key:value}
    if (Array.isArray(obj)) {
      return obj.filter(r => r && r.key).map(r => ({ key: r.key, value: String(r.value ?? ''), enabled: r.enabled !== false }))
    }
    return Object.entries(obj).map(([key, value]) => ({ key, value: String(value ?? ''), enabled: true }))
  } catch (e) { return [] }
}

function skeletonKv(paramMap) {
  try {
    const map = JSON.parse(paramMap || 'null')
    return map ? Object.keys(map).map(k => ({ key: k, value: '' })) : []
  } catch (e) { return [] }
}

function skeletonJson(paramMap) {
  const rows = skeletonKv(paramMap)
  if (!rows.length) return ''
  return JSON.stringify(Object.fromEntries(rows.map(r => [r.key, ''])), null, 2)
}

function mapBodyType(ct) {
  if (!ct || ct === 'none') return 'none'
  if (ct.includes('json')) return 'json'
  if (ct.includes('x-www-form-urlencoded')) return 'x-www-form-urlencoded'
  if (ct.includes('multipart')) return 'form-data'
  return 'none'
}

function formatBody() {
  try {
    form.body = JSON.stringify(JSON.parse(form.body), null, 2)
  } catch (e) {
    ElMessage.warning('JSON 格式有误：' + e.message)
  }
}

async function send() {
  if (!envId.value) return ElMessage.warning('请先选择环境')
  if (!form.url) return ElMessage.warning('请填写请求 URL')
  sending.value = true
  try {
    resp.value = await debugSend({
      projectId: props.projectId, envId: envId.value, urlId: props.urlId,
      method: form.method, url: form.url,
      headers: form.headers.filter(r => r.key && r.enabled !== false),
      query: form.query.filter(r => r.key && r.enabled !== false),
      bodyType: form.bodyType, body: form.body
    })
    respTab.value = 'pretty'
  } finally {
    sending.value = false
  }
}

function saveAsDoc() {
  if (!resp.value?.body) return
  ElMessage.success('已存入文档示例（成功示例）')
  apiSaveDoc(props.urlId, { successRespExample: resp.value.body })
}

/* ---------- AI 集成 ---------- */
const genLoading = ref(false)
const analyzing = ref(false)
const aiVisible = ref(false)
const aiResult = ref(null)

/** AI 生成测试数据并回填 Body */
async function genTestData() {
  genLoading.value = true
  try {
    const r = await aiGenData(props.urlId)
    if (r.dataJson) {
      form.bodyType = 'json'
      try { form.body = JSON.stringify(JSON.parse(r.dataJson), null, 2) } catch (e) { form.body = r.dataJson }
      ElMessage.success('已生成测试数据')
    } else {
      ElMessage.warning('未生成有效数据')
    }
  } finally {
    genLoading.value = false
  }
}

/** AI 分析当前响应 */
async function analyzeResp() {
  if (!resp.value) return
  analyzing.value = true
  aiVisible.value = true
  aiResult.value = null
  try {
    const request = JSON.stringify({
      method: form.method, url: form.url, bodyType: form.bodyType,
      query: form.query.filter(r => r.key && r.enabled !== false),
      headers: form.headers.filter(r => r.key && r.enabled !== false),
      body: form.body || undefined
    }, null, 2)
    const response = JSON.stringify({
      status: resp.value.status, success: resp.value.success,
      errorMsg: resp.value.errorMsg, body: resp.value.body
    }, null, 2)
    const r = await aiAnalyze({ request, response })
    aiResult.value = r.content || '(空)'
  } catch (e) {
    aiVisible.value = false
  } finally {
    analyzing.value = false
  }
}

const prettyBody = computed(() => {
  if (resp.value?.body == null) return '(空)'
  try { return JSON.stringify(JSON.parse(resp.value.body), null, 2) } catch (e) { return resp.value.body }
})

const statusClass = computed(() => {
  const s = resp.value?.status || 0
  if (s === 0) return 'st-err'
  if (s < 300) return 'st-ok'
  if (s < 400) return 'st-warn'
  return 'st-fail'
})

const respHeaderRows = computed(() => Object.entries(resp.value?.headers || {}).map(([key, value]) => ({ key, value })))

function fmtSize(n) {
  if (n == null) return '-'
  return n < 1024 ? n + ' B' : (n / 1024).toFixed(1) + ' KB'
}

async function loadHistory() {
  const d = await debugRecords({ urlId: props.urlId, pageNum: page.pageNum, pageSize: page.pageSize })
  records.value = d.list
  page.total = d.total
}

watch(historyVisible, v => {
  if (v) { page.pageNum = 1; loadHistory() }
})

/** 点击历史行回填请求与响应 */
function loadRecord(row) {
  form.method = row.requestMethod
  form.url = row.requestUrl
  form.bodyType = row.bodyType || 'none'
  form.body = row.requestBody || ''
  form.headers = parseKv(row.requestHeaders)
  form.query = parseKv(row.requestQuery)
  stripBase(form)
  resp.value = {
    status: row.responseStatus, costMs: row.costMs, size: (row.responseBody || '').length,
    headers: safeParse(row.responseHeaders), body: row.responseBody,
    success: row.success === 1, errorMsg: row.errorMsg
  }
  historyVisible.value = false
  respTab.value = 'pretty'
}

function safeParse(str) {
  try { return JSON.parse(str || '{}') } catch (e) { return {} }
}
</script>

<style scoped>
.debug-panel { display: flex; flex-direction: column; }
.req-bar { display: flex; gap: 8px; }
.method-select { width: 110px; }
.env-url { float: right; color: #909399; font-size: 12px; margin-left: 12px; }
.req-tabs { margin-top: 10px; }
.body-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.body-badge { margin-left: 4px; font-size: 10px; color: var(--el-color-primary); }
.body-input { font-family: Menlo, Consolas, monospace; }
.resp-area { margin-top: 10px; border-top: 1px solid var(--el-border-color-lighter); padding-top: 10px; }
.resp-head { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.status-badge {
  font-weight: 700; font-size: 14px; padding: 2px 10px; border-radius: 4px; color: #fff;
}
.st-ok { background: #67c23a; }
.st-warn { background: #e6a23c; }
.st-fail { background: #f56c6c; }
.st-err { background: #909399; }
.resp-meta { color: #909399; font-size: 12px; }
.json-view { background: #f6f8fa; border-radius: 6px; padding: 12px; font-size: 12px; overflow: auto; max-height: 360px; margin: 0; }
.md :deep(.code-block) { background: #282c34; color: #abb2bf; padding: 10px 12px; border-radius: 6px; overflow-x: auto; font-size: 13px; margin: 6px 0; }
.md :deep(.inline-code) { background: #e8eaed; color: #c7254e; padding: 1px 5px; border-radius: 3px; font-size: 13px; }
.md :deep(.md-h) { font-weight: 700; margin: 6px 0 2px; }
.md :deep(.md-li) { padding-left: 8px; }
:deep(.el-table) { cursor: pointer; }
</style>
