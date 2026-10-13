import { resolveApiMode } from './apiMode.js'
import { createDriverHttpApi } from './driverApi.http.js'
import { createDriverApi as createDriverMockApi } from './driverApi.mock.js'

export function createDriverApi(context, { mode } = {}) {
  return resolveApiMode(mode) === 'http'
    ? createDriverHttpApi(context)
    : createDriverMockApi(context)
}
