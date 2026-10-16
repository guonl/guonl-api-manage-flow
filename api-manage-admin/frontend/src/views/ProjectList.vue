<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索项目名 / 系统 / 负责人" clearable style="width:240px" @keyup.enter="load" @clear="load" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width:110px" @change="load">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      <div style="flex:1" />
      <el-button type="primary" plain @click="openCreate"><el-icon><Plus /></el-icon>新建项目</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="projectId" label="ID" width="60" />
      <el-table-column prop="projectName" label="项目名称" min-width="160">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/apis/${row.projectId}`)">{{ row.projectName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="systemName" label="所属系统" min-width="120" />
      <el-table-column prop="owner" label="负责人" width="100" />
      <el-table-column prop="manageUrl" label="actuator地址" min-width="220" show-overflow-tooltip />
      <el-table-column prop="projectStatus" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.projectStatus === 1 ? 'success' : 'info'" size="small">{{ row.projectStatus === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="onSync(row)" :loading="row._syncing">同步接口</el-button>
          <el-button size="small" @click="openEnv(row)">环境</el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
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

    <!-- 新建/编辑项目 -->
    <el-dialog v-model="editVisible" :title="form.projectId ? '编辑项目' : '新建项目'" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="项目名称" required><el-input v-model="form.projectName" /></el-form-item>
        <el-form-item label="所属系统"><el-input v-model="form.systemName" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.owner" /></el-form-item>
        <el-form-item label="actuator地址">
          <el-input v-model="form.manageUrl" placeholder="http://host:port/actuator" />
          <div class="form-tip">目标 Spring Boot 应用的 actuator 基地址，用于一键同步接口</div>
        </el-form-item>
        <el-form-item label="项目描述"><el-input v-model="form.projectDesc" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.projectStatus" :active-value="1" :inactive-value="0" active-text="启用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 环境管理 -->
    <el-dialog v-model="envVisible" :title="`环境管理 - ${envProject?.projectName || ''}`" width="720px">
      <div style="margin-bottom:10px">
        <el-button type="primary" size="small" @click="openEnvEdit()"><el-icon><Plus /></el-icon>新增环境</el-button>
      </div>
      <el-table :data="envs" size="small" border>
        <el-table-column prop="envName" label="环境名" width="100" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="isDefault" label="默认" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="envDesc" label="描述" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="openEnvEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteEnv(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="envEditVisible" :title="envForm.envId ? '编辑环境' : '新增环境'" width="480px" append-to-body>
        <el-form :model="envForm" label-width="90px">
          <el-form-item label="环境名" required><el-input v-model="envForm.envName" placeholder="dev/test/pre/prod" /></el-form-item>
          <el-form-item label="Base URL" required><el-input v-model="envForm.baseUrl" placeholder="http://host:port" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="envForm.envDesc" /></el-form-item>
          <el-form-item label="默认环境"><el-switch v-model="envForm.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="envEditVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEnv">保存</el-button>
        </template>
      </el-dialog>
    </el-dialog>

    <!-- 同步历史 -->
    <el-dialog v-model="syncLogVisible" title="同步历史（最近20次）" width="760px">
      <el-table :data="syncLogs" size="small" border>
        <el-table-column prop="syncId" label="ID" width="60" />
        <el-table-column prop="syncStatus" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.syncStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.syncStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="addCount" label="新增" width="70" />
        <el-table-column prop="updateCount" label="更新" width="70" />
        <el-table-column prop="offlineCount" label="下线" width="70" />
        <el-table-column prop="operator" label="操作人" width="90" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="failMsg" label="失败信息" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectPage, projectCreate, projectUpdate, projectDelete, envList, envCreate, envUpdate, envDelete, syncRun, syncLog } from '../api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const data = await projectPage(query)
    list.value = data.list || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

const editVisible = ref(false)
const form = reactive({})

function openCreate() {
  Object.assign(form, { projectId: null, projectName: '', systemName: '', owner: '', manageUrl: '', projectDesc: '', projectStatus: 1 })
  editVisible.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  editVisible.value = true
}

async function save() {
  if (!form.projectName) return ElMessage.warning('请输入项目名称')
  if (form.projectId) await projectUpdate(form)
  else await projectCreate(form)
  editVisible.value = false
  ElMessage.success('已保存')
  load()
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除项目「${row.projectName}」？其环境配置将一并删除`, '删除确认', { type: 'warning' })
  await projectDelete(row.projectId)
  ElMessage.success('已删除')
  load()
}

/* ---------- 环境 ---------- */
const envVisible = ref(false)
const envEditVisible = ref(false)
const envProject = ref(null)
const envs = ref([])
const envForm = reactive({})

async function openEnv(row) {
  envProject.value = row
  envs.value = await envList(row.projectId)
  envVisible.value = true
}

function openEnvEdit(row) {
  Object.assign(envForm, row || { envId: null, projectId: envProject.value.projectId, envName: '', baseUrl: 'http://', envDesc: '', isDefault: 0 })
  envEditVisible.value = true
}

async function saveEnv() {
  if (!envForm.envName || !envForm.baseUrl) return ElMessage.warning('环境名与 Base URL 必填')
  if (envForm.envId) await envUpdate(envForm)
  else await envCreate(envForm)
  envEditVisible.value = false
  envs.value = await envList(envProject.value.projectId)
  ElMessage.success('已保存')
}

async function deleteEnv(row) {
  await ElMessageBox.confirm(`确认删除环境「${row.envName}」？`, '删除确认', { type: 'warning' })
  await envDelete(row.envId)
  envs.value = await envList(envProject.value.projectId)
}

/* ---------- 同步 ---------- */
const syncLogVisible = ref(false)
const syncLogs = ref([])

async function onSync(row) {
  await ElMessageBox.confirm(`从 ${row.manageUrl || '（未配置 actuator 地址）'} 同步接口到项目「${row.projectName}」？`, '同步确认')
  row._syncing = true
  try {
    const r = await syncRun(row.projectId)
    ElMessage.success(`同步完成：新增 ${r.addCount}，更新 ${r.updateCount}，下线 ${r.offlineCount}`)
    syncLogs.value = await syncLog(row.projectId)
    syncLogVisible.value = true
  } finally {
    row._syncing = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-card { background: var(--el-bg-color); border-radius: 8px; padding: 16px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
.form-tip { color: #c0c4cc; font-size: 12px; line-height: 1.4; }
</style>
