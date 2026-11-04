import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import { askAiHelp } from '@/api/mock/AiApi'
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
  const formData = ref({ question: '', answer: '' })
  const submittedQuestion = ref('')
  const history = ref([])
  const previousTurns = ref([])
  const loading = ref(false)
  const error = ref('')
  const stopped = ref(false)
  const metrics = ref(null)
  let controller
  const stop = () => {
    const current = controller
    if (!current) return
    controller = undefined
    current?.abort()
    if (loading.value) {
      stopped.value = true
      restoreQuestion()
    }
    loading.value = false
  }
  const restoreQuestion = () => {
    if (!formData.value.question) formData.value.question = submittedQuestion.value
  }
  const errorMessage = err => err?.data?.message || err?.response?.data?.message || err?.message || $i18nBundle('common.msg.unknownError')
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
      }, current.signal, history.value)
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
    stop()
    formData.value = { question: '', answer: '' }
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
    modelCache.clear()
  }
  return { modelCache, formData, submittedQuestion, history, previousTurns, loading, error, stopped, metrics, ask, stop, clearConversation, clear }
})
