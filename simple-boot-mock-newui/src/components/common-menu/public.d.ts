import { RouteRecordRaw } from 'vue-router'

/**
 * 菜单对象
 */
export interface CommonMenuItem {
    /** 是否是下拉Dropdown样式 */
    isDropdown?: boolean;
    /** 是否是分割元素 */
    isSplit?: boolean;
    /**
     * 是否禁用，禁用状态仍然是显示的
     */
    disabled?: boolean;
    /**
     * 是否启用，默认true，设置false不显示
     */
    enabled?: boolean;
    /** 自定义样式 */
    menuCls?: string;
    /** 路由地址 */
    index?: string;
    /** 路由 */
    route?: RouteRecordRaw;
    /** 图标 */
    icon?: string;
    /** 图标大小 */
    iconSize?: number | string;
    /** 菜单显示名称 */
    label?: string;
    /** 菜单显示名称的Key，国际化需要 */
    labelKey?: string;
    /** 图标计算函数 */
    iconIf?: () => string;
    /** click事件 */
    click?: () => string;
    /** 子菜单 */
    children?: Array<CommonMenuItem>;
    /** 自定义属性 */
    attrs: {
        [key: string]: any
    }
}

export interface CommonMenuItemProps {
    /** menu配置 */
    menuItem: CommonMenuItem;
    /** index序号 */
    index: number | string;
}
