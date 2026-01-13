import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useJsonTableConfigStore = defineStore('jsonTableConfig', () => {
  const jsonTableConfigs = ref({})
  return {
    jsonTableConfigs,
    saveTableConfig (data) {
      jsonTableConfigs.value[data.name] = data
    },
    getTableConfig (name) {
      return jsonTableConfigs.value[name]
    },
    deleteTableConfig (name) {
      delete jsonTableConfigs.value[name]
    },
    clearConfigs () {
      jsonTableConfigs.value = {}
    }
  }
}, {
  persist: true
})
