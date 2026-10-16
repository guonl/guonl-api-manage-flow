<template>
  <div class="settings-page">
    <!-- AI 模型配置 -->
    <div class="page-card">
      <div class="card-title">
        <el-icon color="#409eff"><MagicStick /></el-icon>
        <span>AI 模型配置</span>
        <el-tag :type="statusTagType" size="small" style="margin-left:8px">{{ statusTagText }}</el-tag>
        <div style="flex:1" />
        <el-button :loading="testing" @click="testConn"><el-icon><Promotion /></el-icon>测试连通</el-button>
      </div>

      <el-alert
        v-if="loaded && !form.baseUrl || !form.model"
        title="尚未配置模型服务，AI 助手相关功能不可用。填写 OpenAI 兼容协议的网关地址、API Key 与模型名后保存。"
        type="info" show-icon :closable="false" style="margin-bottom:14px"
      />

      <el-form label-width="100px" style="max-width:640px" :disabled="testing">
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" placeholder="https://api.openai.com/v1（需含版本路径）" />
          <div class="form-tip">OpenAI 兼容协议地址，必须包含版本路径，如 https://host/v1</div>
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password :placeholder="maskedKey || 'sk-...'" />
          <div class="form-tip">{{ maskedKey ? `已配置（${maskedKey}），留空表示不修改` : '首次使用请填写访问密钥' }}</div>
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="form.model" placeholder="gpt-4o / glm-4-flash / qwen-plus ..." />
        </el-form-item>
        <el-form-item label="温度">
          <el-input v-model="form.temperature" placeholder="0.7（可选，留空使用模型默认）" style="width:240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
          <span v-if="testResult" class="test-result" :class="testResult.success ? 'ok' : 'fail'">
            {{ testResult.success ? '连通成功' : '连通失败' }} · {{ testResult.latencyMs }}ms · {{ testResult.reply }}
          </span>
        </el-form-item>
      </el-form>
    </div>

    <!-- 账号安全 -->
    <div class="page-card" style="margin-top:16px">
      <div class="card-title">
        <el-icon color="#67c23a"><Lock /></el-icon>
        <span>账号安全</span>
      </div>
      <el-form label-width="100px" style="max-width:640px">
        <el-form-item label="原密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password style="width:320px" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" type="password" show-password placeholder="不少于6位" style="width:320px" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwd.confirm" type="password" show-password style="width:320px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="changePwd">修改密码</el-button>
          <div class="form-tip">修改成功后需使用新密码重新登录</div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { aiStatus, aiTest, sysConfig, sysConfigSave, sysChangePassword } from '../api'

/* ---------- AI 配置 ---------- */
const loaded = ref(false)
const saving = ref(false)
const testing = ref(false)
const testResult = ref(null)
const maskedKey = ref('')
const form = reactive({ baseUrl: '', apiKey: '', model: '', temperature: '' })

const statusInfo = ref({ configured: false, ready: false, model: '' })
const statusTagType = computed(() => (statusInfo.value.ready ? 'success' : statusInfo.value.configured ? 'warning' : 'info'))
const statusTagText = computed(() =>
  statusInfo.value.ready ? '可用' : statusInfo.value.configured ? '已配置' : '未配置')

async function loadStatus() {
  statusInfo.value = await aiStatus()
}

async function loadConfig() {
  const list = await sysConfig()
  const map = {}
  list.forEach(c => { map[c.configKey] = c.configValue || '' })
  form.baseUrl = map['ai.base-url'] || ''
  form.model = map['ai.model'] || ''
  form.temperature = map['ai.temperature'] || ''
  maskedKey.value = map['ai.api-key'] || ''
  loaded.value = true
}

async function save() {
  if (!form.baseUrl || !form.model) return ElMessage.warning('Base URL 与模型名称必填')
  saving.value = true
  try {
    const items = [
      { configKey: 'ai.base-url', configValue: form.baseUrl, configDesc: 'AI 网关地址(OpenAI兼容)' },
      { configKey: 'ai.model', configValue: form.model, configDesc: 'AI 模型名' },
      { configKey: 'ai.temperature', configValue: form.temperature, configDesc: 'AI 温度' }
    ]
    if (form.apiKey) items.push({ configKey: 'ai.api-key', configValue: form.apiKey, configDesc: 'AI 访问密钥' })
    await sysConfigSave(items)
    ElMessage.success('已保存')
    form.apiKey = ''
    await Promise.all([loadStatus(), loadConfig()])
  } finally {
    saving.value = false
  }
}

async function testConn() {
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await aiTest()
    await loadStatus()
  } finally {
    testing.value = false
  }
}

/* ---------- 修改密码 ---------- */
const router = useRouter()
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写原密码与新密码')
  if (pwd.newPassword.length < 6) return ElMessage.warning('新密码不少于6位')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await sysChangePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改，请重新登录')
  localStorage.removeItem('apiflow_user')
  router.push('/login')
}

onMounted(() => { loadStatus(); loadConfig() })
</script>

<style scoped>
.page-card { background: var(--el-bg-color); border-radius: 8px; padding: 16px; }
.card-title { display: flex; align-items: center; gap: 6px; font-size: 15px; font-weight: 600; margin-bottom: 16px; }
.form-tip { color: #c0c4cc; font-size: 12px; line-height: 1.4; }
.test-result { margin-left: 12px; font-size: 12px; }
.test-result.ok { color: #67c23a; }
.test-result.fail { color: #f56c6c; }
</style>
