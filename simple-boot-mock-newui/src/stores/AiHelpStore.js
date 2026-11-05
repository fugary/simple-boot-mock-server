import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import { AiConfigApi, askAiHelp, loadAiConfigOptions } from '@/api/mock/AiApi'
import { $i18nBundle } from '@/messages'

const createModelCache = () => {
  const entries = reactive(new Map())
  const connections = reactive(new Map())
  const epoch = ref(0)
  const ttl = 5 * 60 * 1000
  const connectionKey = config => JSON.stringify([
    config.provider?.trim().toLowerCase(), config.baseUrl?.trim().replace(/\/+$/, ''), config.apiKey?.trim() || ''
  ])
  const savedKey = config => JSON.stringify([config.id, config.modifyDate, config.provider, config.baseUrl])
  const key = (config, saved) => saved
    ? connections.get(savedKey(config)) || `saved:${savedKey(config)}`
    : config.id && !config.apiKey?.trim()
      ? connections.get(savedKey(config)) || `saved:${savedKey(config)}`
      : connectionKey(config)
  const put = (map, id, value) => {
    map.delete(id)
    map.set(id, value)
    if (map.size > 100) map.delete(map.keys().next().value)
  }
  const get = (config, saved = false) => {
    const entry = entries.get(key(config, saved))
    return entry && Date.now() - entry.time < ttl ? [...entry.models] : []
  }
  const set = (config, saved, models) => {
    put(entries, key(config, saved), { models: [...new Set(models.filter(model => typeof model === 'string' && model.trim()))], time: Date.now() })
  }
  // Only bind server-confirmed connections; unsaved edits must not alias the saved ID.
  const remember = config => {
    if (!config.id || !config.apiKey?.trim()) return
    const id = savedKey(config)
    const target = connectionKey(config)
    const existing = entries.get(`saved:${id}`)
    if (existing) {
      put(entries, target, existing)
      entries.delete(`saved:${id}`)
    }
    put(connections, id, target)
  }
  const invalidate = id => {
    for (const [identity, connection] of connections) {
      if (JSON.parse(identity)[0] === id) {
        entries.delete(connection)
        connections.delete(identity)
      }
    }
    for (const identity of entries.keys()) {
      if (identity.startsWith('saved:') && JSON.parse(identity.slice(6))[0] === id) entries.delete(identity)
    }
    epoch.value++
  }
  const clear = () => {
    entries.clear()
    connections.clear()
    epoch.value++
  }
  return reactive({ epoch, get, set, remember, invalidate, clear })
}

