const PH = {
  schema: '${schema}',
  catalog: '${catalog}',
  tableName: '${tableName}',
  tableComment: '${tableComment}',
  numRows: '${numRows}',
  columnName: '${columnName}',
  columnType: '${columnType}',
  columnSize: '${columnSize}',
  columnDigit: '${columnDigit}',
  columnNullable: '${columnNullable}',
  columnAutoIncrement: '${columnAutoIncrement}',
  columnPk: '${columnPk}',
  columnDef: '${columnDef}',
  columnComment: '${columnComment}',
  uuid: '${UUID}',
  nanoId: '${nanoId}',
  order: '${order}',
}

const COLUMN_PLACEHOLDER_LIST = [
  PH.columnName,
  PH.columnType,
  PH.columnSize,
  PH.columnDigit,
  PH.columnNullable,
  PH.columnAutoIncrement,
  PH.columnPk,
  PH.columnDef,
  PH.columnComment,
]

const replaceAll = (text, from, to) => String(text).split(from).join(to)

const containsColumnPlaceholder = (text = '') => {
  return COLUMN_PLACEHOLDER_LIST.some((item) => String(text).includes(item))
}

const generateUuid = () => {
  return 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

const generateNanoId = (len = 16) => {
  const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
  let out = ''
  for (let i = 0; i < len; i++) {
    out += chars[Math.floor(Math.random() * chars.length)]
  }
  return out
}

const renderText = (text, table, column, tableOrder, columnOrder) => {
  let out = String(text ?? '')
  out = replaceAll(out, PH.schema, table?.schema ?? '')
  out = replaceAll(out, PH.catalog, table?.catalog ?? '')
  out = replaceAll(out, PH.tableName, table?.tableName ?? '')
  out = replaceAll(out, PH.tableComment, table?.comment ?? '')
  out = replaceAll(out, PH.numRows, table?.numRows ?? '')

  if (column) {
    out = replaceAll(out, PH.columnName, column?.name ?? '')
    out = replaceAll(out, PH.columnType, column?.typeName ?? '')
    out = replaceAll(out, PH.columnSize, column?.size ?? '')
    out = replaceAll(out, PH.columnDigit, column?.digit ?? '')
    out = replaceAll(out, PH.columnNullable, column?.nullable ? 'YES' : 'NO')
    out = replaceAll(out, PH.columnAutoIncrement, column?.autoIncrement ? 'YES' : 'NO')
    out = replaceAll(out, PH.columnPk, column?.pk ? 'YES' : 'NO')
    out = replaceAll(out, PH.columnDef, column?.columnDef ?? '')
    out = replaceAll(out, PH.columnComment, column?.comment ?? '')
  } else {
    out = replaceAll(out, PH.columnName, '')
    out = replaceAll(out, PH.columnType, '')
    out = replaceAll(out, PH.columnSize, '')
    out = replaceAll(out, PH.columnDigit, '')
    out = replaceAll(out, PH.columnNullable, '')
    out = replaceAll(out, PH.columnAutoIncrement, '')
    out = replaceAll(out, PH.columnPk, '')
    out = replaceAll(out, PH.columnDef, '')
    out = replaceAll(out, PH.columnComment, '')
  }

  out = replaceAll(out, PH.order, columnOrder || tableOrder || 1)
  if (out.includes(PH.uuid)) {
    out = replaceAll(out, PH.uuid, generateUuid())
  }
  if (out.includes(PH.nanoId)) {
    out = replaceAll(out, PH.nanoId, generateNanoId())
  }
  return out
}

const rowHasColumnPlaceholder = (rowObj = {}) => {
  return Object.keys(rowObj).some((colKey) => {
    const cell = rowObj[colKey] || {}
    return containsColumnPlaceholder(cell?.v) || containsColumnPlaceholder(cell?.m)
  })
}

const renderRow = (rowObj = {}, table, column, tableOrder, columnOrder) => {
  const rendered = {}
  Object.keys(rowObj).forEach((colKey) => {
    const sourceCell = rowObj[colKey] || {}
    const cell = JSON.parse(JSON.stringify(sourceCell))
    if (typeof cell.v === 'string') {
      cell.v = renderText(cell.v, table, column, tableOrder, columnOrder)
    }
    if (typeof cell.m === 'string') {
      cell.m = renderText(cell.m, table, column, tableOrder, columnOrder)
    }
    rendered[colKey] = cell
  })
  return rendered
}

export const resolveDefaultTemplate = (templateList = []) => {
  if (!templateList.length) return null
  const sorted = [...templateList].sort((a, b) => {
    const defDiff = (b.isDef || 0) - (a.isDef || 0)
    if (defDiff !== 0) return defDiff
    return (a.tpId || 0) - (b.tpId || 0)
  })
  return sorted[0] || null
}

export const renderMarkdownByTemplate = (templateContent, tableInfoList = []) => {
  if (!templateContent) return ''
  if (!tableInfoList.length) return ''
  const lines = String(templateContent).split(/\\r?\\n/)
  const blocks = []

  tableInfoList.forEach((table, tableIndex) => {
    const renderedLines = []
    lines.forEach((line) => {
      if (containsColumnPlaceholder(line)) {
        const columns = table?.columnList || []
        if (!columns.length) {
          renderedLines.push(renderText(line, table, null, tableIndex + 1))
        } else {
          columns.forEach((column, columnIndex) => {
            renderedLines.push(renderText(line, table, column, tableIndex + 1, columnIndex + 1))
          })
        }
      } else {
        renderedLines.push(renderText(line, table, null, tableIndex + 1))
      }
    })
    blocks.push(renderedLines.join('\\n'))
  })

  return blocks.join('\\n\\n')
}

export const renderWorkbookByTemplate = (templateContent, tableInfoList = []) => {
  if (!templateContent) return null
  if (!tableInfoList.length) return null
  let workbook
  try {
    workbook = JSON.parse(templateContent)
  } catch (e) {
    return null
  }
  const result = JSON.parse(JSON.stringify(workbook || {}))
  const sheets = result?.sheets || {}

  Object.keys(sheets).forEach((sheetId) => {
    const sheet = sheets[sheetId]
    const cellData = sheet?.cellData || {}
    const rowIndexes = Object.keys(cellData).map(Number).sort((a, b) => a - b)
    const renderedCellData = {}
    let nextRow = 0

    tableInfoList.forEach((table, tableIndex) => {
      rowIndexes.forEach((templateRowIndex) => {
        const rowObj = cellData[String(templateRowIndex)] || {}
        if (rowHasColumnPlaceholder(rowObj)) {
          const columns = table?.columnList || []
          if (!columns.length) {
            renderedCellData[String(nextRow++)] = renderRow(rowObj, table, null, tableIndex + 1)
          } else {
            columns.forEach((column, columnIndex) => {
              renderedCellData[String(nextRow++)] = renderRow(rowObj, table, column, tableIndex + 1, columnIndex + 1)
            })
          }
        } else {
          renderedCellData[String(nextRow++)] = renderRow(rowObj, table, null, tableIndex + 1)
        }
      })
      if (tableIndex < tableInfoList.length - 1) {
        nextRow += 1
      }
    })

    sheet.cellData = renderedCellData
    sheet.rowCount = Math.max(100, nextRow + 10)
    sheet.mergeData = []
  })

  return result
}
