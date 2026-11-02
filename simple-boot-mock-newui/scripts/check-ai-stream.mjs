import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
const source = fs.readFileSync(new URL('../src/api/mock/AiStreamApi.js', import.meta.url), 'utf8')
  .replace(/^import .*$/gm, '')
  .replace('export const streamAiRequest', 'globalThis.streamAiRequest')
  .replaceAll('import.meta.env.VITE_APP_API_BASE_URL', "'/api'")

async function run (payload, callbacks = {}, signal) {
  let cancelled = false
  const context = {
    TextDecoder,
    DOMException,
    useLoginConfigStore: () => ({ accessToken: 'test-token' }),
    useGlobalConfigStore: () => ({ currentLocale: 'en-US' }),
    $i18nBundle: key => key,
    fetch: async (url, options) => {
      assert.equal(url, '/api/test')
      assert.equal(options.headers.Authorization, 'Bearer test-token')
      assert.equal(options.headers.locale, 'en-US')
      const bytes = new TextEncoder().encode(payload)
      let index = 0
      return {
        ok: true,
        headers: new Map(),
        body: {
          getReader: () => ({
            read: async () => index < bytes.length ? { value: bytes.slice(index, ++index), done: false } : { done: true },
            cancel: async () => { cancelled = true },
            releaseLock: () => {}
          })
        }
      }
    }
  }
  vm.runInNewContext(source, context)
  try { await context.streamAiRequest('/test', {}, callbacks, signal) } finally { assert.equal(cancelled, true) }
}

async function main () {
  let text = ''
  let finished = false
  await run('event:delta\r\ndata:{"text":"你好😀"}\r\n\r\nevent:finish\ndata:{"status":"SUCCESS"}\n\n', {
    onDelta: data => { text += data.text }, onFinish: () => { finished = true }
  })
  assert.equal(text, '你好😀')
  assert.equal(finished, true)
  for (const payload of ['event:finish\ndata:{"status":"SUCCESS"}', 'event:finish\ndata:{"status":"FAILED"}\n\n', 'event:finish\ndata:{"status":"INCOMPLETE"}\n\n', 'data:broken\n\n']) {
    await assert.rejects(run(payload), /aiStreamInterrupted/)
  }
  let error = ''
  await run('event:error\ndata:{"message":"failed"}\n\n', { onError: data => { error = data.message } })
  assert.equal(error, 'failed')
  await assert.rejects(run('data:{}\n\n', {}, { aborted: true }), { name: 'AbortError' })
  console.log('AI stream framing, UTF-8, interruption, error and cancellation checks passed')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
