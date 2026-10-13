import { resolveApiBaseUrl } from './apiMode.js'
import { clearSession, getSession, setSession } from '../auth/session.js'

export class ApiError extends Error {
  constructor(message, code = 5000, status = 500) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

const demoAccounts = {
  'passenger-001': { username: 'passenger_demo', password: '123456', role: 'passenger' },
  'driver-001': { username: 'driver_demo', password: '123456', role: 'driver' },
  'admin-001': { username: 'admin_demo', password: '123456', role: 'admin' }
}

export function createHttpClient({ userId, baseUrl = resolveApiBaseUrl() } = {}) {
  let accessToken = ''
  let refreshToken = ''
  const pendingMutations = new Map()
  const account = demoAccounts[userId]

  function sessionMatchesClient(session) {
    if (!session?.token) return false
    if (!account) return true
    return session.user?.id === userId || session.user?.role === account.role
  }

  function syncSessionTokens() {
    const session = getSession()
    if (!sessionMatchesClient(session)) return false
    accessToken = session.token
    refreshToken = session.refreshToken || ''
    return true
  }

  syncSessionTokens()

  async function parseResponse(response, path) {
    try {
      return await response.json()
    } catch {
      throw new ApiError(`接口返回了无法解析的数据：${path}`, 5000, response.status)
    }
  }

  async function login() {
    if (!account) throw new ApiError(`未配置演示账号：${userId}`, 1001, 401)
    const response = await fetch(`${baseUrl}/api/auth/login`, {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify(account)
    })
    const payload = await parseResponse(response, '/api/auth/login')
    if (!response.ok || payload.code !== 0) {
      throw new ApiError(payload.msg || '登录失败', payload.code, response.status)
    }
    accessToken = payload.data.token
    refreshToken = payload.data.refreshToken
  }

  async function refresh() {
    if (!refreshToken) return false
    const response = await fetch(`${baseUrl}/api/auth/refresh`, {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    })
    const payload = await parseResponse(response, '/api/auth/refresh')
    if (!response.ok || payload.code !== 0) {
      clearSession()
      accessToken = ''
      refreshToken = ''
      return false
    }
    accessToken = payload.data.token
    refreshToken = payload.data.refreshToken
    const currentSession = getSession()
    if (sessionMatchesClient(currentSession)) {
      setSession({ ...currentSession, ...payload.data, user: currentSession.user })
    }
    return true
  }

  async function executeRequest(path, { method = 'GET', body } = {}, retried = false) {
    syncSessionTokens()
    if (!accessToken) await login()
    const headers = {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`
    }
    if (body !== undefined) headers['Content-Type'] = 'application/json'

    const response = await fetch(`${baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    })

    const payload = await parseResponse(response, path)
    if (response.status === 401 && !retried && await refresh()) {
      return executeRequest(path, { method, body }, true)
    }
    if (!response.ok || payload.code !== 0) {
      throw new ApiError(payload.msg || `接口请求失败：${path}`, payload.code, response.status)
    }
    return payload.data
  }

  async function executeTextRequest(path, { method = 'GET', body } = {}, retried = false) {
    syncSessionTokens()
    if (!accessToken) await login()
    const headers = {
      Accept: 'text/csv,text/plain,*/*',
      Authorization: `Bearer ${accessToken}`
    }
    if (body !== undefined) headers['Content-Type'] = 'application/json'

    const response = await fetch(`${baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    })

    if (response.status === 401 && !retried && await refresh()) {
      return executeTextRequest(path, { method, body }, true)
    }
    if (!response.ok) {
      throw new ApiError(`接口请求失败：${path}`, 5000, response.status)
    }
    return response.text()
  }

  async function executeFormRequest(path, formData, { method = 'POST' } = {}, retried = false) {
    syncSessionTokens()
    if (!accessToken) await login()
    const response = await fetch(`${baseUrl}${path}`, {
      method,
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${accessToken}`
      },
      body: formData
    })

    const payload = await parseResponse(response, path)
    if (response.status === 401 && !retried && await refresh()) {
      return executeFormRequest(path, formData, { method }, true)
    }
    if (!response.ok || payload.code !== 0) {
      throw new ApiError(payload.msg || `接口请求失败：${path}`, payload.code, response.status)
    }
    return payload.data
  }

  function request(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase()
    if (['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      return executeRequest(path, options)
    }

    const key = `${method}:${path}:${JSON.stringify(options.body ?? null)}`
    if (pendingMutations.has(key)) return pendingMutations.get(key)
    const pending = executeRequest(path, options).finally(() => pendingMutations.delete(key))
    pendingMutations.set(key, pending)
    return pending
  }

  return {
    login,
    get(path, options) {
      return request(path, options)
    },
    post(path, body, options = {}) {
      return request(path, { ...options, method: 'POST', body })
    },
    put(path, body, options = {}) {
      return request(path, { ...options, method: 'PUT', body })
    },
    delete(path, options = {}) {
      return request(path, { ...options, method: 'DELETE' })
    },
    text(path, options) {
      return executeTextRequest(path, options)
    },
    upload(path, formData, options) {
      return executeFormRequest(path, formData, options)
    }
  }
}
