import { $http } from '@/vendors/axios'
import { useResourceApi } from '@/hooks/ApiHooks'

const BASE_URL = '/admin/scheduled-tasks'

const ScheduledTaskApi = useResourceApi(BASE_URL, {
  trigger (taskCode, config) {
    return $http(Object.assign({
      url: `${BASE_URL}/${taskCode}/trigger`,
      method: 'post'
    }, config)).then(response => response.data)
  }
})

export default ScheduledTaskApi
