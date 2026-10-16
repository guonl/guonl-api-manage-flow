import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台', icon: 'Odometer' } },
      { path: 'project', name: 'project', component: () => import('../views/ProjectList.vue'), meta: { title: '项目管理', icon: 'Folder' } },
      { path: 'apis/:projectId', name: 'api', component: () => import('../views/ApiList.vue'), meta: { title: '接口管理', icon: 'Connection' } },
      { path: 'flow', name: 'flow', component: () => import('../views/FlowList.vue'), meta: { title: '流程编排', icon: 'Guide' } },
      { path: 'flow/editor/:id', name: 'flowEditor', component: () => import('../views/FlowEditor.vue'), meta: { title: '流程编辑器' } },
      { path: 'flow/run/:runId', name: 'flowRun', component: () => import('../views/FlowRunDetail.vue'), meta: { title: '运行详情' } },
      { path: 'ai', name: 'ai', component: () => import('../views/AiChat.vue'), meta: { title: 'AI 助手', icon: 'MagicStick' } },
      { path: 'settings', name: 'settings', component: () => import('../views/Settings.vue'), meta: { title: '系统设置', icon: 'Setting' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const user = localStorage.getItem('apiflow_user')
  if (!to.meta.public && !user) return '/login'
  if (to.path === '/login' && user) return '/dashboard'
  return true
})

export default router
