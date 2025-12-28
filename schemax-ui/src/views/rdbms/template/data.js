export const TEMPLATE_TYPE_LIST = [
  {value: 1, label: 'excel'},
  // TODO  word待实现
  // {value: 2, label: 'word'},
  {value: 3, label: 'markdown'},
]

export const PLACEHOLDER_TREE = [
  {
    label: 'ID类型',
    children: [
      {label: '${UUID}'},
      {label: '${nanoId}'},
      {label: '${order}'},
    ],
  },
  {
    label: '表格元信息',
    children: [
      {label: '${schema}'},
      {label: '${tableName}'},
      {label: '${tableComment}'},
    ],
  },
  {
    label: '字段信息',
    children: [
      {label: '${columnName}'},
      {label: '${columnType}'},
      {label: '${columnSize}'},
      {label: '${columnDigit}'},
      {label: '${columnNullable}'},
      {label: '${columnAutoIncrement}'},
      {label: '${columnPk}'},
      {label: '${columnDef}'},
      {label: '${columnComment}'},
    ],
  },
]

