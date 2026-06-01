import { useResourceApi } from '@/hooks/ApiHooks'
import { $http } from '@/vendors/axios'

const BASE_URL = '/admin/logs'

const ApiLogApi = useResourceApi(BASE_URL, {
  loadPreviewMeta (diagnoseId, config) {
    return $http(Object.assign({
      url: `${BASE_URL}/diagnose/${diagnoseId}`,
      method: 'get',
      loading: false,
      showErrorMessage: false
    }, config)).then(response => response.data)
  }
})

export default ApiLogApi
