import { ButtonProps, LinkProps, TableProps, PaginationProps } from 'element-plus'
import tableColumnProps from 'element-plus/es/components/table/src/table-column/defaults'
import { CommonPage } from '../public'
import { ExtractPropTypes, VNode } from 'vue'

export type TableColumnProps = ExtractPropTypes<typeof tableColumnProps>

export type TableButtonProps = {
    /**
     * 计算是否显示按钮
     * @param data 表格数据
     */
    buttonIf: (data: any) => boolean
    /**
     * 动态计算按钮属性
     */
    dynamicButton: (data: any) => any
    /**
     * 按钮其他选项
     */
    attrs: ButtonProps
} & ButtonProps

/**
 * 表格列定义
 */
export interface CommonTableColumn {
    /** 是否显示 */
    enabled?: boolean;
    /** 表格头 */
    label?: string;
    /** 表格头国际化key */
    labelKey?: string;
    /** 属性名 */
    property?: string;
    /** 属性名，同property */
    prop?: string;
    /** 宽度 */
    width?: string;
    /** 是否是可操作列 */
    isOperation?: boolean;
    /** 自定义插槽名称，用于自定义显示数据 */
    slot?: string;
    /** 自定义插槽名称，用于自定义显示Label */
    headerSlot?: string;
    /** 自定义按钮 */
    buttons?: Array<TableButtonProps>
    /** 可选属性 */
    attrs?: TableColumnProps;
    /** 链接可选属性 */
    linkAttrs?: LinkProps;
    /** 点击事件 */
    click?: (data: any) => any;
    /** 格式化函数 */
    formatter?: (data: any, scope: any) => string|VNode;
    /** header格式化 */
    headerFormatter?: (data: any, scope: any) => string|VNode;
    /** 日期格式化 */
    dateFormat?: string
}

/**
 * 表格配置
 */
export interface CommonTableProps extends TableProps<any> {
    /** 列配置 */
    columns: Array<CommonTableColumn>;
    /** 显示数据 */
    data: Array<any>;
    /** 高亮当前行 */
    highlightCurrentRow: boolean;
    /** stripe */
    stripe: boolean;
    /** 边框配置 */
    border: boolean;
    /** 自定义按钮配置 */
    buttons?: Array<TableButtonProps>;
    /** buttons插槽 */
    buttonsSlot?: string;
    /** 默认的按钮大小 */
    buttonSize?: string;
    /** 按钮列配置 */
    buttonsColumnAttrs?: TableColumnProps;
    /** 分页配置 */
    page?: CommonPage;
    /** 分页对齐 */
    pageAlign?: 'left' | 'center' | 'right';
    /** 其他分页配置项 */
    pageAttrs?: PaginationProps;
    /** 是否显示分页数量选择 **/
    showPageSizes?: boolean;
    /** loading状态 */
    loading?: boolean;
    /** loading显示消息 */
    loadingText?: string;
    /** 可以展开的table */
    expandTable: boolean;
    /** 是否隐藏展开按钮 */
    hideExpandBtn: boolean;
    /** 是否是前台分页**/
    frontendPaging: boolean;
    /** 前端模式分页数量 **/
    frontendPageSize: number;
    /** 是否是无限加载分页**/
    infinitePaging: boolean;
}

export interface CommonTableColumnProps {
    column: CommonTableColumn,
    buttonSize: '' | 'small' | 'large' | 'default'
}
