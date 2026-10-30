import { useResourceApi } from '@/hooks/ApiHooks'
import { $httpGet, $httpPost } from '@/vendors/axios'
export const AiConfigApi = useResourceApi('/admin/ai/configs')
export const AiLogApi = useResourceApi('/admin/ai/logs')
export const testAiConfig = (id, data) => $httpPost('/admin/ai/configs/' + id + '/test', data, { timeout: 90000 })

export const loadAiModels = data => $httpPost('/admin/ai/configs/models', data, { timeout: 90000, showErrorMessage: false })

export const loadAiConfigOptions = () => $httpGet('/admin/ai/configs/options')
