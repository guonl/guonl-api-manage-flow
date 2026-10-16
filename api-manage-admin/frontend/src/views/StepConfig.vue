<template>
  <div v-if="step" class="step-config">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="请求" name="req">
        <div class="row-line">
          <el-select v-model="step.stepType" style="width:120px" @change="onTypeChange">
            <el-option label="HTTP 请求" value="HTTP" />
            <el-option label="等待延时" value="DELAY" />
          </el-select>
          <el-input v-model="step.stepName" placeholder="步骤名称" style="flex:1" />
          <el-switch v-model="step.enabledBool" active-text="启用" />
        </div>

        <template v-if="step.stepType === 'HTTP'">
          <div class="row-line" style="margin-top:12px">
            <el-select v-model="step.urlId" filterable remote clearable placeholder="从接口库选择（可搜索）" style="flex:1"
              :remote-method="searchApis" :loading="apiSearching" @change="onApiPicked">
              <el-option v-for="a in apiOptions" :key="a.urlId" :value="a.urlId" :label="a.methodName">
                <span class="m-type" :class="(a.methodType || '').toLowerCase()">{{ a.methodType }}</span>
                {{ a.methodName }}
                <span class="m-url">{{ a.requestUrl }}</span>
              </el-option>
            </el-select>
          </div>
          <div class="row-line" style="margin-top:8px">
            <el-select v-model="step.requestMethod" style="width:110px">
              <el-option v-for="m in ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']" :key="m" :label="m" :value="m" />
            </el-select>
            <el-input v-model="step.requestUrl" placeholder="/path 或 {{baseUrl}}/path，支持 {{var}} 变量" style="flex:1" />
          </div>

          <div class="grid-head">
            <span>Query 参数（值支持 <b v-pre>{{var}}</b>）</span>
            <el-button size="small" link type="primary" @click="queryRows.push({ key: '', value: '' })">+ 添加</el-button>
          </div>
          <div v-for="(q, i) in queryRows" :key="'q' + i" class="param-row">
            <el-input v-model="q.key" placeholder="参数名" @input="syncParams" />
            <el-input v-model="q.value" placeholder="参数值" @input="syncParams" />
            <el-button size="small" type="danger" link @click="queryRows.splice(i, 1); syncParams()">删除</el-button>
          </div>

          <div class="grid-head">
            <span>Header</span>
            <el-button size="small" link type="primary" @click="headerRows.push({ key: '', value: '' })">+ 添加</el-button>
          </div>
          <div v-for="(h, i) in headerRows" :key="'h' + i" class="param-row">
            <el-input v-model="h.key" placeholder="Header名" @input="syncParams" />
            <el-input v-model="h.value" placeholder="值" @input="syncParams" />
            <el-button size="small" type="danger" link @click="headerRows.splice(i, 1); syncParams()">删除</el-button>
          </div>

          <div class="grid-head"><span>请求体</span></div>
          <el-radio-group v-model="step.bodyType" size="small">
            <el-radio-button value="none">无</el-radio-button>
            <el-radio-button value="json">JSON</el-radio-button>
            <el-radio-button value="form">FORM</el-radio-button>
            <el-radio-button value="text">TEXT</el-radio-button>
          </el-radio-group>
          <el-input v-if="step.bodyType !== 'none'" v-model="step.requestBody" type="textarea" :rows="6"
            :placeholder="step.bodyType === 'json' ? '{&quot;orderId&quot;: &quot;{{orderId}}&quot;}' : 'key1={{var1}}&key2={{var2}} 或原始文本'"
            style="margin-top:6px; font-family:monospace" />
        </template>

        <template v-else>
          <div class="row-line" style="margin-top:12px">
            <span class="lbl">等待时长（毫秒）</span>
            <el-input-number v-model="step.delayMsNum" :min="0" :max="60000" :step="500" @change="syncDelay" />
          </div>
          <div class="form-tip">用于流程中插入等待，例如等待异步任务完成；上限 60 秒</div>
        </template>
      </el-tab-pane>

      <el-tab-pane label="提取变量" name="extract">
        <div class="form-tip" style="margin-bottom:8px">
          从本步骤响应 JSON 中按路径提取值，写入流程上下文，供后续步骤引用。路径示例：<b>$.data.orderId</b>、<b>$.list[0].id</b>
        </div>
        <div class="grid-head">
          <span>提取规则</span>
          <el-button size="small" link type="primary" @click="extractRows.push({ varName: '', jsonPath: '', desc: '' })">+ 添加</el-button>
        </div>
        <div v-for="(e, i) in extractRows" :key="'e' + i" class="rule-row">
          <el-input v-model="e.varName" placeholder="变量名" @input="syncRules" />
          <el-input v-model="e.jsonPath" placeholder="$.data.id" @input="syncRules" />
          <el-input v-model="e.desc" placeholder="说明（可选）" @input="syncRules" />
          <el-button size="small" type="danger" link @click="extractRows.splice(i, 1); syncRules()">删除</el-button>
        </div>
        <div v-if="!extractRows.length" class="empty-tip">尚未配置提取规则</div>
      </el-tab-pane>

      <el-tab-pane label="断言" name="assert">
        <div class="form-tip" style="margin-bottom:8px">
          校验本步骤的响应内容；target 为 JSON 路径（如 <b>$.code</b>），类型 STATUS 时校验 HTTP 状态码、target 留空
        </div>
        <div class="grid-head">
          <span>断言规则</span>
          <el-button size="small" link type="primary" @click="assertRows.push({ type: 'EQUALS', target: '', expect: '', desc: '' })">+ 添加</el-button>
        </div>
        <div v-for="(a, i) in assertRows" :key="'a' + i" class="rule-row">
          <el-select v-model="a.type" style="width:130px" @change="syncRules">
            <el-option v-for="t in ['EQUALS', 'NOT_EQUALS', 'CONTAINS', 'NOT_CONTAINS', 'NOT_NULL', 'GT', 'LT', 'GTE', 'LTE', 'STATUS']" :key="t" :label="t" :value="t" />
          </el-select>
          <el-input v-model="a.target" placeholder="$.code" @input="syncRules" />
          <el-input v-model="a.expect" placeholder="期望值" @input="syncRules" />
          <el-input v-model="a.desc" placeholder="说明（可选）" @input="syncRules" />
          <el-button size="small" type="danger" link @click="assertRows.splice(i, 1); syncRules()">删除</el-button>
        </div>
        <div v-if="!assertRows.length" class="empty-tip">尚未配置断言</div>
      </el-tab-pane>
    </el-tabs>

    <div class="fail-line">
      <span class="lbl">本步失败时</span>
      <el-radio-group v-model="step.onFailure" size="small">
        <el-radio-button value="TERMINATE">终止流程</el-radio-button>
        <el-radio-button value="CONTINUE">继续执行</el-radio-button>
      </el-radio-group>
    </div>
  </div>
  <div v-else class="empty-tip" style="padding:60px 0; text-align:center">← 请选择或添加一个步骤</div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { apiSearch } from '../api'

