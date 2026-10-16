<template>
  <el-container style="height:100%">
    <el-aside :width="collapsed ? '64px' : '200px'" class="aside" :class="{ collapsed }">
      <div class="logo">
        <el-icon :size="22" style="color: var(--apiflow-primary)"><Connection /></el-icon>
        <span v-show="!collapsed">APIFlow</span>
      </div>
      <el-menu router :default-active="$route.path" class="menu"
               :collapse="collapsed" :collapse-transition="false">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon><template #title>工作台</template>
        </el-menu-item>
        <el-menu-item index="/project">
          <el-icon><Folder /></el-icon><template #title>项目管理</template>
        </el-menu-item>
        <el-menu-item v-if="currentProject" :index="`/apis/${currentProject.projectId}`">
          <el-icon><Connection /></el-icon><template #title>接口管理</template>
        </el-menu-item>
        <el-menu-item index="/flow">
          <el-icon><Guide /></el-icon><template #title>流程编排</template>
        </el-menu-item>
        <el-menu-item index="/ai">
          <el-icon><MagicStick /></el-icon><template #title>AI 助手</template>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon><template #title>系统设置</template>
        </el-menu-item>
      </el-menu>
      <div class="aside-foot">{{ collapsed ? '' : (user || '-') }}</div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="icon-btn" :title="collapsed ? '展开侧边栏' : '折叠侧边栏'" @click="toggleAside">
            <el-icon :size="18"><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </span>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentProject">{{ currentProject.projectName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-popover placement="bottom-end" :width="172" trigger="click">
            <template #reference>
              <span class="icon-btn" title="主题色"><el-icon :size="18"><Brush /></el-icon></span>
            </template>
            <div class="theme-panel">
              <div class="theme-label">主题色</div>
              <div class="swatches">
                <span v-for="p in THEME_PRESETS" :key="p.key" class="swatch"
                      :style="{ background: p.value }" :title="p.label"
                      :class="{ active: p.value === primary }" @click="setPrimary(p.value)" />
              </div>
              <div class="theme-label">外观模式</div>
              <el-radio-group :model-value="mode" size="small" @change="setMode">
                <el-radio-button value="light">浅色</el-radio-button>
                <el-radio-button value="dark">深色</el-radio-button>
              </el-radio-group>
            </div>
          </el-popover>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-icon><UserFilled /></el-icon>{{ user || '未登录' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main style="padding:16px">
        <router-view />
      </el-main>
      <el-footer class="footer" height="40px">
        <a class="gh-link" href="https://github.com/guonl/guonl-api-manage-flow" target="_blank" rel="noopener">
          <svg viewBox="0 0 16 16" width="16" height="16" fill="currentColor" aria-hidden="true">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"/>
          </svg>
          <span>guonl</span>
        </a>
        <span class="copy">© {{ year }} guonl. 保留所有权利.</span>
      </el-footer>
    </el-container>

    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="不少于6位" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="doChangePassword">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, reactive, ref, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logout, changePassword, projectList } from '../api'
import { THEME_PRESETS, getTheme, setTheme } from '../utils/theme'

const route = useRouter()
const r = useRoute()
const user = computed(() => localStorage.getItem('apiflow_user'))
const currentProject = ref(null)
const routePid = computed(() => Number(r.params.projectId))

// 侧边栏折叠状态（本地持久化）
const collapsed = ref(localStorage.getItem('apiflow_aside_collapsed') === '1')
function toggleAside() {
  collapsed.value = !collapsed.value
  localStorage.setItem('apiflow_aside_collapsed', collapsed.value ? '1' : '0')
}

// 版权年份（自动取当前年份）
const year = new Date().getFullYear()

// 主题状态
const mode = ref(getTheme().mode)
const primary = ref(getTheme().primary)
function setMode(m) {
  mode.value = setTheme({ mode: m }).mode
}
function setPrimary(c) {
  primary.value = setTheme({ primary: c }).primary
}

// 读取当前项目名用于面包屑
watchEffect(async () => {
  if (routePid.value) {
    try {
      const list = await projectList()
      currentProject.value = list.find(p => p.projectId === routePid.value) || null
    } catch (e) { /* ignore */ }
  } else {
    currentProject.value = null
  }
})

const pwdVisible = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

function onCommand(cmd) {
  if (cmd === 'logout') {
    logout().finally(() => {
      localStorage.removeItem('apiflow_user')
      route.push('/login')
    })
  } else if (cmd === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdVisible.value = true
  }
}

async function doChangePassword() {
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码不少于6位')
    return
  }
  await changePassword(pwdForm)
  pwdVisible.value = false
  ElMessage.success('密码已修改，请重新登录')
  localStorage.removeItem('apiflow_user')
  route.push('/login')
}

void ElMessageBox
</script>

<style scoped>
.aside { background: var(--el-bg-color); border-right: 1px solid var(--el-border-color); display: flex; flex-direction: column; transition: width .25s; }
.logo { height: 56px; display: flex; align-items: center; gap: 8px; padding: 0 20px; font-size: 18px; font-weight: 700; color: var(--el-text-color-primary); white-space: nowrap; overflow: hidden; }
.aside.collapsed .logo { justify-content: center; padding: 0; }
.menu { border-right: none; flex: 1; }
.aside-foot { padding: 14px 20px; color: var(--el-text-color-placeholder); font-size: 12px; white-space: nowrap; overflow: hidden; }
.header { background: var(--el-bg-color); border-bottom: 1px solid var(--el-border-color); display: flex; align-items: center; justify-content: space-between; }
.header-left, .header-right { display: flex; align-items: center; gap: 14px; }
.icon-btn { display: inline-flex; align-items: center; cursor: pointer; color: var(--el-text-color-regular); }
.icon-btn:hover { color: var(--el-color-primary); }
.user-chip { display: flex; align-items: center; gap: 6px; cursor: pointer; color: var(--el-text-color-primary); font-size: 14px; }
.theme-panel { display: flex; flex-direction: column; gap: 10px; }
.theme-label { font-size: 12px; color: var(--el-text-color-secondary); }
.swatches { display: flex; gap: 10px; }
.swatch { width: 22px; height: 22px; border-radius: 50%; cursor: pointer; display: inline-block; border: 2px solid transparent; box-sizing: content-box; }
.swatch.active { border-color: var(--el-text-color-secondary); box-shadow: 0 0 0 2px var(--el-bg-color) inset; }
.footer { background: var(--el-bg-color); border-top: 1px solid var(--el-border-color); display: flex; align-items: center; justify-content: space-between; padding: 0 20px; font-size: 12px; color: var(--el-text-color-secondary); }
.gh-link { display: inline-flex; align-items: center; gap: 6px; color: var(--el-text-color-secondary); text-decoration: none; transition: color .2s; }
.gh-link:hover { color: var(--el-color-primary); }
</style>
