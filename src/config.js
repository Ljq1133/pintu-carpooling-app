/**
 * 前端运行时配置入口。
 * <p>
 * Vite 在构建时把 import.meta.env.VITE_* 注入到 bundle，
 * 但运行时仍然可以通过 window.__PINTU_RUNTIME_CONFIG__ 注入到容器中（部署后不重新构建即可切环境）。
 * </p>
 */
const runtime =
  (typeof window !== 'undefined' && window.__PINTU_RUNTIME_CONFIG__) || {}

const env = import.meta.env || {}

export const config = Object.freeze({
  apiBaseUrl: runtime.VITE_API_BASE_URL ?? env.VITE_API_BASE_URL ?? '',
  apiMode: runtime.VITE_API_MODE ?? env.VITE_API_MODE ?? 'http',
  mapProvider: runtime.VITE_MAP_PROVIDER ?? env.VITE_MAP_PROVIDER ?? 'mock',
  features: {
    useMockApi: (runtime.VITE_API_MODE ?? env.VITE_API_MODE ?? 'http') === 'mock'
  }
})

export function isHttpMode() {
  return config.apiMode === 'http'
}