const props = defineProps({ step: Object })
const activeTab = ref('req')
const queryRows = ref([])
const headerRows = ref([])
const extractRows = ref([])
const assertRows = ref([])
const apiOptions = ref([])
const apiSearching = ref(false)

const DEFAULT_HEADERS = '{"Content-Type":"application/json"}'

function parseObj(str) {
  if (!str) return []
  try {
    const o = JSON.parse(str)
    return Object.entries(o).map(([key, value]) => ({ key, value: value == null ? '' : String(value) }))
  } catch (e) {
    return []
  }
}
function parseArr(str) {
  if (!str) return []
  try {
    const a = JSON.parse(str)
    return Array.isArray(a) ? a.map((x) => ({ varName: x.varName || '', jsonPath: x.jsonPath || '', desc: x.desc || '', type: x.type || 'EQUALS', target: x.target || '', expect: x.expect == null ? '' : String(x.expect) })) : []
  } catch (e) {
    return []
  }
}

function hydrate(s) {
  activeTab.value = 'req'
  s.enabledBool = s.enabled === 1 || s.enabled === '1'
  s.delayMsNum = Number(s.delayMs || 0)
  queryRows.value = parseObj(s.requestQuery)
  headerRows.value = parseObj(s.requestHeaders)
  extractRows.value = parseArr(s.extractRules)
  assertRows.value = parseArr(s.assertions)
}

