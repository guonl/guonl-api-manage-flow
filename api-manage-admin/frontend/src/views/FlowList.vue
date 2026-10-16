<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.projectId" placeholder="全部项目" clearable style="width:180px" @change="load">
        <el-option v-for="p in projects" :key="p.projectId" :label="p.projectName" :value="p.projectId" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜索流程名称" clearable style="width:220px" @keyup.enter="load" @clear="load" />
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      <div style="flex:1" />
      <el-button @click="openImport"><el-icon><Upload /></el-icon>导入</el-button>
      <el-button type="primary" plain @click="openCreate"><el-icon><Plus /></el-icon>新建流程</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="flowId" label="ID" width="60" />
      <el-table-column prop="flowName" label="流程名称" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/flow/editor/${row.flowId}`)">{{ row.flowName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="flowDesc" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="flowStatus" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.flowStatus === '1' ? 'success' : 'info'" size="small">{{ row.flowStatus === '1' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="330" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="$router.push(`/flow/editor/${row.flowId}`)">编辑</el-button>
          <el-button size="small" type="success" @click="openRun(row)">运行</el-button>
          <el-button size="small" @click="openHistory(row)">历史</el-button>
          <el-dropdown style="margin-left:10px; vertical-align:middle" @command="(cmd) => onMore(cmd, row)">
            <el-button size="small">更多<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="export">导出 JSON</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      :page-size="query.pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top:14px; justify-content:flex-end"
      @current-change="load"
    />

    <!-- 新建/编辑基本信息 -->
    <el-dialog v-model="editVisible" :title="form.flowId ? '编辑流程信息' : '新建流程'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属项目" required>
          <el-select v-model="form.projectId" placeholder="选择项目" style="width:100%">
            <el-option v-for="p in projects" :key="p.projectId" :label="p.projectName" :value="p.projectId" />
          </el-select>
        </el-form-item>
        <el-form-item label="流程名称" required><el-input v-model="form.flowName" placeholder="如：订单创建冒烟流程" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.flowDesc" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.flowStatus" active-value="1" inactive-value="0" active-text="启用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">{{ form.flowId ? '保存' : '创建并编辑' }}</el-button>
      </template>
    </el-dialog>

    <!-- 运行 -->
    <el-dialog v-model="runVisible" :title="`运行流程 - ${runRow?.flowName || ''}`" width="560px">
      <el-form label-width="90px">
        <el-form-item label="目标环境">
          <el-select v-model="runForm.envId" placeholder="默认取项目默认环境" clearable style="width:100%">
            <el-option v-for="e in runEnvs" :key="e.envId" :label="`${e.envName}（${e.baseUrl}）`" :value="e.envId" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始变量">
          <el-input v-model="runForm.initVarsText" type="textarea" :rows="5" placeholder='JSON 对象，如 {"orderId": 12345}，可留空' />
          <div class="form-tip">变量将以 <b v-pre>{{var}}</b> 形式在步骤的 URL / Header / Query / Body 中引用</div>
        </el-form-item>
        <el-form-item label="失败策略">
          <el-radio-group v-model="runForm.mode">
            <el-radio value="NORMAL">按步骤配置</el-radio>
            <el-radio value="DEBUG">调试模式（失败即停）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runVisible = false">取消</el-button>
        <el-button type="primary" :loading="running" @click="doRun">立即运行</el-button>
      </template>
    </el-dialog>

    <!-- 运行历史 -->
    <el-dialog v-model="histVisible" :title="`运行历史 - ${histRow?.flowName || ''}`" width="860px">
      <el-table :data="histList" v-loading="histLoading" size="small" border>
        <el-table-column prop="runId" label="ID" width="70" />
        <el-table-column prop="runStatus" label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="runTag(row.runStatus)" size="small">{{ row.runStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerType" label="触发" width="90" />
        <el-table-column prop="totalCostMs" label="耗时" width="90">
          <template #default="{ row }">{{ row.totalCostMs != null ? row.totalCostMs + 'ms' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="180" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="90" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="$router.push(`/flow/run/${row.runId}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="histPage.pageNum"
        :page-size="histPage.pageSize"
        :total="histTotal"
        layout="total, prev, pager, next"
        style="margin-top:12px; justify-content:flex-end"
        @current-change="loadHistory"
      />
    </el-dialog>

    <!-- 导入 -->
    <el-dialog v-model="importVisible" title="导入流程 JSON" width="640px">
      <el-input v-model="importText" type="textarea" :rows="14" placeholder="粘贴流程导出的 JSON 内容" />
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="doImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectList, envList, flowPage, flowCreate, flowDelete, flowImport, flowExport, flowRun, flowRunPage } from '../api'

