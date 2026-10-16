import request from './request'

export const login = (data) => request.post('/api/auth/login', data)
export const logout = () => request.post('/api/auth/logout')
export const me = () => request.get('/api/auth/me')
export const changePassword = (data) => request.put('/api/auth/password', data)

// 项目
export const projectPage = (params) => request.get('/api/project/page', { params })
export const projectList = () => request.get('/api/project/list')
export const projectCreate = (data) => request.post('/api/project', data)
export const projectUpdate = (data) => request.put('/api/project', data)
export const projectDelete = (id) => request.delete(`/api/project/${id}`)

// 环境
export const envList = (projectId) => request.get('/api/env/list', { params: { projectId } })
export const envCreate = (data) => request.post('/api/env', data)
export const envUpdate = (data) => request.put('/api/env', data)
export const envDelete = (id) => request.delete(`/api/env/${id}`)

// 分组
export const groupList = (projectId) => request.get('/api/group/list', { params: { projectId } })
export const groupCreate = (data) => request.post('/api/group', data)
export const groupUpdate = (data) => request.put('/api/group', data)
export const groupDelete = (id) => request.delete(`/api/group/${id}`)

// 接口
export const apiTree = (projectId) => request.get('/api/api/tree', { params: { projectId } })
export const apiSearch = (params) => request.get('/api/api/search', { params })
export const apiDetail = (urlId) => request.get(`/api/api/${urlId}`)
export const apiCreate = (data) => request.post('/api/api', data)
export const apiUpdate = (data) => request.put('/api/api', data)
export const apiUpdateStatus = (urlId, status) => request.put(`/api/api/${urlId}/status`, null, { params: { status } })
export const apiDelete = (urlId) => request.delete(`/api/api/${urlId}`)
export const apiSaveDoc = (urlId, data) => request.put(`/api/api/${urlId}/doc`, data)

// 同步
export const syncRun = (projectId) => request.post(`/api/sync/${projectId}`)
export const syncLog = (projectId) => request.get('/api/sync/log', { params: { projectId, limit: 20 } })

// 调试
export const debugSend = (data) => request.post('/api/debug/send', data)
export const debugTemplate = (urlId) => request.get('/api/debug/template', { params: { urlId } })
export const debugRecords = (params) => request.get('/api/debug/records', { params })
export const debugRecent = (limit = 10) => request.get('/api/debug/records/recent', { params: { limit } })
export const debugRecord = (recordId) => request.get(`/api/debug/record/${recordId}`)

// 流程编排
export const flowPage = (params) => request.get('/api/flow/list', { params })
export const flowDetail = (flowId) => request.get(`/api/flow/${flowId}`)
export const flowCreate = (data) => request.post('/api/flow', data)
export const flowUpdate = (data) => request.put('/api/flow', data)
export const flowDelete = (flowId) => request.delete(`/api/flow/${flowId}`)
export const flowImport = (data) => request.post('/api/flow/import', data)
export const flowExport = (flowId) => request.get(`/api/flow/${flowId}/export`)
export const flowRun = (data) => request.post('/api/flow/run', data)
export const flowRunPage = (params) => request.get('/api/flow/run/list', { params })
export const flowRunDetail = (runId) => request.get(`/api/flow/run/${runId}`)
export const flowRerunStep = (runId, runStepId) => request.post(`/api/flow/run/${runId}/step/${runStepId}/rerun`)

// AI 助手（大模型调用较慢，单独放宽超时）
const AI_TIMEOUT = { timeout: 180000 }
export const aiStatus = () => request.get('/api/ai/status')
export const aiTest = () => request.post('/api/ai/test', null, AI_TIMEOUT)
export const aiChat = (data) => request.post('/api/ai/chat', data, AI_TIMEOUT)
export const aiAnalyze = (data) => request.post('/api/ai/analyze', data, AI_TIMEOUT)
export const aiGenData = (urlId) => request.post('/api/ai/gen-data', { urlId }, AI_TIMEOUT)
export const aiGenDoc = (urlId) => request.post('/api/ai/gen-doc', { urlId }, AI_TIMEOUT)
export const aiGenFlow = (data) => request.post('/api/ai/gen-flow', data, AI_TIMEOUT)

// 系统设置
export const sysConfig = () => request.get('/api/sys/config')
export const sysDashboard = () => request.get('/api/sys/dashboard')
export const sysConfigSave = (items) => request.post('/api/sys/config', items)
export const sysChangePassword = (data) => request.post('/api/sys/password', data)
