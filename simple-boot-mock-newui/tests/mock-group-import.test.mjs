import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'

// 执行组件本身的提交函数，覆盖 common-window 的同步关闭契约。
const source = fs.readFileSync(new URL('../src/views/components/mock/MockGroupImport.vue', import.meta.url), 'utf8')
const submitCode = source.slice(source.indexOf('const executeUpload ='), source.indexOf('</script>'))
const tick = () => new Promise(resolve => setImmediate(resolve))

const createDialog = (files = []) => {
  const state = {
    importFiles: { value: files },
    importModel: { value: { type: 'simple' } },
    showWindow: { value: true },
    detectedType: { value: null },
    maxFileSize: { value: 256 },
    maxRequestSize: { value: 512 },
    configPromise: Promise.resolve(),
    refreshImportConfig: () => Promise.resolve(),
    isSupportedFile: () => true,
    isOversizedFile: file => file.size > 256,
    getFileName: file => file.name,
    formatFileSize: String,
    formatLimit: String,
    getFormatLabel: String,
    SUPPORTED_IMPORT_FILE_EXTS: ['.json'],
    $i18nBundle: key => key,
    $coreError: () => {},
    $coreAlert: () => {},
    // Element Plus 区分 close / cancel，实际接口使用字符串拒绝原因。
    // eslint-disable-next-line prefer-promise-reject-errors
    $coreConfirm: () => Promise.reject('close'),
    uploadFiles: () => Promise.resolve({ success: true, resultData: 1 }),
    emit: () => {}
  }
  vm.createContext(state)
  vm.runInContext(submitCode + ';globalThis.submit = doImportGroups', state)
  state.clickOk = () => {
    // 与 common-window.onOkClick 相同的同步返回值判断。
    if (state.submit() !== false) state.showWindow.value = false
  }
  return state
}

test('empty files and oversized files keep the dialog open', async () => {
  for (const files of [[], [{ name: 'large.json', size: 257 }]]) {
    const state = createDialog(files)
    state.clickOk()
    await tick()
    assert.equal(state.showWindow.value, true)
  }
})

test('dismissing format confirmation keeps the dialog open without uploading', async () => {
  const state = createDialog([{ name: 'sample.json', size: 20 }])
  state.detectedType.value = 'postman'
  let uploads = 0
  state.uploadFiles = () => { uploads++; return Promise.resolve({ success: true }) }
  state.clickOk()
  await tick()
  assert.equal(uploads, 0)
  assert.equal(state.showWindow.value, true)
})

test('failed upload keeps dialog open and successful upload closes it', async () => {
  const state = createDialog([{ name: 'sample.json', size: 20 }])
  state.uploadFiles = () => Promise.resolve({ success: false })
  state.clickOk()
  await tick()
  assert.equal(state.showWindow.value, true)
  state.uploadFiles = () => Promise.resolve({ success: true, resultData: 1 })
  state.clickOk()
  assert.equal(state.showWindow.value, true)
  await tick()
  assert.equal(state.showWindow.value, false)
})
