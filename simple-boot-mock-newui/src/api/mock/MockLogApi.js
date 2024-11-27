import { useResourceApi } from '@/hooks/ApiHooks'

const BASE_URL = '/admin/logs'

const ApiLogApi = useResourceApi(BASE_URL)

export default ApiLogApi
