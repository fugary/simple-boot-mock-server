import { useResourceApi } from '@/hooks/ApiHooks'
import { $http } from '@/vendors/axios'

const MOCK_SCENARIO_URL = '/admin/scenarios'

export const activateScenario = (data, config) => {
  return $http(Object.assign({
    url: `${MOCK_SCENARIO_URL}/activate`,
    method: 'POST',
    data
  }, config)).then(response => response.data)
}

export const toggleScenarioStatus = (id, config) => {
  return $http(Object.assign({
    url: `${MOCK_SCENARIO_URL}/toggleStatus/${id}`,
    method: 'POST'
  }, config)).then(response => response.data)
}

export default useResourceApi(MOCK_SCENARIO_URL)
