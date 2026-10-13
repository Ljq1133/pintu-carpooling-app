const STORAGE_KEY = 'pintu.auth.session'

export function getSession() {
  if (typeof localStorage === 'undefined') return null
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

export function setSession(data) {
  const session = {
    token: data.token,
    refreshToken: data.refreshToken,
    expiresIn: data.expiresIn,
    user: data.user
  }
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  }
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('pintu:session', { detail: session }))
  }
  return session
}

export function clearSession() {
  if (typeof localStorage !== 'undefined') localStorage.removeItem(STORAGE_KEY)
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('pintu:session', { detail: null }))
  }
}

export function isAuthenticated() {
  return Boolean(getSession()?.token)
}
