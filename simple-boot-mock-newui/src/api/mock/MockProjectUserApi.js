import { useResourceApi } from '@/hooks/ApiHooks'

const MOCK_PROJECT_USER_URL = '/admin/projectUsers'
const MockProjectUserApi = useResourceApi(MOCK_PROJECT_USER_URL)

export default MockProjectUserApi
