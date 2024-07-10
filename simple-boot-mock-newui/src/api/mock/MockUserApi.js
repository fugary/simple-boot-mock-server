import { useResourceApi } from '@/hooks/ApiHooks'

const MockUserApi = useResourceApi('/admin/users')

/**
 * @return {CommonAutocompleteOption}
 */
export const useUserAutocompleteConfig = () => {
  return {
    idProp: 'userName',
    labelProp: 'userName',
    columns: [{
      labelKey: 'common.label.username',
      property: 'userName'
    }, {
      labelKey: 'common.label.nickName',
      property: 'nickName'
    }, {
      labelKey: 'common.label.email',
      property: 'userEmail'
    }],
    searchMethod ({ query, page }, cb) {
      MockUserApi.search({ status: 1, keyword: query, page })
        .then(result => {
          const data = {
            page: result.page,
            items: result.resultData
          }
          cb(data)
        })
    }
  }
}

export default MockUserApi
