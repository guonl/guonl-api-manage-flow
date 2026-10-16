<template>
  <div class="api-page">
    <!-- 左侧树 -->
    <div class="tree-panel">
      <div class="tree-toolbar">
        <el-input v-model="keyword" size="small" placeholder="搜索接口" clearable @input="onSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button size="small" type="primary" @click="openGroupEdit()"><el-icon><Plus /></el-icon>分组</el-button>
      </div>
      <el-tree
        ref="treeRef"
        :data="treeData"
        node-key="id"
        :props="{ label: 'label', children: 'children' }"
        :expand-on-click-node="false"
        highlight-current
        @node-click="onNodeClick"
        style="flex:1; overflow:auto"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <template v-if="data.type === 'group'">
              <el-icon color="#e6a23c"><FolderOpened /></el-icon>
              <span class="node-label" :title="data.name">{{ data.label }}</span>
              <span class="node-count">{{ data.children?.length || 0 }}</span>
              <span class="node-actions" @click.stop>
                <el-dropdown trigger="click" @command="cmd => onGroupCmd(cmd, data)">
                  <el-icon class="act"><MoreFilled /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="add">新增接口</el-dropdown-item>
                      <el-dropdown-item command="edit">编辑分组</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除分组</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </span>
            </template>
            <template v-else>
              <span class="method-tag" :class="`m-${data.method?.toLowerCase()}`">{{ data.method }}</span>
              <span class="node-label" :title="`${data.label} ${data.url}`">{{ data.label }}</span>
              <el-tag v-if="data.status === 2" type="info" size="small">已下线</el-tag>
              <el-tag v-else-if="data.status === 0" type="warning" size="small">禁用</el-tag>
            </template>
          </span>
        </template>
      </el-tree>
      <div class="tree-foot">
        <el-button size="small" text type="primary" @click="loadTree"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
    </div>

    <!-- 右侧详情 -->
    <div class="detail-panel">
      <el-empty v-if="!current" description="选择左侧接口查看详情" />
      <template v-else>
        <div class="detail-head">
          <span class="method-tag method-big" :class="`m-${current.methodType?.toLowerCase()}`">{{ current.methodType }}</span>
          <span class="detail-url">{{ current.requestUrl }}</span>
          <div style="flex:1" />
          <el-tag v-if="current.sourceType === 1" size="small">同步</el-tag>
          <el-tag v-else type="warning" size="small">手工</el-tag>
          <el-tag :type="statusType" size="small">{{ statusText }}</el-tag>
          <el-dropdown @command="cmd => onApiCmd(cmd)">
            <el-button size="small"><el-icon><MoreFilled /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑接口</el-dropdown-item>
                <el-dropdown-item command="enable" v-if="current.urlStatus !== 1">启用</el-dropdown-item>
                <el-dropdown-item command="disable" v-else>禁用</el-dropdown-item>
                <el-dropdown-item command="offline" v-if="current.sourceType !== 1">下线</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <el-descriptions :column="2" border size="small" style="margin-top:14px">
          <el-descriptions-item label="方法名">{{ current.methodName }}</el-descriptions-item>
          <el-descriptions-item label="Content-Type">{{ current.methodContentType }}</el-descriptions-item>
          <el-descriptions-item label="返回类型">{{ current.returnParamClass || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标签">{{ current.urlLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ current.methodDesc || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="activeTab" style="margin-top:8px">
          <el-tab-pane label="请求参数" name="params">
            <el-table :data="paramRows" size="small" border v-if="paramRows.length">
              <el-table-column prop="name" label="参数名" width="180" />
              <el-table-column prop="type" label="类型" width="180" />
              <el-table-column prop="desc" label="说明" min-width="200" />
            </el-table>
            <el-empty v-else description="无请求参数" :image-size="60" />
          </el-tab-pane>
          <el-tab-pane label="返回结构" name="return">
            <pre v-if="current.returnParamMap" class="json-view">{{ pretty(current.returnParamMap) }}</pre>
            <el-empty v-else description="无返回结构" :image-size="60" />
          </el-tab-pane>
          <el-tab-pane label="接口文档" name="doc">
            <el-form label-width="90px" size="small">
              <el-form-item label="请求头"><el-input v-model="docForm.requestHeaders" type="textarea" :rows="3" placeholder='{"Authorization":"Bearer xxx"}' /></el-form-item>
              <el-form-item label="Query示例"><el-input v-model="docForm.queryParams" type="textarea" :rows="3" /></el-form-item>
              <el-form-item label="成功示例"><el-input v-model="docForm.successRespExample" type="textarea" :rows="5" /></el-form-item>
              <el-form-item label="失败示例"><el-input v-model="docForm.failRespExample" type="textarea" :rows="3" /></el-form-item>
              <el-form-item label="备注"><el-input v-model="docForm.remark" type="textarea" :rows="2" /></el-form-item>
              <el-form-item>
                <el-button type="primary" size="small" @click="saveDoc">保存文档</el-button>
                <el-button size="small" type="primary" plain :loading="genDocLoading" @click="genDoc">
                  <el-icon><MagicStick /></el-icon>AI 生成文档
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="接口调试" name="debug" lazy>
            <DebugPanel :url-id="current.urlId" :project-id="projectId" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>

    <!-- 分组编辑 -->
    <el-dialog v-model="groupVisible" :title="groupForm.groupId ? '编辑分组' : '新增分组'" width="500px">
      <el-form :model="groupForm" label-width="110px">
        <el-form-item label="controller全名" required><el-input v-model="groupForm.controllerName" placeholder="com.xxx.XxxController" /></el-form-item>
        <el-form-item label="分组显示名" required><el-input v-model="groupForm.controllerShortName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="groupForm.controllerDesc" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="groupForm.groupSort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGroup">保存</el-button>
      </template>
    </el-dialog>

    <!-- 接口编辑 -->
    <el-dialog v-model="apiVisible" :title="apiForm.urlId ? '编辑接口' : '新增接口'" width="620px">
      <el-form :model="apiForm" label-width="100px">
        <el-form-item label="所属分组">
          <el-select v-model="apiForm.groupId" style="width:100%">
            <el-option v-for="g in groups" :key="g.groupId" :label="g.controllerShortName" :value="g.groupId" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求方法" required>
          <el-select v-model="apiForm.methodType" style="width:120px">
            <el-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求路径" required><el-input v-model="apiForm.requestUrl" placeholder="/api/xxx/{id}" /></el-form-item>
        <el-form-item label="方法名"><el-input v-model="apiForm.methodName" placeholder="留空自动生成" /></el-form-item>
        <el-form-item label="Content-Type">
          <el-select v-model="apiForm.methodContentType" style="width:220px">
            <el-option v-for="c in ['none','application/json','application/x-www-form-urlencoded','multipart/form-data']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签"><el-input v-model="apiForm.urlLabel" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="apiForm.methodDesc" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="apiVisible = false">取消</el-button>
        <el-button type="primary" @click="saveApi">保存</el-button>
      </template>
    </el-dialog>

    <!-- AI 生成文档 -->
    <el-dialog v-model="docAiVisible" title="AI 生成接口文档" width="720px">
      <div v-loading="genDocLoading" style="min-height:100px">
        <div v-if="docAiContent" class="md" v-html="renderMd(docAiContent)"></div>
      </div>
      <template #footer>
        <el-button @click="copyDoc">复制 Markdown</el-button>
        <el-button type="primary" @click="docAiVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  apiTree, apiSearch, apiDetail, apiCreate, apiUpdate, apiUpdateStatus, apiDelete, apiSaveDoc,
  groupList, groupCreate, groupUpdate, groupDelete, aiGenDoc
} from '../api'
import { renderMd } from '../utils/md'
import DebugPanel from '../components/DebugPanel.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.projectId))

const treeData = ref([])
const groups = ref([])
const keyword = ref('')
const treeRef = ref()

async function loadTree() {
  const [tree, gl] = await Promise.all([apiTree(projectId.value), groupList(projectId.value)])
  treeData.value = tree
  groups.value = gl
}

let searchTimer = null
function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (!keyword.value) return loadTree()
    const hits = await apiSearch({ projectId: projectId.value, keyword: keyword.value })
    // 关键字过滤：扁平命中结果包装成临时树
    treeData.value = [{
      id: 'search', label: `搜索结果（${hits.length}）`, type: 'group', name: '', children:
        hits.map(u => ({ id: 'u' + u.urlId, urlId: u.urlId, label: u.methodName, url: u.requestUrl, method: u.methodType, status: u.urlStatus, sourceType: u.sourceType, contentType: u.methodContentType, type: 'api' }))
    }]
  }, 300)
}