const router = useRouter()
const projects = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ projectId: null, keyword: '', pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const data = await flowPage(query)
    list.value = data.list || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

/* ---------- 新建 / 基本信息 ---------- */
const editVisible = ref(false)
const form = reactive({})

function openCreate() {
  Object.assign(form, { flowId: null, projectId: query.projectId, flowName: '', flowDesc: '', flowStatus: '1' })
  editVisible.value = true
}

async function save() {
  if (!form.projectId) return ElMessage.warning('请选择所属项目')
  if (!form.flowName) return ElMessage.warning('请输入流程名称')
  const data = { ...form, steps: [] }
  const saved = form.flowId ? (await flowUpdate(data), form) : await flowCreate(data)
  editVisible.value = false
  ElMessage.success('已保存')
  if (!form.flowId && saved && saved.flowId) router.push(`/flow/editor/${saved.flowId}`)
  else load()
}

/* ---------- 运行 ---------- */
const runVisible = ref(false)
const running = ref(false)
const runRow = ref(null)
const runEnvs = ref([])
const runForm = reactive({ envId: null, initVarsText: '', mode: 'NORMAL' })

async function openRun(row) {
  runRow.value = row
  Object.assign(runForm, { envId: null, initVarsText: '', mode: 'NORMAL' })
  runVisible.value = true
  try {
    runEnvs.value = await envList(row.projectId)
  } catch (e) {
    runEnvs.value = []
  }
}

async function doRun() {
  let initVars = {}
  if (runForm.initVarsText && runForm.initVarsText.trim()) {
    try {
      initVars = JSON.parse(runForm.initVarsText)
    } catch (e) {
      return ElMessage.warning('初始变量不是合法 JSON')
    }
    if (typeof initVars !== 'object' || Array.isArray(initVars)) return ElMessage.warning('初始变量必须是 JSON 对象')
  }
  running.value = true
  try {
    const r = await flowRun({ flowId: runRow.value.flowId, envId: runForm.envId, initVars, mode: runForm.mode })
    runVisible.value = false
    ElMessage.success(`流程执行完成：${r.runStatus}（${r.totalCostMs}ms）`)
    router.push(`/flow/run/${r.runId}`)
  } finally {
    running.value = false
  }
}

/* ---------- 历史 ---------- */
const histVisible = ref(false)
const histLoading = ref(false)
const histRow = ref(null)
const histList = ref([])
const histTotal = ref(0)
const histPage = reactive({ pageNum: 1, pageSize: 10 })

function openHistory(row) {
  histRow.value = row
  histPage.pageNum = 1
  histVisible.value = true
  loadHistory()
}

async function loadHistory() {
  histLoading.value = true
  try {
    const data = await flowRunPage({ flowId: histRow.value.flowId, ...histPage })
    histList.value = data.list || []
    histTotal.value = Number(data.total || 0)
  } finally {
    histLoading.value = false
  }
}

/* ---------- 导入 / 导出 / 删除 ---------- */
const importVisible = ref(false)
const importText = ref('')

function openImport() {
  importText.value = ''
  importVisible.value = true
}

async function doImport() {
  let data
  try {
    data = JSON.parse(importText.value)
  } catch (e) {
    return ElMessage.warning('不是合法 JSON')
  }
  await flowImport(data)
  importVisible.value = false
  ElMessage.success('导入成功')
  load()
}

function runTag(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

async function onMore(cmd, row) {
  if (cmd === 'export') {
    const data = await flowExport(row.flowId)
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `flow-${row.flowId}-${row.flowName}.json`
    a.click()
    URL.revokeObjectURL(a.href)
  } else if (cmd === 'delete') {
    await ElMessageBox.confirm(`确认删除流程「${row.flowName}」？其步骤与历史记录将不可见`, '删除确认', { type: 'warning' })
    await flowDelete(row.flowId)
    ElMessage.success('已删除')
    load()
  }
}

onMounted(async () => {
  try {
    projects.value = await projectList()
  } catch (e) {
    projects.value = []
  }
  load()
})
</script>

<style scoped>
.page-card { background: var(--el-bg-color); border-radius: 8px; padding: 16px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
.form-tip { color: #c0c4cc; font-size: 12px; line-height: 1.4; }
</style>
