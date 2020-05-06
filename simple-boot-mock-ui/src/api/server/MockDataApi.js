import MockModelApi from '@/api/server/MockModelApi'
import request from '@/utils/request'

const MOCK_DATA_URL = '/admin/data'
const MockDataApi = new MockModelApi(MOCK_DATA_URL)

MockDataApi.previewData = function(data) {
  request({
    url: `${MOCK_DATA_URL}/preview`,
    method: 'post',
    data
  })
}

export default MockDataApi