/* ---------- 详情 ---------- */
const current = ref(null)
const activeTab = ref('params')
const docForm = reactive({})

function onNodeClick(data) {
  if (data.type !== 'api') return
  loadDetail(data.urlId)
}

async function loadDetail(urlId) {
  const d = await apiDetail(urlId)
  current.value = d.api
  Object.assign(docForm, d.doc || {})
}

const paramRows = computed(() => {
  if (!current.value?.requestParamMap) return []
  try {
    const map = JSON.parse(current.value.requestParamMap)
    return Object.entries(map).map(([name, v]) => ({
      name,
      type: typeof v === 'object' ? (v?.type || 'object') : String(v),
      desc: typeof v === 'object' ? (v?.desc || '') : ''
    }))
  } catch (e) {
    return []
  }
})

const statusText = computed(() => ({ 1: '启用', 0: '禁用', 2: '已下线' })[current.value?.urlStatus] || '-')
const statusType = computed(() => ({ 1: 'success', 0: 'warning', 2: 'info' })[current.value?.urlStatus] || 'info')

function pretty(str) {
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch (e) { return str }
}

async function saveDoc() {
  await apiSaveDoc(current.value.urlId, { ...docForm })
  ElMessage.success('文档已保存')
}

/* ---------- AI 生成文档 ---------- */
const genDocLoading = ref(false)
const docAiVisible = ref(false)
const docAiContent = ref('')

