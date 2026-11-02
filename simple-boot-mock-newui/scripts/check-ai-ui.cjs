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
  let markdownConfig
  let markdownTheme
  const markdownSource = read('vendors/markdown.js').replace(/^import .*$/gm, '').replace(/export const /g, 'const ')
  const markdown = new Function('config', 'mermaid', 'katex', 'hljs', markdownSource + '; return { mermaidFencePlugin, initMarkdownTheme, handleMarkdownLink, MD_AUTO_FOLD_THRESHOLD }')(
    value => { markdownConfig = value }, { initialize: value => { markdownTheme = value } }, {}, {}
  )
  assert.equal(markdown.MD_AUTO_FOLD_THRESHOLD, 200)
  markdown.initMarkdownTheme(true)
  assert.equal(markdownTheme.theme, 'dark')
  assert.equal(markdownTheme.securityLevel, 'strict')
  assert.equal(markdownConfig.mermaidConfig({ theme: 'dark' }).securityLevel, 'strict')
  for (const [markup, lines, expected] of [['~~~', 3, 'true'], ['~~~', 2, 'false'], ['```', 3, 'original']]) {
    let closed = 'original'
    const md = { renderer: { renderAttrs: () => closed } }
    markdown.mermaidFencePlugin(md)
    assert.equal(md.renderer.renderAttrs({
      type: 'fence',
      info: 'mermaid',
      markup,
      map: [0, lines],
      content: 'graph TD; A-->B\n',
      attrGet: () => closed,
      attrSet: (name, value) => { closed = value }
    }), expected)
  }
  let scrolled = false
  markdown.handleMarkdownLink({
    target: { closest: () => ({ getAttribute: () => '#%E6%B5%8B%E8%AF%95' }) },
    currentTarget: { querySelectorAll: () => [{ id: '测试', scrollIntoView: () => { scrolled = true } }] },
    preventDefault: () => {}
  })
  assert.equal(scrolled, true)
  console.log('Markdown configuration, incomplete fences, scoped anchors and theme checks passed')
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
    ElSpace: 'span',
    AiConfigLabel: { name: 'AiConfigLabel' }
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
  const jsonWindows = []
  const recordProps = ref({
    record: { id: 2, configName: 'Historical name', durationMs: 0, promptTokens: 0, totalTokens: 0 },
    config: { configName: 'Renamed config', deleted: true }
  })
  const detail = setup('views/admin/ai/AiLogDetailWindow.vue', {
    ref,
    computed,
    h,
    watch,
    defineModel: () => ref(true),
    defineProps: () => recordProps.value,
    formatDate: value => value,
    $i18nBundle: key => key,
    showCodeWindow: (...args) => jsonWindows.push(args)
  }, 'activeTab,metrics,configIdentity,viewJson')
  assert.equal(detail.configIdentity.value.configName, 'Historical name')
  assert.equal(detail.configIdentity.value.deleted, true)
  assert.equal(detail.metrics.value.find(field => field.labelKey === 'mock.label.aiDuration').value, '0')
  assert.equal(detail.metrics.value.find(field => field.labelKey === 'mock.label.aiCompletionTokens').value, '-')
  detail.viewJson()
  assert.equal(jsonWindows[0][0], recordProps.value.record)
  assert.equal(jsonWindows[0][1].readOnly, true)
  detail.activeTab.value = 'metrics'
  recordProps.value.record = { id: 3, configName: 'Next record' }
  await Promise.resolve()
  assert.equal(detail.activeTab.value, 'result')
  const answerProps = ref({ content: '{"id":9007199254740993123}', maxHeight: 240 })
  const answer = setup('views/components/utils/AiAnswerViewer.vue', {
    ref,
    watch: () => {},
    initMarkdownTheme: () => {},
    computed,
    defineProps: () => answerProps.value,
    useGlobalConfigStore: () => ({ monacoTheme: 'vs-dark' }),
    $i18nBundle: key => key
  }, 'isJson,sourceOptions,sourceHeight,modeOptions,previewContent')
  assert.equal(answer.isJson.value, true)
  answerProps.value.loading = true
  assert.equal(answer.isJson.value, false)
  answerProps.value.loading = false
  assert.equal(answer.sourceHeight.value, '160px')
  assert.equal(answerProps.value.content, '{"id":9007199254740993123}')
  assert.equal(answer.previewContent.value, '```json\n{"id":9007199254740993123}\n```')
  assert.equal(answer.sourceOptions.readOnly, true)
  assert.equal(answer.sourceOptions.domReadOnly, true)
  answerProps.value.content = '# Markdown\n'.repeat(30)
  assert.equal(answer.isJson.value, false)
  assert.equal(answer.sourceHeight.value, '240px')
  assert.deepEqual(answer.modeOptions.value.map(option => option.value), ['preview', 'source'])
  const rawResponse = '{"usage":{"total_tokens":2238},"id":9007199254740993123}'
  const responseAction = setup('views/components/utils/AiResponseJsonAction.vue', {
    defineProps: () => ({}),
    $i18nBundle: key => key,
    showCodeWindow: (...args) => jsonWindows.push(args)
  }, 'view')
  responseAction.view(rawResponse)
  assert.equal(jsonWindows.at(-1)[0], rawResponse)
  assert.equal(jsonWindows.at(-1)[1].language, 'json')
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
  let mountTest
  const configRequests = []
  const testCalls = []
  const switchable = setup('views/admin/ai/AiConfigTestWindow.vue', {
    h,
    AiConfigLabel: {},
    ref,
    computed,
    watch,
    onMounted: callback => { mountTest = callback },
    onScopeDispose: () => {},
    defineModel: () => ref(true),
    defineProps: () => ({ config: {} }),
    AI_PROVIDERS: providers,
    useAiModels: () => ({ modelOption: ref({}) }),
    $i18nBundle: key => key,
    loadAiConfigOptions: async () => ({
      success: true,
      resultData: [
        { id: 1, status: 0, isDefault: 1 },
        { id: 2, status: 1 },
        { id: 3, status: 1, isDefault: 1 },
        { id: 4, status: 1, deleted: true }
      ]
    }),
    AiConfigApi: { getById: id => new Promise(resolve => configRequests.push({ id, resolve })) },
    testAiConfig: async (id, data) => { testCalls.push({ id, data }); return { success: true, resultData: { content: 'ok' } } }
  }, 'configs,formData,formOptions,selectConfig,selectedConfig,executeTest,visible')
  const mounted = mountTest()
  await Promise.resolve()
  assert.deepEqual(switchable.configs.value.map(config => config.id), [2, 3])
  assert.equal(switchable.formData.value.configId, 3)
  configRequests[0].resolve({ success: true, resultData: { id: 3, defaultModel: 'default-3' } })
  await mounted
  assert.equal(switchable.formData.value.model, 'default-3')
  const configSelector = switchable.formOptions.value[0]
  assert.equal(configSelector.slots.label().props.config.id, 3)
  assert.equal(configSelector.children[0].slots.default().props.config.id, 2)
  const pendingTest = switchable.executeTest()
  assert.ok(switchable.formOptions.value.slice(0, 3).every(option => option.disabled))
  await pendingTest
  testCalls.length = 0
  const oldSelection = switchable.selectConfig(2)
  const newSelection = switchable.selectConfig(3)
  configRequests[2].resolve({ success: true, resultData: { id: 3, defaultModel: 'new-3' } })
  await newSelection
  configRequests[1].resolve({ success: true, resultData: { id: 2, defaultModel: 'stale-2' } })
  await oldSelection
  assert.equal(switchable.formData.value.model, 'new-3')
  switchable.formData.value.model = 'custom-model'
  await switchable.executeTest()
  assert.equal(testCalls[0].id, 3)
  assert.equal(testCalls[0].data.model, 'custom-model')
  assert.equal(switchable.selectedConfig.value.defaultModel, 'new-3')
  const closingSelection = switchable.selectConfig(2)
  switchable.visible.value = false
  configRequests[3].resolve({ success: true, resultData: { id: 2, defaultModel: 'closed' } })
  await closingSelection
  assert.equal(switchable.formData.value.model, '')
  const validations = []
  let calls = 0
  const test = setup('views/admin/ai/AiConfigTestWindow.vue', {
    onMounted: () => {},
    ref,
    computed,
    watch,
    onScopeDispose: () => {},
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
  const responses = []
  const signals = []
  const cancellation = setup('views/admin/ai/AiConfigTestWindow.vue', {
    onMounted: () => {},
    ref,
    computed,
    watch,
    onScopeDispose: () => {},
    defineModel: () => ref(true),
    defineProps: () => ({ config: { id: 7, defaultModel: 'm' } }),
    useAiModels: () => ({ modelOption: ref({}) }),
    $i18nBundle: key => key,
    testAiConfig: (id, data, callbacks, signal) => {
      signals.push(signal)
      return new Promise(resolve => responses.push(response => {
        if (response.success) callbacks.onFinish(response)
        else callbacks.onError(response)
        resolve()
      }))
    }
  }, 'executeTest,stopTest,visible,formData,testing,metrics,metricFields,error')
  const firstTest = cancellation.executeTest()
  cancellation.stopTest()
  assert.equal(signals[0].aborted, true)
  const nextTest = cancellation.executeTest()
  responses[0]({ success: false, message: 'stale failure' })
  await firstTest
  assert.equal(cancellation.testing.value, true)
  assert.equal(cancellation.error.value, '')
  responses[1]({ success: true, resultData: { content: 'latest', durationMs: 0, totalTokens: 0 } })
  await nextTest
  assert.equal(cancellation.formData.value.result, 'latest')
  assert.equal(cancellation.metricFields.value.length, 2)
  assert.equal(cancellation.testing.value, false)
  const closedTest = cancellation.executeTest()
  cancellation.visible.value = false
  assert.equal(signals[2].aborted, true)
  responses[2]({ success: true, resultData: { content: 'closed' } })
  await closedTest
  assert.equal(cancellation.formData.value.result, '')
  assert.equal(cancellation.metrics.value, null)
  assert.ok(test.formData.value.prompt.trim())
  test.runTest({ form })
  test.visible.value = false
  validations[1](true)
  await Promise.resolve()
  assert.equal(calls, 1)
  const helpRequests = []
  const helpProps = ref({ active: true })
  let deactivateHelp
  let resizeAnswer
  const helpScope = effectScope()
  const help = helpScope.run(() => setup('views/components/mock/MockAiHelp.vue', {
    ref,
    computed,
    onScopeDispose,
    onDeactivated: callback => { deactivateHelp = callback },
    watch,
    defineProps: () => helpProps.value,
    defineExpose: () => {},
    nextTick: callback => Promise.resolve().then(callback),
    useResizeObserver: (target, callback) => { resizeAnswer = callback },
    $i18nBundle: key => key,
    askAiHelp: (prompt, callbacks, signal) => new Promise((resolve, reject) => {
      helpRequests.push({ prompt, callbacks, signal, resolve, reject })
    })
  }, 'ask,stop,formData,loading,error,stopped,metricFields,answerViewport,following,trackScroll,scrollToLatest,handleQuestionKeydown,submittedQuestion,prefillQuestion'))
  help.answerViewport.value = { scrollHeight: 1000, scrollTop: 0, clientHeight: 300 }
  resizeAnswer()
  assert.equal(help.answerViewport.value.scrollTop, 1000)
  help.answerViewport.value.scrollTop = 100
  help.trackScroll()
  resizeAnswer()
  assert.equal(help.answerViewport.value.scrollTop, 100)
  assert.equal(help.following.value, false)
  help.scrollToLatest()
  assert.equal(help.following.value, true)
  assert.equal(help.answerViewport.value.scrollTop, 1000)
  await help.prefillQuestion('Explain matching rules')
  assert.equal(help.formData.value.question, 'Explain matching rules')
  assert.equal(helpRequests.length, 0)
  await help.prefillQuestion('Explain matching rules')
  assert.equal(helpRequests.length, 0)
  help.formData.value.question = '  help  '
  for (const event of [{ key: 'Enter', shiftKey: true }, { key: 'Enter', isComposing: true }, { key: 'Enter', keyCode: 229 }, { key: 'Enter', ctrlKey: true }]) {
    help.handleQuestionKeydown({ ...event, preventDefault: () => assert.fail('Must preserve newline or IME input') })
  }
  assert.equal(helpRequests.length, 0)
  let prevented = false
  const firstHelp = help.handleQuestionKeydown({ key: 'Enter', preventDefault: () => { prevented = true } })
  assert.equal(prevented, true)
  assert.equal(helpRequests.length, 1)

  help.ask()
  assert.equal(helpRequests.length, 1)
  assert.equal(helpRequests[0].prompt, 'help')
  assert.equal(help.formData.value.question, '')
  assert.equal(help.submittedQuestion.value, 'help')
  helpRequests[0].callbacks.onDelta({ text: 'partial' })
  assert.equal(help.formData.value.answer, 'partial')
  help.stop()
  assert.equal(helpRequests[0].signal.aborted, true)
  assert.equal(help.formData.value.answer, 'partial')
  assert.equal(help.stopped.value, true)
  assert.equal(help.formData.value.question, 'help')
  const secondHelp = help.ask()
  helpRequests[0].callbacks.onDelta({ text: 'stale' })
  helpRequests[0].callbacks.onFinish({ resultData: { content: 'stale' } })
  helpRequests[0].callbacks.onError({ message: 'stale' })
  helpRequests[0].reject(new Error('stale'))
  await firstHelp
  assert.equal(help.formData.value.answer, '')
  assert.equal(help.error.value, '')
  assert.equal(help.loading.value, true)
  helpRequests[1].callbacks.onDelta({ text: 'new' })
  helpRequests[1].callbacks.onFinish({ resultData: { content: 'complete', durationMs: 0, totalTokens: 0 } })
  helpRequests[1].resolve()
  await secondHelp
  assert.equal(help.formData.value.answer, 'complete')
  assert.equal(help.metricFields.value.length, 2)
  assert.equal(help.loading.value, false)
  assert.equal(help.formData.value.question, '')
  help.formData.value.question = 'retry question'
  const failedHelp = help.ask()
  helpRequests[2].callbacks.onDelta({ text: 'incomplete' })
  helpRequests[2].reject(new Error('interrupted'))
  await failedHelp
  assert.equal(help.formData.value.answer, 'incomplete')
  assert.equal(help.error.value, 'interrupted')
  assert.equal(help.formData.value.question, 'retry question')
  const hiddenHelp = help.ask()
  helpRequests[3].callbacks.onDelta({ text: 'hidden partial' })
  helpProps.value.active = false
  assert.equal(helpRequests[3].signal.aborted, true)
  assert.equal(help.formData.value.answer, 'hidden partial')
  helpRequests[3].resolve()
  await hiddenHelp
  await help.ask()
  assert.equal(helpRequests.length, 4)
  helpProps.value.active = true
  const deactivatedHelp = help.ask()
  deactivateHelp()
  assert.equal(helpRequests[4].signal.aborted, true)
  helpRequests[4].resolve()
  await deactivatedHelp
  const serverErrorHelp = help.ask()
  helpRequests[5].callbacks.onError({ message: 'provider failed' })
  helpRequests[5].resolve()
  await serverErrorHelp
  assert.equal(help.formData.value.question, 'retry question')
  assert.equal(help.error.value, 'provider failed')
  const disposedHelp = help.ask()
  helpScope.stop()
  assert.equal(helpRequests[6].signal.aborted, true)
  helpRequests[6].resolve()
  await disposedHelp
  console.log('AI help streaming, cancellation, stale callbacks, metrics, failure and disposal checks passed')
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
