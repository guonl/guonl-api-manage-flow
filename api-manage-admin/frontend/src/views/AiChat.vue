<template>
  <div class="page-card chat-card">
    <div class="chat-head">
      <el-icon style="color: var(--apiflow-primary)"><MagicStick /></el-icon>
      <span>AI 助手</span>
      <el-tag v-if="status.model" size="small" type="info" style="margin-left:8px">{{ status.model }}</el-tag>
      <div style="flex:1" />
      <el-button size="small" @click="clearChat" :disabled="messages.length === 0">清空对话</el-button>
    </div>

    <el-alert
      v-if="status.configured === false"
      title="AI 服务尚未配置，请先到「系统设置」完成模型配置"
      type="warning" show-icon :closable="false"
    >
      <el-button size="small" type="primary" link @click="$router.push('/settings')">前往配置</el-button>
    </el-alert>

    <div class="chat-body" ref="bodyRef">
      <div v-if="messages.length === 0" class="chat-empty">
        <div class="empty-icon"><el-icon :size="42" color="#c0c4cc"><ChatDotRound /></el-icon></div>
        <div class="empty-title">你好，我是 APIFlow 智能助手</div>
        <div class="empty-sub">精通 REST API 设计、接口测试与流程编排，试试这些问题：</div>
        <div class="suggest-wrap">
          <el-tag
            v-for="s in suggestions" :key="s" class="suggest"
            effect="plain" @click="input = s; send()"
          >{{ s }}</el-tag>
        </div>
      </div>

      <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
        <div class="avatar">
          <el-icon v-if="m.role === 'user'"><UserFilled /></el-icon>
          <el-icon v-else style="color: var(--apiflow-primary)"><MagicStick /></el-icon>
        </div>
        <div class="bubble">
          <div v-if="m.role === 'user'" class="plain">{{ m.content }}</div>
          <div v-else class="md" v-html="renderMd(m.content)"></div>
        </div>
      </div>

      <div v-if="sending" class="msg-row assistant">
        <div class="avatar"><el-icon style="color: var(--apiflow-primary)"><MagicStick /></el-icon></div>
        <div class="bubble typing"><span></span><span></span><span></span></div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="input" type="textarea" :rows="3" resize="none"
        placeholder="输入问题，Enter 发送，Shift+Enter 换行"
        :disabled="sending" @keydown.enter.exact.prevent="send"
      />
      <div class="input-foot">
        <span class="form-tip">内容由 AI 生成，仅供参考</span>
        <el-button type="primary" :loading="sending" @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiChat, aiStatus } from '../api'
import { renderMd } from '../utils/md'

const status = ref({})
const messages = ref([])
const input = ref('')
const sending = ref(false)
const bodyRef = ref(null)

const suggestions = [
  '什么是接口流程编排？举一个实际例子',
  '如何为 REST API 设计合理的断言？',
  '接口测试数据应该怎么构造？',
  'API 管理有哪些最佳实践？'
]

async function loadStatus() {
  try { status.value = await aiStatus() } catch (e) { /* ignore */ }
}

function scrollBottom() {
  nextTick(() => { if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight })
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  sending.value = true
  scrollBottom()
  try {
    // 后端取最后 20 条消息作为上下文
    const history = messages.value.slice(-20).map(m => ({ role: m.role, content: m.content }))
    const reply = await aiChat({ messages: history })
    messages.value.push({ role: 'assistant', content: reply || '（空回复）' })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: `**请求失败**：${e.message || '请检查 AI 配置或稍后重试'}` })
  } finally {
    sending.value = false
    scrollBottom()
  }
}

function clearChat() {
  messages.value = []
}

onMounted(loadStatus)
void ElMessage
</script>

<style scoped>
.chat-card { display: flex; flex-direction: column; height: calc(100vh - 96px); }
.chat-head { display: flex; align-items: center; gap: 6px; font-size: 15px; font-weight: 600; padding-bottom: 12px; border-bottom: 1px solid #e6e8eb; }
.chat-body { flex: 1; overflow-y: auto; padding: 16px 4px; }
.chat-empty { text-align: center; margin-top: 8vh; }
.empty-title { font-size: 17px; font-weight: 600; margin: 10px 0 4px; }
.empty-sub { color: #909399; font-size: 13px; margin-bottom: 16px; }
.suggest-wrap { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; max-width: 560px; margin: 0 auto; }
.suggest { cursor: pointer; }
.msg-row { display: flex; gap: 10px; margin-bottom: 14px; }
.msg-row.user { flex-direction: row-reverse; }
.avatar { width: 32px; height: 32px; border-radius: 6px; background: var(--el-fill-color); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.msg-row.user .avatar { background: var(--el-color-primary); color: #fff; }
.bubble { max-width: 72%; padding: 10px 14px; border-radius: 8px; font-size: 14px; line-height: 1.7; }
.msg-row.assistant .bubble { background: var(--el-fill-color-light); }
.msg-row.user .bubble { background: var(--el-color-primary); color: #fff; }
.plain { white-space: pre-wrap; word-break: break-word; }
.md :deep(.code-block) { background: #282c34; color: #abb2bf; padding: 10px 12px; border-radius: 6px; overflow-x: auto; font-size: 13px; margin: 6px 0; }
.md :deep(.inline-code) { background: #e8eaed; color: #c7254e; padding: 1px 5px; border-radius: 3px; font-size: 13px; }
.md :deep(.md-h) { font-weight: 700; font-size: 14px; margin: 6px 0 2px; }
.md :deep(.md-li) { padding-left: 8px; }
.typing { display: flex; gap: 4px; align-items: center; }
.typing span { width: 6px; height: 6px; border-radius: 50%; background: #c0c4cc; animation: blink 1.2s infinite; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0%, 80%, 100% { opacity: 0.2; } 40% { opacity: 1; } }
.chat-input { border-top: 1px solid #e6e8eb; padding-top: 12px; }
.input-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.form-tip { color: #c0c4cc; font-size: 12px; }
</style>
