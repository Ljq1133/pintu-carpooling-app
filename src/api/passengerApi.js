import { resolveApiMode } from './apiMode.js'
import { createPassengerHttpApi } from './passengerApi.http.js'
import {
  DRIVER_CREDIT_MIN,
  MATCH_REQUEST_STATUS,
  ORDER_STATUS,
  PAYMENT_STATUS,
  REVIEW_STATUS,
  createPassengerApi as createPassengerMockApi,
  createPassengerGroupState,
  mapBackendOrderStatus,
  passengerApiNeeds,
  passengerIntegrationChecklist,
  passengerStatusContract
} from './passengerApi.mock.js'

export {
  DRIVER_CREDIT_MIN,
  MATCH_REQUEST_STATUS,
  ORDER_STATUS,
  PAYMENT_STATUS,
  REVIEW_STATUS,
  createPassengerGroupState,
  mapBackendOrderStatus,
  passengerApiNeeds,
  passengerIntegrationChecklist,
  passengerStatusContract
}

export function createPassengerApi(state, { mode } = {}) {
  return resolveApiMode(mode) === 'http'
    ? createPassengerHttpApi(state)
    : createPassengerMockApi(state)
}