async function genDoc() {
  genDocLoading.value = true
  docAiVisible.value = true
  docAiContent.value = ''
  try {
    const r = await aiGenDoc(current.value.urlId)
    docAiContent.value = r.content || '(空)'
  } catch (e) {
    docAiVisible.value = false
  } finally {
    genDocLoading.value = false
  }
}

function copyDoc() {
  navigator.clipboard.writeText(docAiContent.value)
  ElMessage.success('已复制到剪贴板')
}

/* ---------- 分组 ---------- */
const groupVisible = ref(false)
const groupForm = reactive({})

function openGroupEdit(row) {
  Object.assign(groupForm, row || { groupId: null, projectId: projectId.value, controllerName: '', controllerShortName: '', controllerDesc: '', groupSort: 0 })
  groupVisible.value = true
}

async function onGroupCmd(cmd, data) {
  const gid = data.groupId
  const g = groups.value.find(x => x.groupId === gid)
  if (cmd === 'add') openApiEdit(null, gid)
  else if (cmd === 'edit') openGroupEdit(g)
  else if (cmd === 'delete') {
    await ElMessageBox.confirm(`确认删除分组「${data.label}」？`, '删除确认', { type: 'warning' })
    await groupDelete(gid)
    ElMessage.success('已删除')
    loadTree()
  }
}

async function saveGroup() {
  if (!groupForm.controllerName || !groupForm.controllerShortName) return ElMessage.warning('请填写分组名')
  if (groupForm.groupId) await groupUpdate(groupForm)
  else await groupCreate(groupForm)
  groupVisible.value = false
  ElMessage.success('已保存')
  loadTree()
}

/* ---------- 接口 ---------- */
const apiVisible = ref(false)
const apiForm = reactive({})

function openApiEdit(row, groupId) {
  Object.assign(apiForm, row || {
    urlId: null, groupId: groupId || groups.value[0]?.groupId, methodType: 'GET',
    requestUrl: '', methodName: '', methodContentType: 'none', urlLabel: '', methodDesc: ''
  })
  apiVisible.value = true
}

function onApiCmd(cmd) {
  const u = current.value
  if (cmd === 'edit') openApiEdit(u)
  else if (cmd === 'enable') setStatus(u, 1)
  else if (cmd === 'disable') setStatus(u, 0)
  else if (cmd === 'offline') setStatus(u, 2)
  else if (cmd === 'delete') {
    ElMessageBox.confirm(`确认删除接口「${u.methodName}」？`, '删除确认', { type: 'warning' })
      .then(async () => {
        await apiDelete(u.urlId)
        current.value = null
        ElMessage.success('已删除')
        loadTree()
      })
  }
}

async function setStatus(u, status) {
  await apiUpdateStatus(u.urlId, status)
  ElMessage.success('状态已更新')
  loadDetail(u.urlId)
  loadTree()
}

async function saveApi() {
  if (!apiForm.groupId || !apiForm.requestUrl || !apiForm.methodType) return ElMessage.warning('分组、方法、路径必填')
  if (apiForm.urlId) await apiUpdate(apiForm)
  else await apiCreate(apiForm)
  apiVisible.value = false
  ElMessage.success('已保存')
  loadTree()
}

onMounted(loadTree)
</script>

<style scoped>
.api-page { display: flex; gap: 14px; height: calc(100vh - 120px); }
.tree-panel {
  width: 320px; background: var(--el-bg-color); border-radius: 8px; padding: 10px;
  display: flex; flex-direction: column;
}
.tree-toolbar { display: flex; gap: 8px; margin-bottom: 8px; }
.tree-foot { border-top: 1px solid var(--el-border-color-lighter); padding-top: 4px; }
.tree-node { display: flex; align-items: center; gap: 6px; flex: 1; overflow: hidden; }
.node-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.node-count { color: #c0c4cc; font-size: 12px; }
.node-actions { margin-left: auto; visibility: hidden; }
.tree-node:hover .node-actions { visibility: visible; }
.act { cursor: pointer; color: #909399; }
.detail-panel { flex: 1; background: var(--el-bg-color); border-radius: 8px; padding: 16px; overflow: auto; }
.detail-head { display: flex; align-items: center; gap: 10px; }
.method-big { font-size: 13px; padding: 3px 8px; border-radius: 4px; }
.detail-url { font-family: Menlo, Consolas, monospace; font-size: 16px; font-weight: 600; }
.json-view { background: #f6f8fa; border-radius: 6px; padding: 12px; font-size: 12px; overflow: auto; max-height: 400px; margin: 0; }
.md :deep(.code-block) { background: #282c34; color: #abb2bf; padding: 10px 12px; border-radius: 6px; overflow-x: auto; font-size: 13px; margin: 6px 0; }
.md :deep(.inline-code) { background: #e8eaed; color: #c7254e; padding: 1px 5px; border-radius: 3px; font-size: 13px; }
.md :deep(.md-h) { font-weight: 700; margin: 6px 0 2px; }
.md :deep(.md-li) { padding-left: 8px; }
</style>
