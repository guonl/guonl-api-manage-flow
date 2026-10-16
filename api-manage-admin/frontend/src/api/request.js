import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({ baseURL: '/', timeout: 60000 })

request.interceptors.response.use(
  (resp) => {
    // blob 直接透传
    if (resp.config.responseType === 'blob') return resp
    const body = resp.data
    if (body && body.code === 0) {
      return body.data === undefined ? body : body.data
    }
    if (body && body.code === 401) {
      localStorage.removeItem('apiflow_user')
      router.push('/login')
    }
    ElMessage.error(body?.msg || `请求失败(${body?.code})`)
    return Promise.reject(new Error(body?.msg || '请求失败'))
  },
  (err) => {
    const body = err.response?.data
    if (err.response?.status === 401 || body?.code === 401) {
      localStorage.removeItem('apiflow_user')
      router.push('/login')
      ElMessage.error('未登录或登录已过期')
    } else {
      ElMessage.error(body?.msg || err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export default request