function syncParams() {
  if (!props.step) return
  props.step.requestQuery = objToJson(queryRows.value)
  props.step.requestHeaders = objToJson(headerRows.value)
}
function objToJson(rows) {
  const o = {}
  rows.filter((r) => r.key).forEach((r) => {
    o[r.key] = r.value
  })
  return JSON.stringify(o)
}
function syncRules() {
  if (!props.step) return
  props.step.extractRules = JSON.stringify(extractRows.value.map((e) => ({ varName: e.varName, jsonPath: e.jsonPath, desc: e.desc })))
  props.step.assertions = JSON.stringify(assertRows.value.map((a) => ({ type: a.type, target: a.target, expect: a.expect, desc: a.desc })))
}
function syncDelay() {
  if (props.step) props.step.delayMs = props.step.delayMsNum || 0
}
function onTypeChange() {
  if (props.step) props.step.delayMs = props.step.delayMsNum || 0
}

async function searchApis(keyword) {
  if (!props.step) return
  apiSearching.value = true
  try {
    apiOptions.value = await apiSearch({ projectId: props.step.projectId, keyword }) || []
  } catch (e) {
    apiOptions.value = []
  } finally {
    apiSearching.value = false
  }
}

function onApiPicked(urlId) {
  const api = apiOptions.value.find((a) => a.urlId === urlId)
  if (!api || !props.step) return
  props.step.requestMethod = api.methodType || 'GET'
  props.step.requestUrl = api.requestUrl
  if (api.methodContentType && ['json', 'form', 'none'].includes(api.methodContentType)) props.step.bodyType = api.methodContentType
  if (api.methodContentType === 'json' && !props.step.requestHeaders) props.step.requestHeaders = DEFAULT_HEADERS
}

watch(() => props.step, (s) => {
  if (s) hydrate(s)
}, { immediate: true })
</script>

<style scoped>
.step-config { height: 100%; display: flex; flex-direction: column; }
.row-line { display: flex; gap: 8px; align-items: center; }
.grid-head { display: flex; justify-content: space-between; align-items: center; margin: 14px 0 6px; font-size: 13px; color: #606266; }
.param-row { display: flex; gap: 8px; margin-bottom: 6px; }
.rule-row { display: flex; gap: 8px; margin-bottom: 6px; }
.rule-row .el-select { flex-shrink: 0; }
.rule-row .el-input { flex: 1 1 0; min-width: 0; }
.fail-line { margin-top: 12px; padding-top: 10px; border-top: 1px dashed var(--el-border-color-lighter); display: flex; align-items: center; gap: 12px; }
.lbl { color: #606266; font-size: 13px; }
.form-tip { color: #c0c4cc; font-size: 12px; line-height: 1.5; }
.empty-tip { color: #c0c4cc; font-size: 13px; padding: 10px 0; }
.m-type { display: inline-block; width: 48px; font-size: 11px; font-weight: 700; }
.m-type.get { color: #67c23a; }
.m-type.post { color: #e6a23c; }
.m-type.put { color: var(--el-color-primary); }
.m-type.delete { color: #f56c6c; }
.m-url { color: #c0c4cc; font-size: 12px; margin-left: 6px; }
</style>
