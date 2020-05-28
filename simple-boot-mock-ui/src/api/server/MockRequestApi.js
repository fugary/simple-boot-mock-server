import RequestModelApi, { $http } from '@/utils/RequestModelApi'

const MOCK_REQUEST_URL = '/admin/requests'

const MockRequestApi = new RequestModelApi(MOCK_REQUEST_URL)

Object.assign(MockRequestApi, {
  getDefaultData (requestId, config) {
    return $http(Object.assign({
      url: `${MOCK_REQUEST_URL}/getDefaultData/${requestId}`,
      method: 'get'
    }, config))
  },
  saveMockParams (data, config) {
    return $http(Object.assign({
      url: `${MOCK_REQUEST_URL}/saveMockParams`,
      method: 'post',
      data
    }, config))
  }
})
export default MockRequestApi
