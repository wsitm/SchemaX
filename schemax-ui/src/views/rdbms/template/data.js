export const TEMPLATE_TYPE_LIST = [
  {value: 1, label: 'excel'},
  // TODO  word待实现
  // {value: 2, label: 'word'},
  {value: 3, label: 'markdown'},
]

export const PLACEHOLDER_TREE = [
  {
    label: 'ID类型',
    level: 1,
    children: [
      {label: '${UUID}', level: 2},
      {label: '${nanoId}', level: 2},
      {label: '${order}', level: 2},
    ],
  },
  {
    label: '表格元信息',
    level: 1,
    children: [
      {label: '${schema}', level: 2},
      {label: '${tableName}', level: 2},
      {label: '${tableComment}', level: 2},
    ],
  },
  {
    label: '字段信息',
    level: 1,
    children: [
      {
        label: '${columnList}',
        level: 2,
        children: [
          {label: '${name}', level: 3},
          {label: '${type}', level: 3},
          {label: '${size}', level: 3},
          {label: '${digit}', level: 3},
          {label: '${nullable}', level: 3},
          {label: '${autoIncrement}', level: 3},
          {label: '${pk}', level: 3},
          {label: '${def}', level: 3},
          {label: '${comment}', level: 3},
        ]
      }
    ],
  },
]

