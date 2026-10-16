// 主题工具：暗色模式 + 主色切换，持久化到 localStorage
const STORAGE_KEY = 'apiflow_theme'

export const THEME_PRESETS = [
  { key: 'blue', label: '科技蓝', value: '#409eff' },
  { key: 'green', label: '翡翠绿', value: '#00b96b' },
  { key: 'purple', label: '典雅紫', value: '#722ed1' },
  { key: 'orange', label: '活力橙', value: '#fa8c16' },
  { key: 'rose', label: '热情红', value: '#f5488b' }
]

function hex2rgb(hex) {
  let h = hex.replace('#', '')
  if (h.length === 3) h = h.split('').map(c => c + c).join('')
  const n = parseInt(h, 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

// 颜色混合：weight 为 b 的占比
function mix(a, b, weight) {
  const [r1, g1, b1] = hex2rgb(a)
  const [r2, g2, b2] = hex2rgb(b)
  const c = i => Math.round(r1 * (1 - weight) + [r2, g2, b2][i] * weight)
  return `rgb(${c(0)}, ${c(1)}, ${c(2)})`
}

export function getTheme() {
  let t = { mode: 'light', primary: '#409eff' }
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) t = { ...t, ...JSON.parse(raw) }
  } catch (e) { /* ignore */ }
  return t
}

export function setTheme(patch) {
  const next = { ...getTheme(), ...patch }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  applyTheme(next.mode, next.primary)
  return next
}

function applyTheme(mode, primary) {
  const root = document.documentElement
  root.classList.toggle('dark', mode === 'dark')
  // 暗色下 Element Plus 的 light 系列变量向黑色混合
  const softTarget = mode === 'dark' ? '#000000' : '#ffffff'
  const darkTarget = mode === 'dark' ? '#ffffff' : '#000000'
  root.style.setProperty('--el-color-primary', primary)
  root.style.setProperty('--apiflow-primary', primary)
  for (const i of [3, 5, 7, 8, 9]) {
    root.style.setProperty(`--el-color-primary-light-${i}`, mix(primary, softTarget, i / 10))
  }
  root.style.setProperty('--el-color-primary-dark-2', mix(primary, darkTarget, 0.2))
}

// 应用启动时调用，避免主题闪烁
export function initTheme() {
  const { mode, primary } = getTheme()
  applyTheme(mode, primary)
}
