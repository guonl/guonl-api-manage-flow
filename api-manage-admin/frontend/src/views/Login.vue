<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-title">
        <el-icon :size="34" style="color: var(--apiflow-primary)"><Connection /></el-icon>
        <span>APIFlow</span>
      </div>
      <div class="login-sub">API 管理与流程编排平台</div>
      <el-form :model="form" @keyup.enter="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" show-password placeholder="密码" size="large">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">登 录</el-button>
      </el-form>
      <div class="login-tip">默认账号 admin / admin123</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'

const router = useRouter()
const form = reactive({ username: 'admin', password: '' })
const loading = ref(false)

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await login(form)
    localStorage.setItem('apiflow_user', form.username)
    ElMessage.success('登录成功')
    router.push('/project')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #2b4a6f 55%, #409eff 130%);
}
.login-card {
  width: 380px;
  padding: 40px 36px 28px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, .25);
}
.login-title { display: flex; align-items: center; gap: 10px; font-size: 26px; font-weight: 700; }
.login-sub { color: var(--el-text-color-secondary); font-size: 13px; margin: 8px 0 26px; }
.login-tip { color: var(--el-text-color-placeholder); font-size: 12px; text-align: center; margin-top: 16px; }
</style>
