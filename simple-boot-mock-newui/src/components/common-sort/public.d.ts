export interface SortOption {
  prop: string; // 排序字段，如price，该值会作为
  labelKey?: string;// 国际化资源key，首选该属性，不存在才使用label
  label?: string;
  showIcon?: boolean; // 控制某些排序不显示图标
  fixedValue?: 'ASC' | 'DESC'; // 固定排序模式
}

/**
 * 排序结果
 * key是排序字段，来自SortOption的prop，value是排序方式
 * 'ASC':升序  'DESC':降序 '':不生效
 * 如 { "deptTime": "DESC", "price": "", "duration": "" }
 */
export interface SortProps {
  [key: string]: 'ASC' | 'DESC' | ''
}
