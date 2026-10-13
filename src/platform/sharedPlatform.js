import { reactive } from 'vue'
import { createAdminApi } from '../api/adminApi.js'
import { createDriverApi } from '../api/driverApi.js'
import { createPassengerApi, createPassengerGroupState } from '../api/passengerApi.js'
import { createCGroupState } from '../mock/localCGroupStore.js'
import { resolveApiMode } from '../api/apiMode.js'

export function createSharedPlatform({ reactiveStates = false, apiMode } = {}) {
  const wrap = reactiveStates ? reactive : (value) => value
  const mode = resolveApiMode(apiMode)
  const passengerState = wrap(createPassengerGroupState())
  const cState = wrap(createCGroupState())
  const passengerApi = createPassengerApi(passengerState, { mode })
  const driverApi = createDriverApi({ passengerState, cState, passengerApi }, { mode })
  const adminApi = createAdminApi({ passengerState, cState, passengerApi }, { mode })
  const ready = Promise.all([
    passengerApi.initialize?.(),
    driverApi.initialize?.(),
    adminApi.initialize?.()
  ])

  return {
    mode,
    ready,
    passengerState,
    cState,
    passengerApi,
    driverApi,
    adminApi
  }
}

const sharedPlatform = createSharedPlatform({ reactiveStates: true })

export function useSharedPlatform() {
  return sharedPlatform
}
