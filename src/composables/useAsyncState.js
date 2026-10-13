import { ref, shallowRef } from 'vue'

/**
 * 异步操作状态封装。
 * <p>
 * 用法：
 * </p>
 * <pre>
 *   const orders = useAsyncState([])
 *   await orders.run(() => api.listOrders(passengerId))
 *   // orders.loading / orders.error / orders.data / orders.lastUpdated
 * </pre>
 *
 * 同时阻止重复点击：第 N+1 次 run 在第 N 次未完成时直接返回当前 promise。
 *
 * @param {*} initialData 初始数据
 * @returns {{ data, loading, error, lastUpdated, run, reset }}
 */
export function useAsyncState(initialData = null) {
  const data = shallowRef(initialData)
  const loading = ref(false)
  const error = ref(null)
  const lastUpdated = ref(null)
  let inflight = null

  async function run(fetcher) {
    if (typeof fetcher !== 'function') {
      throw new TypeError('useAsyncState.run expects a function')
    }
    if (inflight) return inflight

    loading.value = true
    error.value = null
    inflight = (async () => {
      try {
        const value = await fetcher()
        data.value = value
        lastUpdated.value = new Date()
        return value
      } catch (err) {
        error.value = err
        throw err
      } finally {
        loading.value = false
        inflight = null
      }
    })()
    return inflight
  }

  function reset(value = initialData) {
    data.value = value
    error.value = null
    loading.value = false
    lastUpdated.value = null
  }

  return { data, loading, error, lastUpdated, run, reset }
}
