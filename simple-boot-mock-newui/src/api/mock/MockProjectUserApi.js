import { useResourceApi } from '@/hooks/ApiHooks'
import { $http } from '@/vendors/axios'

const MOCK_PROJECT_USER_URL = '/admin/projectUsers'
const MockProjectUserApi = useResourceApi(MOCK_PROJECT_USER_URL, {
  saveBatch (data, config) {
    return $http(Object.assign({
      url: `${MOCK_PROJECT_USER_URL}/batch`,
      method: 'post',
      data
    }, config)).then(response => response.data)
  }
})

export default MockProjectUserApi
