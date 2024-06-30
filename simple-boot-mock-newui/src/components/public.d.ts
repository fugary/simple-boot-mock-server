/**
 * 分页参数
 */
export interface CommonPage {
    /** 当前第几页 */
    pageNumber: number;
    /** 每页数量 */
    pageSize: number;
    /** 总页数 */
    pageCount?: number;
    /** 数据总数 */
    totalCount?: number;
}

/**
 * element树结构
 */
export interface CommonTreeNode {
    /** 树值 */
    value: string;
    /** 展示标签 */
    label: string;
    /** 子节点 */
    children?: Array<CommonTreeNode>;
    /** 是否是叶子节点 */
    isLeaf?: boolean;
}
