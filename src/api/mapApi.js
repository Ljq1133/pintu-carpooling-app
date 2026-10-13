import { createHttpClient } from './httpClient.js'
import { resolveApiMode } from './apiMode.js'

const client = createHttpClient({ userId: 'passenger-001' })
const isMockMode = () => resolveApiMode() === 'mock'

const mockLocation = {
  name: '同济科技园',
  address: '同济科技园',
  lng: 121.479,
  lat: 31.226
}

const mockPois = [
  mockLocation,
  { name: 'SOHO', address: 'SOHO', lng: 121.494, lat: 31.233 },
  { name: '三里屯', address: '三里屯', lng: 116.456, lat: 39.936 }
]

const query = (values) => new URLSearchParams(
  Object.entries(values)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => [key, String(value)])
).toString()

export const mapApi = {
  locate() {
    if (isMockMode()) return Promise.resolve(mockLocation)
    return client.get('/api/map/location')
  },
  reverse(lng, lat) {
    if (isMockMode()) return Promise.resolve({ ...mockLocation, lng, lat })
    return client.get(`/api/map/reverse?${query({ lng, lat })}`)
  },
  searchPois(keyword, nearLng, nearLat) {
    if (isMockMode()) {
      const text = String(keyword || '').trim()
      const matches = mockPois.filter((point) => point.name.includes(text) || point.address.includes(text))
      return Promise.resolve(matches.length ? matches : [{ name: text || '当前位置', address: text || '当前位置', lng: nearLng || 121.479, lat: nearLat || 31.226 }])
    }
    return client.get(`/api/map/pois?${query({ keyword, nearLng, nearLat })}`)
  },
  route(points) {
    if (isMockMode()) {
      return Promise.resolve({
        distanceKm: 7.2,
        durationMinutes: 18,
        polyline: [
          [points.startLng, points.startLat],
          [points.endLng, points.endLat]
        ]
      })
    }
    return client.get(`/api/map/route?${query(points)}`)
  },
  estimate(points) {
    if (isMockMode()) {
      const passengerCount = Number(points.passengerCount || 1)
      return Promise.resolve({
        distanceKm: 7.2,
        durationMinutes: 18,
        total: Number((16 + passengerCount * 2.6).toFixed(1))
      })
    }
    return client.post('/api/map/price-estimate', points)
  }
}
