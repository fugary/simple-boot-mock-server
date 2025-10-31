import { useResourceApi } from '@/hooks/ApiHooks'

const BASE_URL = '/admin/dbs'

const MockDbApi = useResourceApi(BASE_URL)

export default MockDbApi
