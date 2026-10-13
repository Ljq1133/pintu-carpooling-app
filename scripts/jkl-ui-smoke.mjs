import { spawn } from 'node:child_process'
import { setTimeout as delay } from 'node:timers/promises'
import { fileURLToPath } from 'node:url'

const port = Number(process.env.JKL_UI_SMOKE_PORT || 5187)
const baseUrl = `http://127.0.0.1:${port}`
const projectRoot = fileURLToPath(new URL('../', import.meta.url))
const viteBin = fileURLToPath(new URL('../node_modules/vite/bin/vite.js', import.meta.url))

const routes = [
  '/share/mock-token?api=mock',
  '/admin/login?api=mock',
  '/app/passenger/profile/safety?api=mock',
  '/app/admin?api=mock'
]

const componentModules = [
  '/src/views/TripShareView.vue',
  '/src/components/Passenger/Passenger.vue',
  '/src/components/c-workspace/DriverWorkspace.vue',
  '/src/components/c-workspace/AdminWorkspace.vue'
]

async function waitForServer(processRef) {
  for (let attempt = 0; attempt < 40; attempt += 1) {
    if (processRef.exitCode != null) break
    try {
      const response = await fetch(baseUrl)
      if (response.ok) return
    } catch {
      // Server is still booting.
    }
    await delay(250)
  }
  throw new Error(`Vite dev server did not become ready at ${baseUrl}`)
}

async function assertRoute(route) {
  const response = await fetch(`${baseUrl}${route}`)
  if (!response.ok) throw new Error(`${route} returned ${response.status}`)
  const html = await response.text()
  if (!html.includes('<div id="app">')) {
    throw new Error(`${route} did not return the Vue app shell`)
  }
}

async function assertCompiledModule(modulePath) {
  const response = await fetch(`${baseUrl}${modulePath}`)
  if (!response.ok) throw new Error(`${modulePath} failed Vue compilation: ${response.status}`)
  if (!response.headers.get('content-type')?.includes('javascript')) {
    throw new Error(`${modulePath} did not return a compiled JavaScript module`)
  }
}

const server = spawn(process.execPath, [viteBin, '--host', '127.0.0.1', '--port', String(port)], {
  cwd: projectRoot,
  stdio: ['ignore', 'pipe', 'pipe'],
  windowsHide: true
})

let stderr = ''
server.stderr.on('data', (chunk) => {
  stderr += chunk.toString()
})

try {
  await waitForServer(server)
  await Promise.all(routes.map(assertRoute))
  await Promise.all(componentModules.map(assertCompiledModule))
  console.log(`JKL UI smoke passed: ${routes.length} route shells and ${componentModules.length} compiled Vue modules`)
} finally {
  server.kill()
  await delay(100)
}

if (server.exitCode && server.exitCode !== 0 && stderr) {
  console.warn(stderr.trim())
}
