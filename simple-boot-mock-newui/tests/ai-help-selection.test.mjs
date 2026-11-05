import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { computed, effectScope, onScopeDispose, reactive, ref, watch } from 'vue'
import { createPinia, defineStore, setActivePinia, storeToRefs } from 'pinia'

const tick = () => new Promise(resolve => setImmediate(resolve))
const source = fs.readFileSync(new URL('../src/views/components/mock/MockAiHelp.vue', import.meta.url), 'utf8')
const script = path => fs.readFileSync(new URL(path, import.meta.url), 'utf8').replace(/^import .*$/gm, '')
const storeCode = script('../src/stores/AiHelpStore.js').replace('export const useAiHelpStore', 'const useAiHelpStore')
const modelsCode = script('../src/hooks/AiHooks.js').split('export const useAiConfigOption')[0].replace('export const useAiModels', 'const useAiModels')
const selectorCode = source.slice(source.indexOf('const conversation ='), source.indexOf('const answerViewport ='))
const resetCode = source.slice(source.indexOf('const resetComposer ='), source.indexOf('const ask ='))
const createSession = (options, selection = {}, generating = false) => {
  setActivePinia(createPinia())
  const calls = []
  const refreshes = []
  const context = {
    computed,
    defineStore,
    reactive,
    ref,
    AbortController,
    onScopeDispose,
    watch,
    $i18nBundle: key => key,
    ElMessage: { success: () => {} },
    loadAiModels: async config => {
      refreshes.push(config.id)
      return { success: true, resultData: [config.id + '-model'] }
    },
    loadAiConfigOptions: async () => ({ success: true, resultData: options }),
    AiConfigApi: { getById: async id => ({ success: true, resultData: { id, status: 1, defaultModel: 'model-' + id } }) },
    askAiHelp: async (...args) => {
      calls.push(args)
      args[1].onFinish({ resultData: { content: 'answer' } })
    }
  }
  vm.createContext(context)
  vm.runInContext(storeCode + ';globalThis.store = useAiHelpStore();' + modelsCode + ';globalThis.models = useAiModels', context)
  context.store.selection = selection
  context.store.loading = generating
  return { context, calls, refreshes }
}

const mountSelector = ({ context }) => {
  const scope = effectScope()
  const state = {
    computed,
    reactive,
    $i18nBundle: key => key,
    handleQuestionKeydown: () => {},
    watch,
    storeToRefs,
    nextTick: tick,
    scrollToLatest: () => {},
    props: reactive({ active: true }),
    useAiHelpStore: () => context.store,
    useAiConfigOption: () => ref({ attrs: {} }),
    useAiModels: context.models
  }
  vm.createContext(state)
  scope.run(() => vm.runInContext(selectorCode + resetCode + ';globalThis.reset = resetComposer;globalThis.ready = canAsk;globalThis.fields = options;globalThis.form = composerModel', state))
  return { state, dispose: () => scope.stop() }
}
const createSelector = (...args) => {
  const session = createSession(...args)
  const { state, dispose } = mountSelector(session)
  Object.assign(state, storeToRefs(session.context.store), {
    select: session.context.store.selectConfig,
    AiConfigApi: session.context.AiConfigApi
  })
  return { state, dispose }
}

test('selects enabled default, falls back without a default, and excludes deleted entries', async () => {
  for (const [options, expected] of [
    [[{ id: 2, status: 1 }, { id: 1, status: 1, isDefault: 1 }], 1],
    [[{ id: 3, status: 0, isDefault: 1 }, { id: 4, status: 1, deleted: true }, { id: 2, status: 1 }], 2]
  ]) {
    const { state, dispose } = createSelector(options)
    await tick()
    assert.equal(state.selection.value.configId, expected)
    assert.equal(state.selection.value.model, 'model-' + expected)
    assert.equal(state.ready.value, true)
    dispose()
  }
})

test('restores a temporary model and initializes after an in-progress answer finishes', async () => {
  const { state, dispose } = createSelector([{ id: 2, status: 1 }], { configId: 2, model: 'custom' }, true)
  await tick()
  assert.equal(state.ready.value, false)
  state.loading.value = false
  await tick()
  assert.equal(state.ready.value, true)
  assert.equal(state.selection.value.model, 'custom')
  dispose()
})

