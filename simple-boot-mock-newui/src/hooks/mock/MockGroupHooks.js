import { computed, ref } from 'vue'
import MockGroupApi from '@/api/mock/MockGroupApi'

export const useMockGroupItem = (groupId, load = true) => {
  const groupItem = ref({})
  const mockProject = ref()
  const loadSuccess = ref(false)

  const loadGroup = () => {
    MockGroupApi.getById(groupId).then(data => {
      groupItem.value = data.resultData
      mockProject.value = data.infos?.mockProject
      loadSuccess.value = !!data.resultData
    })
  }

  load && loadGroup()

  const groupUrl = computed(() => `/mock/${groupItem.value?.groupPath}`)

  return {
    groupItem,
    loadGroup,
    mockProject,
    groupUrl,
    loadSuccess
  }
}
