import { resolveApiBaseUrl, resolveApiMode } from './apiMode.js'
import { ApiError } from './httpClient.js'

async function parseJson(response, path) {
  let payload
  try {
    payload = await response.json()
  } catch {
    throw new ApiError(`接口返回无法解析：${path}`, 5000, response.status)
  }
  if (!response.ok || payload.code !== 0) {
    throw new ApiError(payload.msg || `接口请求失败：${path}`, payload.code, response.status)
  }
  return payload.data
}

export async function getPublicShareView(token, { baseUrl = resolveApiBaseUrl() } = {}) {
  const normalizedToken = encodeURIComponent(String(token || '').trim())
  if (!normalizedToken) throw new ApiError('分享 token 不能为空', 1002, 400)
  if (resolveApiMode() === 'mock') {
    return {
      token: normalizedToken,
      route: '浦东软件园 -> 静安寺',
      passengerName: '王*',
      driverName: '李*',
      orderStatus: '履约中',
      expiresAt: '课程 mock 链接',
      trajectory: [
        { longitude: 121.6123, latitude: 31.2145, reportedAt: '09:10' },
        { longitude: 121.5642, latitude: 31.2251, reportedAt: '09:15' },
        { longitude: 121.5006, latitude: 31.2348, reportedAt: '09:20' }
      ]
    }
  }
  const path = `/api/safety/share-links/${normalizedToken}`
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'GET',
    headers: { Accept: 'application/json' }
  })
  return parseJson(response, path)
}
