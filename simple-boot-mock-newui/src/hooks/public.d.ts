import type { Ref } from 'vue'

export interface CommonTableAndSearchForm {
    defaultParam?: any;
    searchMethod: (searchParam: any) => Promise<any>;
    dataProcessor?: (resultData: any, searchParam: any) => any[];
    pageProcessor?: (resultData: any, searchParam: any) => void;
    saveParam?: boolean;
}

export interface CommonTableAndSearchResult {
    tableData: Ref<Array<any>>;
    loading: Ref<boolean>;
    searchParam: Ref<any>;
    searchMethod: (pageNumber?: number) => Promise<any>;
}
