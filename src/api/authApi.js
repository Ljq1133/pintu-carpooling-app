import { resolveApiBaseUrl } from './apiMode.js'
import { setSession } from '../auth/session.js'

const baseUrl = resolveApiBaseUrl()

async function publicRequest(path, body) {
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  const payload = await response.json().catch(() => ({
    code: 5000,
    msg: '服务器返回了无法解析的数据'
  }))
  if (!response.ok || payload.code !== 0) {
    const error = new Error(payload.msg || '请求失败')
    error.code = payload.code
    error.status = response.status
    throw error
  }
  return payload.data
}

function persistAuth(data) {
  setSession(data)
  return data
}

export const authApi = {
  passwordLogin(body) {
    return publicRequest('/api/auth/login', body).then(persistAuth)
  },
  codeLogin(body) {
    return publicRequest('/api/auth/login/code', body).then(persistAuth)
  },
  register(body) {
    return publicRequest('/api/auth/register', body).then(persistAuth)
  },
  sendCode(phone, purpose) {
    return publicRequest('/api/auth/codes', { phone, purpose })
  },
  forgotPassword(phone) {
    return publicRequest('/api/auth/password/forgot', { phone })
  },
  resetPassword(body) {
    return publicRequest('/api/auth/password/reset', body)
  }
}

