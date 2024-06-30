import { RuleItem } from 'async-validator/dist-types/interface'
import {
  FormInstance, FormProps, DialogProps,
  InputProps, InputNumberProps, CascaderProps,
  RadioGroupProps, RadioProps, RadioButtonProps,
  CheckboxProps, CheckboxGroupProps, CheckboxButtonProps,
  DatePickerProps, timePickerDefaultProps, SwitchProps, SliderProps, TransferProps
} from 'element-plus'
import { SelectProps as SelectV1Props } from 'element-plus/es/components/select/src/select'
import SelectV2Props from 'element-plus/es/components/select-v2/src/select-v2/defaults'
import { CommonAutocompleteProps } from '../common-autocomplete/public'
import { TreeComponentProps } from 'element-plus/es/components/tree/src/tree.type'
import { ExtractPropTypes, CSSProperties, VNode } from 'vue'

export type TimePickerProps = ExtractPropTypes<typeof timePickerDefaultProps>
export type SelectProps = ExtractPropTypes<typeof SelectV1Props>
export interface OptionProps {
    label: string;
    value: any;
    disabled: boolean;
}

export interface IconSelectProps {
    dialogAttrs: DialogProps,
    colSize: number,
    dialogHeight: string,
    dialogWidth: string,
    disabled: boolean,
    readonly: boolean,
    placeholder: string,
    clearable: boolean,
    validateEvent: boolean
}

export interface CommonFormLabelProps {
    /**
     * 显示文本
     */
    modelText: string;
}

export interface CommonTabFilterProps {
    tabs: Array<{
        icon: string,
        label: string,
        prop: string,
        children: OptionProps[]
    }>,
    type: 'checkbox-group' | 'radio-group',
    defaultIcon: string,
    iconSize: number
}

/**
 * 'input' | 'input-number' | 'cascader' | 'radio'
 *     | 'radio-group' | 'radio-button' | 'checkbox' | 'checkbox-group' | 'checkbox-button' | 'date-picker'
 *     | 'time-picker' | 'switch' | 'select' | 'select-v2' | 'option' | 'slider' | 'transfer' | 'upload'
 *     | 'common-tab-filter' | 'common-icon-select' | 'common-autocomplete' | 'common-form-label'
 *     | 'tree-select'
 */
export type PropsMap = {
    'input': InputProps,
    'input-number': InputNumberProps,
    'cascader': CascaderProps,
    'radio': RadioProps,
    'radio-group': RadioGroupProps,
    'radio-button': RadioButtonProps,
    'checkbox': CheckboxProps,
    'checkbox-group': CheckboxGroupProps,
    'checkbox-button': CheckboxButtonProps,
    'date-picker': DatePickerProps,
    'time-picker': TimePickerProps,
    'switch': SwitchProps,
    'select': SelectProps,
    'select-v2': SelectV2Props,
    'option': OptionProps,
    'slider': SliderProps,
    'transfer': TransferProps,
    'upload': TransferProps,
    'tree-select': SelectProps & TreeComponentProps,
    'common-tab-filter': CommonTabFilterProps,
    'common-form-label': CommonFormLabelProps,
    'common-icon-select': IconSelectProps,
    'common-autocomplete': CommonAutocompleteProps,
    [key:string]: InputProps
}

type FormControlTypeOption = {
    [Type in keyof PropsMap]: {
        type?: Type,
        attrs?: PropsMap[Type]
    }
}[keyof PropsMap]

export interface CommonFormOption extends FormControlTypeOption {
    /** 数据值 */
    value?: any;
    /** 属性名 */
    prop?: string | string[];
    /** 表单标签 */
    label?: string;
    /** 用于国际化的label */
    labelKey?: string;
    /**
     * 样式自定义
     */
    labelCls?: string;
    /**
     * item样式
     */
    style: CSSProperties;
    /** 是否必填,后面解析成为rules的一部分 */
    required?: boolean;
    /** 正则表达式验证，解析成为rules的一部分 */
    pattern?: string | RegExp;
    /** 正则表达式验证消息 */
    patternMsg?: string;
    /** 是否禁用 */
    disabled?: boolean;
    /** 是否显示 */
    enabled?: boolean;
    /** 是否只读 */
    readonly?: boolean;
    /** 占位提示符 */
    placeholder?: string;
    /** 日期最小值**/
    minDate: string|Date;
    /** 日期最大值**/
    maxDate: string|Date;
    /** 是否清理不可用日期值**/
    clearInvalidDate: boolean;
    /** 有些控件柚子节点 */
    children?: Array<CommonFormOption>;
    /** async-validator验证器 */
    rules?: Array<RuleItem>;
    /** change事件 */
    change?: (val: any) => void;
    /** 提示信息 */
    tooltip?: string;
    /** 提示函数 */
    tooltipFunc?: () => void;
    /** 自动trim，默认false**/
    trim?: boolean,
    /** 自动upperCase，默认false**/
    upperCase?: boolean,
    /** 自动lowerCase，默认false**/
    lowerCase?: boolean,
    /**
     * common-form-label格式化
     * @param modelValue 数据
     * @param option 选项
     */
    formatter?: (modelValue:any, option: CommonFormOption) => string|VNode;
    /** 自定义slot名称 */
    slot?: string;
    /** 自定义label slot名称 */
    labelSlot?: string;
    /**
     * 根据model数据动态计算Option值
     * @param model 表单model
     * @param option 原始选项
     */
    dynamicOption?: (model: any, option: CommonFormOption, addInfo?: any) => CommonFormOption;
    /**
     * 根据model数据动态计算attrs的值
     * @param model 表单model
     * @param option 原始选项
     */
    dynamicAttrs?: (model: any, option: CommonFormOption, addInfo?: any) => any;
}

export interface CommonFormProps extends FormProps {
    /** 配置选项 */
    options: Array<CommonFormOption>;
    /** label宽度 */
    labelWidth: string;
    /** model对象 */
    model: any;
    /** 是否在rule改变时执行验证 */
    validateOnRuleChange: boolean;
    /** 提交按钮的label */
    submitLabel: string;
    /** 重置按钮label */
    resetLabel: string;
    /** 返回按钮label */
    backLabel: string;
    /** 是否显示按钮区域 */
    showButtons: boolean;
    /** 是否显示提交按钮 */
    showSubmit: boolean;
    /** 当校验不通过时提交按钮不可点击，默认为 false: 校验不通过也可直接提交 */
    disableSubmitIfNotValid: boolean;
    /** 是否显示重置按钮 */
    showReset: boolean;
    /** 提交逻辑 */
    submitForm: (form: FormInstance) => void;
    /** 返回地址 */
    backUrl: string;
    /** 行级排列 */
    inline: boolean;
    /** 回车提交 */
    submitByEnter: boolean;
}
