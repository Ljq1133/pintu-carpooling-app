export function resolveApiMode(explicitMode) {
  if (explicitMode === 'http' || explicitMode === 'mock') return explicitMode

  if (typeof window !== 'undefined') {
    const queryMode = new URLSearchParams(window.location.search).get('api')
    if (queryMode === 'http' || queryMode === 'mock') return queryMode
    return import.meta.env?.VITE_API_MODE === 'mock' ? 'mock' : 'http'
  }

  return 'mock'
}

export function resolveApiBaseUrl() {
  return String(import.meta.env?.VITE_API_BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '')
}
