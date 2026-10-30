/* eslint-disable @typescript-eslint/no-require-imports, no-new-func -- Standalone regression executes trusted local SFC source without a browser or new dependencies. */
// Executes the production SFC setup and shared option resolver without a browser.
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { ref, computed, h, watch, effectScope, onScopeDispose } = require('vue')
const { transformSync } = require('@babel/core')
let jsxPlugin
const root = path.resolve(__dirname, '..')
const read = file => fs.readFileSync(path.join(root, 'src', file), 'utf8')
const setup = (file, bindings, result) => {
  const script = read(file).match(/<script setup([^>]*)>([\s\S]*?)<\/script>/)
  let source = script[2].replace(/^import .*$/gm, '')
  if (script[1].includes('jsx')) {
    source = transformSync(script[2], { configFile: false, babelrc: false, plugins: [jsxPlugin] }).code
      .replace(/import \{([^}]+)\} from ['"]vue['"];/g, (_, imports) => {
        const helpers = imports.split(',').filter(item => item.includes(' as _')).join(',').replace(/\s+as\s+/g, ': ')
        return helpers ? `const {${helpers}} = require('vue')` : ''
      })
      .replace(/^import .*$/gm, '')
  }
  return new Function('require', ...Object.keys(bindings), source + '\nreturn {' + result + '}')(require, ...Object.values(bindings))
}
const providers = new Function(read('consts/AiConstants.js').replace('export const', 'const') + '; return AI_PROVIDERS')()
const childTypeMapping = new Function('return ' + read('components/common-form-control/index.vue').match(/const childTypeMapping = (\{[\s\S]*?\n\})/)[1])()
const shared = read('components/common-form-control/index.vue').match(/const children = computed\(\(\) => \{([\s\S]*?)\n\}\)/)[1]
const inputType = new Function('option', read('components/utils/index.js').match(/export const useInputType = \(option\) => \{([\s\S]*?)\n\}/)[1])
const assertOptions = options => {
  for (const option of options.filter(item => item.type === 'select')) {
    const children = new Function('calcOption', 'childTypeMapping', shared)({ value: option }, childTypeMapping)
    assert.ok(children.length > 0)
    assert.ok(children.every(child => inputType(child) === 'el-option'))
  }
}
async function checkDelFlagTag () {
  const vue = require('vue')
  const { compile } = require('@vue/compiler-dom')
  const { renderToString } = require('@vue/server-renderer')
  const file = 'views/components/utils/DelFlagTag.vue'
  const template = read(file).match(/<template>([\s\S]*?)<\/template>/)[1]
  const render = new Function('Vue', compile(template, { mode: 'function', prefixIdentifiers: true }).code)(vue)
  for (const value of [0, 1, '0', '1']) {
    for (const custom of [false, true]) {
      const events = []
      const confirmations = []
      let cancel = false
      const state = setup(file, {
        computed,
        defineProps: definitions => ({
          ...Object.fromEntries(Object.entries(definitions).map(([key, option]) => [key, option.default])),
          clickToToggle: true,
          ...(custom ? { typeConfig: { 1: 'primary', 0: 'info' }, valueConfig: { 1: 'Set default', 0: 'Unset default' } } : {})
        }),
        defineModel: () => ref(value),
        defineEmits: () => (...args) => { events.push(args) },
        $i18nBundle: (key, args) => args?.[0] || key,
        $i18nKey: (key, text) => text,
        $coreConfirm: async text => { confirmations.push(text); if (cancel) throw new Error('cancel') }
      }, 'props,modelValue,typeConf,valueConf,reversedValue,tooltip,handleClick')
      const target = Number(value) === 1 ? '0' : '1'
      const action = custom ? (target === '1' ? 'Set default' : 'Unset default') : `common.label.status${target === '1' ? 'Enabled' : 'Disabled'}`
      assert.equal(state.tooltip.value.content, action)
      await state.handleClick()
      assert.deepEqual(events, [['toggleValue', target]])
      assert.equal(confirmations[0], action)
      assert.equal(state.modelValue.value, value)
      cancel = true
      await assert.rejects(state.handleClick(), /cancel/)
      assert.equal(events.length, 1)
      state.props.clickToToggle = false
      await state.handleClick()
      assert.equal(events.length, 1)
      const app = vue.createSSRApp({ setup: () => ({ ...state, ...state.props }), render })
      app.component('ElLink', (_, { slots }) => h('div', slots.default()))
      app.component('ElTag', (_, { slots }) => h('span', slots.default()))
      app.component('ElSwitch', (_, { attrs }) => h('input', { ...attrs, 'data-model': attrs.modelValue }))
      app.directive('common-tooltip', {})
      const html = await renderToString(app)
      assert.ok(html.includes(`--el-switch-on-color:var(--el-color-${custom ? 'primary' : 'success'})`))
      assert.ok(html.includes(`--el-switch-off-color:var(--el-color-${custom ? 'info' : 'danger'})`))
      assert.ok(html.includes('disabled'))
    }
  }
  console.log('DelFlagTag regression passed: default/custom colors, numeric/string values, confirmation, cancellation, read-only rendering')
}
async function main () {
  jsxPlugin = (await import('@vue/babel-plugin-jsx')).default
  await checkDelFlagTag()
  const route = { name: 'AiLogs', query: { configId: '7' } }
  let adapter
  let request
  const searches = []
  const watches = []
  const api = { search: query => { request = query; return Promise.resolve({ success: true, resultData: [] }) }, deleteById: async () => ({ success: true }) }
  const bindings = {
    ref,
    computed,
    h,
    onMounted: () => {},
    onActivated: () => {},
    watch: (source, callback) => watches.push(callback),
    useRoute: () => route,
    useRouter: () => ({ replace: async ({ query }) => { route.query = query } }),
    useInitLoadOnce: callback => ({ initLoadOnce: callback }),
    useTableAndSearchForm: options => {
      adapter = options.searchMethod
      return { tableData: ref([{ id: 1 }]), loading: ref(false), searchParam: ref(options.defaultParam), searchMethod: async page => searches.push(page) }
    },
    AiLogApi: api,
    loadAiConfigOptions: async () => ({ success: true, resultData: [] }),
    $i18nBundle: key => key,
    $coreConfirm: async () => {},
    useDefaultPage: () => ({ pageNumber: 3 }),
    AI_PROVIDERS: providers,
    ElTag: 'span',
    ElSpace: 'span'
  }
  const page = setup('views/admin/ai/AiLogs.vue', bindings, 'options,configs,syncRouteConfig,searchParam,changeConfig,remove,view,detailRecord')
  page.syncRouteConfig()
  assert.equal(page.searchParam.value.configId, 7)
  page.configs.value = [{ id: 7, configName: 'Configured', provider: 'openai', status: 0, isDefault: 1 }, { id: 8, configName: 'Historical', deleted: true }]
  assertOptions(page.options.value)
  assert.equal(page.options.value.find(option => option.prop === 'configId').children.length, 2)
  await adapter({ status: 0, createDates: ['2026-10-01 00:00:00', '2026-10-30 23:59:59'] })
  assert.equal(request.status, 0)
  assert.equal(request.startDate, '2026-10-01 00:00:00')
  assert.equal(request.endDate, '2026-10-30 23:59:59')
  assert.ok(!('createDates' in request))
  await adapter({ status: '', createDates: null })
  assert.equal(request.status, undefined)
  assert.equal(request.startDate, undefined)
  page.searchParam.value.configId = ''
  await page.changeConfig()
  assert.equal(route.query.configId, undefined)
  await page.remove({ id: 1 })
  assert.equal(searches.at(-1), 2)
  route.name = 'AiConfigs'
  const before = searches.length
  watches[0]()
  assert.equal(searches.length, before)
  const pending = []
  api.search = () => new Promise(resolve => pending.push(resolve))
  const old = adapter({ keyword: 'old' })
  const latest = adapter({ keyword: 'latest' })
  pending[1]({ success: true }); pending[0]({ success: true })
  assert.equal((await latest).success, true)
  assert.equal((await old).success, false)
  // Execute the real test window handler with delayed validation.
  const details = []
  api.getById = () => new Promise(resolve => details.push(resolve))
  const viewA = page.view({ id: 1 })
  const viewB = page.view({ id: 2 })
  details[1]({ success: true, resultData: { id: 2 } })
  details[0]({ success: true, resultData: { id: 1 } })
  await Promise.all([viewA, viewB])
  assert.equal(page.detailRecord.value.id, 2)
  const edits = []
  const configPage = setup('views/admin/ai/AiConfigs.vue', {
    ...bindings,
    formatDate: value => value,
    DelFlagTag: { name: 'DelFlagTag' },
    AiConfigApi: { getById: () => new Promise(resolve => edits.push(resolve)) },
    useSearchStatus: () => ({ type: 'select', children: [{ label: 'Enabled', value: 1 }, { label: 'Disabled', value: 0 }] }),
    useAiModels: () => ({ modelOption: ref({ type: 'select', children: [{ label: 'm', value: 'm' }] }) })
  }, 'options,searchOptions,columns,edit,item')
  for (const prop of ['status', 'isDefault']) {
    const toggle = configPage.columns.value.find(column => column.prop === prop).formatter({ status: 1, isDefault: 0 })
    assert.equal(toggle.type.name, 'DelFlagTag')
    assert.equal(toggle.props.clickToToggle, true)
    assert.equal(typeof toggle.props.onToggleValue, 'function')
  }
  assertOptions(configPage.options.value)
  assertOptions(configPage.searchOptions.value)
  const editA = configPage.edit({ id: 1 })
  const editB = configPage.edit({ id: 2 })
  edits[1]({ success: true, resultData: { id: 2, apiKey: 'test-only-key' } })
  edits[0]({ success: true, resultData: { id: 1 } })
  await Promise.all([editA, editB])
  assert.equal(configPage.item.value.id, 2)
  assert.equal(configPage.item.value.apiKey, 'test-only-key')
  const oldEdit = configPage.edit({ id: 3 })
  await configPage.edit()
  edits[2]({ success: true, resultData: { id: 3 } })
  await oldEdit
  assert.equal(configPage.item.value.id, undefined)
  assert.equal(configPage.item.value.apiKey, '')
  const selectedConfig = page.options.value.find(option => option.prop === 'configId').children[0]
  assert.ok(selectedConfig.slots.default())
  const validations = []
  let calls = 0
  const test = setup('views/admin/ai/AiConfigTestWindow.vue', {
    ref,
    computed,
    defineModel: () => ref(true),
    defineProps: () => ({ config: { id: 7, defaultModel: 'm' } }),
    useAiModels: () => ({ modelOption: ref({}) }),
    $i18nBundle: key => key,
    testAiConfig: async () => { calls++; return { success: true, resultData: { content: 'ok' } } }
  }, 'runTest,visible,formData')
  const form = { validate: callback => validations.push(callback) }
  assert.equal(test.runTest({ form }), false)
  test.runTest({ form })
  assert.equal(validations.length, 1)
  validations[0](true)
  await Promise.resolve()
  assert.equal(calls, 1)
  assert.ok(test.formData.value.prompt.trim())
  test.runTest({ form })
  test.visible.value = false
  validations[1](true)
  await Promise.resolve()
  assert.equal(calls, 1)
  const modelRequests = []
  const messages = []
  const modelSource = read('hooks/AiHooks.js').replace(/^import .*$/gm, '').replace('export const', 'const')
  const useModels = new Function('ref', 'computed', 'watch', 'onScopeDispose', 'loadAiModels', 'ElMessage', '$i18nBundle', modelSource + ';return useAiModels')(ref, computed, watch, onScopeDispose, () => new Promise((resolve, reject) => modelRequests.push({ resolve, reject })), { error: message => messages.push(message) }, key => key)
  const scope = effectScope()
  const config = ref({ id: 1, provider: 'openai', baseUrl: 'url', apiKey: 'key' })
  const modelForm = ref({ model: 'manual' })
  const selector = scope.run(() => useModels(config, modelForm, 'model'))
  assertOptions([selector.modelOption.value])
  const first = selector.modelOption.value.tooltipFunc()
  config.value.provider = 'gemini'
  modelRequests[0].resolve({ success: true, resultData: ['outdated'] })
  await first
  assert.deepEqual(selector.modelOption.value.children.map(item => item.value), ['manual'])
  const second = selector.modelOption.value.tooltipFunc()
  config.value.baseUrl = 'other'
  modelRequests[1].reject(new Error('old failure'))
  await second
  assert.equal(messages.length, 0)
  const third = selector.modelOption.value.tooltipFunc()
  config.value.apiKey = 'new'
  modelRequests[2].resolve({ success: true, resultData: ['old-key-model'] })
  await third
  assert.equal(modelForm.value.model, 'manual')
  const final = selector.modelOption.value.tooltipFunc()
  scope.stop()
  modelRequests[3].reject(new Error('closed window'))
  await final
  assert.equal(messages.length, 0)
  console.log('AI UI regression passed: real option resolver, date/status filters, route clear, stale response, delete paging, validation lock')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