// Session memory only: do not persist questions or answers to browser storage.
export const useAiHelpStore = defineStore('aiHelp', () => {
  const modelCache = createModelCache()
  const selection = ref({ configId: undefined, model: '' })
  const configs = ref([])
  const selectedConfig = ref({})
  const loadingConfig = ref(false)
  let configSequence = 0
  const formData = ref({ question: '', answer: '' })
  const submittedQuestion = ref('')
  const history = ref([])
  const previousTurns = ref([])
  const loading = ref(false)
  const error = ref('')
  const stopped = ref(false)
  const metrics = ref(null)
  let controller
  const stop = (restoreDraft = true) => {
    const current = controller
    if (!current) return
    controller = undefined
    current?.abort()
    if (loading.value) {
      stopped.value = true
      if (restoreDraft) restoreQuestion()
    }
    loading.value = false
  }
  const restoreQuestion = () => {
    if (!formData.value.question) formData.value.question = submittedQuestion.value
  }
  const errorMessage = err => err?.data?.message || err?.response?.data?.message || err?.message || $i18nBundle('common.msg.unknownError')
  const selectConfig = async (id, preserveModel = false) => {
    const sequence = ++configSequence
    selectedConfig.value = {}
    selection.value.configId = id
    if (!preserveModel) {
      error.value = ''
      selection.value.model = ''
    }
    loadingConfig.value = true
    try {
      const response = await AiConfigApi.getById(id)
      if (sequence !== configSequence) return
      if (!response.success) throw new Error(response.message || $i18nBundle('common.msg.unknownError'))
      const config = response.resultData
      if (config.status !== 1) throw new Error($i18nBundle('mock.msg.aiHelpUnavailable'))
      modelCache.remember(config)
      const { id: configId, provider, baseUrl, configName, isDefault, modifyDate } = config
      selectedConfig.value = { id: configId, provider, baseUrl, configName, isDefault, modifyDate }
      if (!preserveModel || !selection.value.model) selection.value.model = config.defaultModel || ''
    } catch (err) {
      if (sequence === configSequence) error.value = errorMessage(err)
    } finally {
      if (sequence === configSequence) loadingConfig.value = false
    }
  }
  const loadConfigs = async () => {
    if (loadingConfig.value || loading.value) return
    const sequence = ++configSequence
    loadingConfig.value = true
    try {
      const response = await loadAiConfigOptions()
      if (sequence !== configSequence) return
      if (!response.success) throw new Error(response.message || $i18nBundle('common.msg.unknownError'))
      configs.value = (response.resultData || []).filter(config => !config.deleted && config.status === 1)
      const previous = configs.value.find(config => config.id === selection.value.configId)
      const initial = previous || configs.value.find(config => config.isDefault === 1) || configs.value[0]
      if (!initial) {
        selectedConfig.value = {}
        selection.value = { configId: undefined, model: '' }
        throw new Error($i18nBundle('mock.msg.aiHelpUnavailable'))
      }
      await selectConfig(initial.id, !!previous)
    } catch (err) {
      if (sequence === configSequence) error.value = errorMessage(err)
    } finally {
      if (sequence === configSequence) loadingConfig.value = false
    }
  }
  const ask = async () => {
    const question = formData.value.question.trim()
    if (!question || loading.value) return
    const current = new AbortController()
    controller = current
    stopped.value = false
    metrics.value = null
    loading.value = true
    error.value = ''
    previousTurns.value = [...history.value]
    submittedQuestion.value = question
    formData.value.question = ''
    formData.value.answer = ''
    try {
      await askAiHelp(question, {
        onDelta: data => {
          if (current === controller) formData.value.answer += data.text || ''
        },
        onFinish: data => {
          if (current !== controller) return
          formData.value.answer = data.resultData?.content || formData.value.answer
          metrics.value = data.resultData
          if (formData.value.answer.trim()) {
            history.value = [...history.value, { question, answer: formData.value.answer }].slice(-3)
            // Match the server's serialized history budget; remove whole oldest turns.
            const turnLength = turn => turn.question.length + turn.answer.length + 53
            while (history.value.reduce((size, turn) => size + turnLength(turn), 0) > 4000) history.value.shift()
          }
        },
        onError: data => {
          if (current === controller) error.value = errorMessage(data)
        }
      }, current.signal, history.value, selection.value)
    } catch (err) {
      if (current === controller) error.value = errorMessage(err)
    } finally {
      if (current === controller) {
        if (error.value) restoreQuestion()
        controller = undefined
        loading.value = false
      }
    }
  }

  const clearConversation = () => {
    stop(false)
    formData.value.answer = ''
    submittedQuestion.value = ''
    history.value = []
    previousTurns.value = []
    loading.value = false
    error.value = ''
    stopped.value = false
    metrics.value = null
  }
  const clear = () => {
    clearConversation()
    formData.value.question = ''
    configSequence++
    loadingConfig.value = false
    configs.value = []
    selectedConfig.value = {}
    selection.value = { configId: undefined, model: '' }
    modelCache.clear()
  }
  return { modelCache, selection, configs, selectedConfig, loadingConfig, selectConfig, loadConfigs, formData, submittedQuestion, history, previousTurns, loading, error, stopped, metrics, ask, stop, clearConversation, clear }
})
