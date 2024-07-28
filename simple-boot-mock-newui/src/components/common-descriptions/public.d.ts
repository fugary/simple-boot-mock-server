import { VNode } from 'vue'
import { descriptionItemProps } from 'element-plus/es/components/descriptions/src/description-item'

export interface CommonDescriptionItem {
    label?: string;
    labelKey?: string;
    value?: string;
    span?: number;
    width?: string;
    minWidth?: string;
    className?: string;
    labelClassName?: string;
    align?: 'left' | 'right' | 'center';
    labelAlign?: string;
    slots?: any;
    formatter?: (item: CommonDescriptionItem) => string | VNode;
    attrs?: descriptionItemProps;
}

export interface CommonDescriptionProps {
    border?: boolean;
    column?: number;
    direction?: 'horizontal' | 'vertical';
    size?: 'default' | 'small' | 'large';
    title?: string;
    extra?: string;
    items?: Array<CommonDescriptionItem>;
    width?: string;
    minWidth?: string;
}
