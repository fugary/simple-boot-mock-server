import MockModelApi from '@/api/server/MockModelApi'
import request from '@/utils/request'

const MOCK_REQUEST_URL = '/admin/requests'

const MockRequestApi = new MockModelApi(MOCK_REQUEST_URL)

Object.assign(MockRequestApi, {
  getDefaultData(requestId, config) {
    return request(Object.assign({
      url: `${MOCK_REQUEST_URL}/getDefaultData/${requestId}`,
      method: 'get'
    }, config))
  },
  saveMockParams(data, config) {
    return request(Object.assign({
      url: `${MOCK_REQUEST_URL}/saveMockParams`,
      method: 'post',
      data
    }, config))
  }
})
export default MockRequestApi