test('ignores stale configuration loads and blocks asking after a failed load', async () => {
  const { state, dispose } = createSelector([{ id: 1, status: 1 }])
  await tick()
  let finishOld
  state.AiConfigApi.getById = id => id === 2
    ? new Promise(resolve => { finishOld = resolve })
    : Promise.resolve({ success: true, resultData: { id, status: 1, defaultModel: 'new' } })
  const old = state.select(2)
  state.selection.value.configId = 3
  await state.select(3)
  finishOld({ success: true, resultData: { id: 2, status: 1, defaultModel: 'old' } })
  await old
  assert.equal(state.selection.value.model, 'new')
  assert.equal(state.ready.value, true)
  state.AiConfigApi.getById = async () => ({ success: false, message: 'unavailable' })
  await state.select(3)
  assert.equal(state.ready.value, false)
  assert.equal(state.error.value, 'unavailable')
  dispose()
})

test('sends selection with retained history; new conversation keeps selection and logout clears it', async () => {
  const { context: { store }, calls } = createSession([])
  store.selection = { configId: 1, model: 'first' }
  store.formData.question = 'question one'
  await store.ask()
  store.selection = { configId: 2, model: 'second' }
  store.formData.question = 'question two'
  await store.ask()
  assert.equal(calls[1][4].configId, 2)
  assert.equal(calls[1][4].model, 'second')
  assert.equal(calls[1][3][0].question, 'question one')
  store.formData.question = '  new draft  '
  store.clearConversation()
  assert.equal(store.formData.question, '  new draft  ')
  assert.equal(store.formData.answer, '')
  assert.equal(store.submittedQuestion, '')
  assert.equal(store.previousTurns.length, 0)
  assert.equal(store.history.length, 0)
  await store.ask()
  assert.equal(calls[2][0], 'new draft')
  assert.equal(calls[2][3].length, 0)
  assert.equal(store.selection.model, 'second')
  store.formData.question = 'private draft'
  store.clear()
  assert.equal(store.formData.question, '')
  assert.equal(store.selection.configId, undefined)
  assert.equal(store.selection.model, '')
})

test('page keeps the dialog selection and refreshes that model list after the dialog closes', async () => {
  const session = createSession([{ id: 1, status: 1 }, { id: 2, status: 1 }])
  const { store } = session.context
  const page = mountSelector(session)
  const dialog = mountSelector(session)
  await tick()
  assert.equal(store.selectedConfig.id, 1)

  let resolveDetail
  session.context.AiConfigApi.getById = () => new Promise(resolve => { resolveDetail = resolve })
  store.selection.configId = 2
  const pending = dialog.state.fields.value[0].change(2)
  assert.equal(page.state.ready.value, false)
  dialog.dispose()
  resolveDetail({ success: true, resultData: { id: 2, status: 1, defaultModel: 'model-2' } })
  await pending

  assert.equal(page.state.ready.value, true)
  await page.state.fields.value[1].tooltipFunc()
  assert.deepEqual(session.refreshes, [2])
  page.state.form.formData.question = 'follow up'
  await store.ask()
  assert.equal(session.calls[0][4].configId, 2)
  assert.equal(session.calls[0][4].model, 'model-2')
  page.dispose()
})

test('new conversation cancels a stream without restoring its question or accepting late chunks', async () => {
  const { context } = createSession([])
  let callbacks, signal, finish
  context.askAiHelp = (_question, events, requestSignal) => {
    callbacks = events
    signal = requestSignal
    return new Promise(resolve => { finish = resolve })
  }
  const { store } = context
  store.formData.question = 'sent'
  const pending = store.ask()
  callbacks.onDelta({ text: 'partial' })
  store.clearConversation()
  assert.equal(signal.aborted, true)
  callbacks.onDelta({ text: 'late' })
  callbacks.onFinish({ resultData: { content: 'late answer' } })
  finish()
  await pending
  assert.equal(store.formData.question, '')
  assert.equal(store.formData.answer, '')
  assert.equal(store.history.length, 0)
})

for (const outcome of ['failed', 'stopped']) {
  test(outcome + ' first question resets the conversation before clearing the restored draft', async () => {
    const session = createSession([{ id: 1, status: 1 }])
    const page = mountSelector(session)
    await tick()
    const { store } = session.context
    session.context.askAiHelp = async (_question, events) => {
      if (outcome === 'failed') events.onError({ message: 'failed' })
      else store.stop()
    }
    store.formData.question = 'retry me'
    await store.ask()
    assert.equal(store.history.length, 0)
    assert.equal(store.submittedQuestion, 'retry me')
    page.state.reset()
    assert.equal(store.submittedQuestion, '')
    assert.equal(store.formData.question, 'retry me')
    page.state.reset()
    assert.equal(store.formData.question, '')
    assert.equal(store.selection.configId, 1)
    assert.equal(store.selection.model, 'model-1')
    page.dispose()
  })
}
