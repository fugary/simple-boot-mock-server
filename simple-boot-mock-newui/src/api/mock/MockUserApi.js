import { useResourceApi } from '@/hooks/ApiHooks'
import { useDefaultPage } from '@/config'
import { ref } from 'vue'

const MockUserApi = useResourceApi('/admin/users')

/**
 * @return {CommonAutocompleteOption}
 */
export const useUserAutocompleteConfig = () => {
  return {
    idProp: 'userName',
    labelProp: 'userName',
    columns: [{
      labelKey: 'common.label.username',
      property: 'userName'
    }, {
      labelKey: 'common.label.nickName',
      property: 'nickName'
    }, {
      labelKey: 'common.label.email',
      property: 'userEmail'
    }],
    searchMethod ({ query, page }, cb) {
      MockUserApi.search({ status: 1, keyword: query, page })
        .then(result => {
          const data = {
            page: result.page,
            items: result.resultData
          }
          cb(data)
        })
    }
  }
}
/**
 * 加载所有可用用户，限制100个，一般不会有这么多
 * @return {Promise<T>}
 */
export const loadAllUsers = () => {
  return MockUserApi.search({ status: 1, page: useDefaultPage(100) })
    .then(result => result.resultData)
}

export const useAllUsers = () => {
  const users = ref([])
  const userOptions = ref([])
  loadAllUsers().then(result => {
    users.value = result || []
    userOptions.value = users.value.map(user => ({ label: user.userName, value: user.userName }))
  })
  return {
    users,
    userOptions
  }
}

export default MockUserApi
