import { resolveApiMode } from './apiMode.js'
import { createAdminHttpApi } from './adminApi.http.js'
import { createAdminApi as createAdminMockApi } from './adminApi.mock.js'

export function createAdminApi(context, { mode } = {}) {
  return resolveApiMode(mode) === 'http'
    ? createAdminHttpApi(context)
    : createAdminMockApi(context)
}
