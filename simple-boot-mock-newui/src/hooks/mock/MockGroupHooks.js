import { computed, ref } from 'vue'
import MockGroupApi from '@/api/mock/MockGroupApi'

export const useMockGroupItem = groupId => {
  const groupItem = ref({})
  const loadSuccess = ref(false)

  MockGroupApi.getById(groupId).then(data => {
    groupItem.value = data.resultData
    loadSuccess.value = !!data.resultData
  })

  const groupUrl = computed(() => `/mock/${groupItem.value?.groupPath}`)

  return {
    groupItem,
    groupUrl,
    loadSuccess
  }
}
