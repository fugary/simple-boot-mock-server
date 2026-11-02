import { defineStore } from 'pinia'
import { ref } from 'vue'
import { askAiHelp } from '@/api/mock/AiApi'
import { $i18nBundle } from '@/messages'

// Session memory only: do not persist questions or answers to browser storage.
export const useAiHelpStore = defineStore('aiHelp', () => {
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

  const clear = () => {
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
  return { formData, submittedQuestion, history, previousTurns, loading, error, stopped, metrics, ask, stop, clear }
})
