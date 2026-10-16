<template>
  <div v-loading="loading">
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card" @click="$router.push(card.to)">
          <div class="stat-icon" :style="{ background: card.bg }">
            <el-icon :size="22" color="#fff"><component :is="card.icon" /></el-icon>
          </div>
          <div>
            <div class="stat-num">{{ card.value }}</div>
            <div class="stat-label">
              {{ card.label }}
              <span v-if="card.extra" class="stat-extra">{{ card.extra }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近调用 / 最近运行 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="14">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">最近接口调用</span>
            <el-link type="primary" :underline="false" @click="gotoApis">全部接口</el-link>
          </div>
          <el-table :data="data.recentDebugs || []" size="small" :show-header="true">
            <el-table-column label="接口" min-width="220">
              <template #default="{ row }">
                <div class="api-line">
                  <el-tag size="small" :type="methodType(row.requestMethod)" class="method-tag">{{ row.requestMethod }}</el-tag>
                  <span class="api-url" :title="row.requestUrl">{{ row.requestUrl }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="90" align="right">
              <template #default="{ row }">{{ row.costMs != null ? row.costMs + 'ms' : '-' }}</template>
            </el-table-column>
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
            </el-table-column>
            <template #empty>暂无调用记录</template>
          </el-table>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">最近流程运行</span>
            <el-link type="primary" :underline="false" @click="$router.push('/flow')">流程编排</el-link>
          </div>
          <el-table :data="data.recentRuns || []" size="small">
            <el-table-column label="流程" min-width="150">
              <template #default="{ row }">
                <el-link :underline="false" @click="$router.push(`/flow/run/${row.runId}`)">{{ row.flowNameSnapshot || '-' }}</el-link>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="runTag(row.runStatus)">{{ runText(row.runStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="90" align="right">
              <template #default="{ row }">{{ row.totalCostMs != null ? row.totalCostMs + 'ms' : '-' }}</template>
            </el-table-column>
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
            </el-table-column>
            <template #empty>暂无运行记录</template>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <div class="panel" style="margin-top:16px">
      <div class="panel-head"><span class="panel-title">快捷入口</span></div>
      <div class="quick-grid">
        <div class="quick-item" v-for="q in quickLinks" :key="q.title" @click="$router.push(q.to)">
          <el-icon :size="26" :color="q.color"><component :is="q.icon" /></el-icon>
          <div>
            <div class="quick-title">{{ q.title }}</div>
            <div class="quick-desc">{{ q.desc }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { sysDashboard } from '../api'

const router = useRouter()
const loading = ref(false)
const data = ref({})

const statCards = computed(() => [
  { label: '项目总数', value: data.value.projectCount ?? '-', icon: 'Folder', bg: '#409eff', to: '/project' },
  { label: '接口总数', value: data.value.apiCount ?? '-', icon: 'Connection', bg: '#67c23a', to: '/project' },
  { label: '流程总数', value: data.value.flowCount ?? '-', icon: 'Guide', bg: '#e6a23c', to: '/flow' },
  {
    label: '今日调试',
    value: data.value.todayDebugCount ?? '-',
    icon: 'DataLine',
    bg: '#f56c6c',
    to: '/project',
    extra: data.value.todayFailCount ? `失败 ${data.value.todayFailCount}` : ''
  }
])

const quickLinks = [
  { title: '接口调试', desc: '类 Postman 在线调试接口', icon: 'Promotion', color: '#409eff', to: '/project' },
  { title: '流程编排', desc: '组合接口形成完整业务流程', icon: 'Guide', color: '#e6a23c', to: '/flow' },
  { title: 'AI 助手', desc: '生成测试数据 / 文档 / 流程', icon: 'MagicStick', color: '#9254de', to: '/ai' },
  { title: '系统设置', desc: 'AI 模型与账号安全配置', icon: 'Setting', color: '#909399', to: '/settings' }
]

function gotoApis() {
  router.push('/project')
}

function methodType(m) {
  const map = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger', PATCH: 'warning' }
  return map[m] || 'info'
}

function runTag(s) {
  return s === 'SUCCESS' ? 'success' : s === 'FAILED' ? 'danger' : 'warning'
}

function runText(s) {
  return s === 'SUCCESS' ? '成功' : s === 'FAILED' ? '失败' : '运行中'
}

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(async () => {
  loading.value = true
  try {
    data.value = await sysDashboard()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.stat-card:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-num { font-size: 24px; font-weight: 700; color: var(--el-text-color-primary); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--el-text-color-secondary); margin-top: 2px; }
.stat-extra { color: #f56c6c; font-size: 12px; }
.panel { background: var(--el-bg-color); border: 1px solid var(--el-border-color); border-radius: 8px; padding: 14px 16px; }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.panel-title { font-weight: 600; font-size: 14px; color: var(--el-text-color-primary); }
.api-line { display: flex; align-items: center; gap: 8px; }
.method-tag { width: 62px; justify-content: center; }
.api-url { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-family: monospace; font-size: 12px; }
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.quick-item { display: flex; align-items: center; gap: 12px; padding: 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 8px; cursor: pointer; transition: all 0.2s; }
.quick-item:hover { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.quick-title { font-weight: 600; font-size: 14px; color: var(--el-text-color-primary); }
.quick-desc { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
</style>
