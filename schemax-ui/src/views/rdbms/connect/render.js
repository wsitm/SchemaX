const FOR_DIRECTIVE_RE = /^#for\s*\(\s*([a-zA-Z_]\w*)\s+in\s+([a-zA-Z_][\w.]*)\s*\)\s*$/
const END_DIRECTIVE_RE = /^#end\s*$/
const EXPRESSION_RE = /\$\{\s*([^}]+?)\s*}/g

const COLUMN_FIELD_SET = new Set([
  'columnName',
  'columnType',
  'columnSize',
  'columnDigit',
  'columnNullable',
  'columnAutoIncrement',
  'columnPk',
  'columnDef',
  'columnComment',
  'name',
  'type',
  'typeName',
  'size',
  'digit',
  'nullable',
  'autoIncrement',
  'pk',
  'def',
  'comment',
])

const LEGACY_COLUMN_EXPRESSION_SET = new Set([
  ...COLUMN_FIELD_SET,
])

const hasOwn = (obj, key) => Object.prototype.hasOwnProperty.call(obj || {}, key)

const deepClone = (val) => JSON.parse(JSON.stringify(val))

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

const formatValue = (value) => {
  if (value === null || value === undefined) return ''
  if (typeof value === 'boolean') {
    return value ? 'YES' : ''
  }
  if (typeof value === 'object') return ''
  return String(value)
}

const normalizeColumn = (column = {}, order = 1) => {
  const name = column?.name ?? ''
  const typeName = column?.typeName ?? column?.type ?? ''
  const size = column?.size ?? ''
  const digit = column?.digit ?? ''
  const nullable = formatValue(column?.nullable)
  const autoIncrement = formatValue(column?.autoIncrement)
  const pk = formatValue(column?.pk)
  const def = column?.def ?? column?.columnDef ?? ''
  const comment = column?.comment ?? ''

  return {
    order,
    name,
    type: typeName,
    typeName,
    size,
    digit,
    nullable,
    autoIncrement,
    pk,
    def,
    columnDef: def,
    comment,
    columnName: name,
    columnType: typeName,
    columnSize: size,
    columnDigit: digit,
    columnNullable: nullable,
    columnAutoIncrement: autoIncrement,
    columnPk: pk,
    columnComment: comment,
  }
}

const normalizeTable = (table = {}, tableOrder = 1) => {
  const columnList = Array.isArray(table?.columnList)
    ? table.columnList.map((item, idx) => normalizeColumn(item, idx + 1))
    : []
  return {
    schema: table?.schema ?? '',
    catalog: table?.catalog ?? '',
    tableName: table?.tableName ?? '',
    tableComment: table?.tableComment ?? table?.comment ?? '',
    comment: table?.comment ?? table?.tableComment ?? '',
    numRows: table?.numRows ?? '',
    order: tableOrder,
    columnList,
  }
}

const buildTableContext = (table = {}, tableOrder = 1) => {
  const normalizedTable = normalizeTable(table, tableOrder)
  return {
    ...normalizedTable,
    table: normalizedTable,
    order: tableOrder,
    _inFor: false,
  }
}

const buildLoopContext = (parent = {}, alias, item, loopOrder) => {
  const next = {
    ...parent,
    [alias]: item,
    order: loopOrder,
    _inFor: true,
  }
  if (item && typeof item === 'object') {
    Object.keys(item).forEach((key) => {
      if (!hasOwn(next, key)) {
        next[key] = item[key]
      }
    })
  }
  return next
}

const resolvePath = (ctx = {}, expr = '') => {
  const key = String(expr || '').trim()
  if (!key) return ''
  if (key === 'UUID') return generateUuid()
  if (key === 'nanoId') return generateNanoId()
  if (key === 'order') return ctx?.order ?? 1

  const segments = key.split('.').filter(Boolean)
  if (!segments.length) return ''
  let cursor = ctx
  for (let i = 0; i < segments.length; i++) {
    const part = segments[i]
    if (cursor === null || cursor === undefined) return ''
    if (typeof cursor !== 'object') return ''
    if (!hasOwn(cursor, part)) return ''
    cursor = cursor[part]
  }
  return cursor
}

const renderText = (text, ctx = {}) => {
  return String(text ?? '').replace(EXPRESSION_RE, (_, expr) => {
    const value = resolvePath(ctx, expr)
    return formatValue(value)
  })
}

const parseDirectiveText = (text = '') => {
  const content = String(text ?? '').trim()
  if (!content) return null
  if (END_DIRECTIVE_RE.test(content)) {
    return {type: 'end'}
  }
  const match = content.match(FOR_DIRECTIVE_RE)
  if (!match) return null
  return {
    type: 'for',
    alias: match[1],
    listExpr: match[2],
  }
}

const getCellTextCandidates = (cell = {}) => {
  const out = []
  if (typeof cell?.v === 'string') out.push(cell.v)
  if (typeof cell?.m === 'string') out.push(cell.m)
  if (typeof cell?.p?.body?.dataStream === 'string') out.push(cell.p.body.dataStream)
  return out
}

const extractExpressions = (text = '') => {
  const out = []
  const raw = String(text ?? '')
  let match
  while ((match = EXPRESSION_RE.exec(raw)) !== null) {
    const expr = String(match[1] || '').trim()
    if (expr) out.push(expr)
  }
  EXPRESSION_RE.lastIndex = 0
  return out
}

const isLegacyColumnExpression = (expr = '') => {
  if (LEGACY_COLUMN_EXPRESSION_SET.has(expr)) {
    return true
  }
  const dotIndex = expr.indexOf('.')
  if (dotIndex <= 0 || dotIndex >= expr.length - 1) {
    return false
  }
  const right = expr.slice(dotIndex + 1).trim()
  return COLUMN_FIELD_SET.has(right)
}

const findForEndIndex = (items = [], fromIndex, getDirective) => {
  let depth = 0
  for (let i = fromIndex; i < items.length; i++) {
    const directive = getDirective(items[i])
    if (!directive) continue
    if (directive.type === 'for') {
      depth += 1
      continue
    }
    if (directive.type === 'end') {
      if (depth === 0) return i
      depth -= 1
    }
  }
  return -1
}

const normalizeLoopItem = (value, listExpr, index) => {
  if (/(^|\.)columnList$/.test(listExpr)) {
    return normalizeColumn(value || {}, index)
  }
  return value
}

const textHasLegacyColumnExpression = (text = '') => {
  const exprList = extractExpressions(text)
  for (let i = 0; i < exprList.length; i++) {
    if (isLegacyColumnExpression(exprList[i])) {
      return true
    }
  }
  return false
}

const renderTemplateItems = (items = [], context = {}, options = {}) => {
  const {
    getDirective,
    renderItem,
  } = options

  const output = []
  let i = 0
  while (i < items.length) {
    const item = items[i]
    const directive = getDirective(item)

    if (directive?.type === 'for') {
      const endIndex = findForEndIndex(items, i + 1, getDirective)
      if (endIndex < 0) {
        const fallback = renderItem(item, context)
        if (Array.isArray(fallback)) {
          output.push(...fallback)
        } else if (fallback !== null && fallback !== undefined) {
          output.push(fallback)
        }
        i += 1
        continue
      }

      const blockItems = items.slice(i + 1, endIndex)
      const source = resolvePath(context, directive.listExpr)
      const list = Array.isArray(source) ? source : []
      if (list.length) {
        list.forEach((entry, idx) => {
          const itemCtx = buildLoopContext(
            context,
            directive.alias,
            normalizeLoopItem(entry, directive.listExpr, idx + 1),
            idx + 1
          )
          const rendered = renderTemplateItems(blockItems, itemCtx, options)
          output.push(...rendered)
        })
      }
      i = endIndex + 1
      continue
    }

    if (directive?.type === 'end') {
      i += 1
      continue
    }

    const rendered = renderItem(item, context)
    if (Array.isArray(rendered)) {
      output.push(...rendered)
    } else if (rendered !== null && rendered !== undefined) {
      output.push(rendered)
    }
    i += 1
  }
  return output
}

const renderWorkbookRow = (rowObj = {}, context = {}) => {
  const rendered = {}
  Object.keys(rowObj).forEach((colKey) => {
    const sourceCell = rowObj[colKey] || {}
    const cell = deepClone(sourceCell)
    if (typeof cell.v === 'string') {
      cell.v = renderText(cell.v, context)
    }
    if (typeof cell.m === 'string') {
      cell.m = renderText(cell.m, context)
    }
    if (typeof cell?.p?.body?.dataStream === 'string') {
      cell.p.body.dataStream = renderText(cell.p.body.dataStream, context)
    }
    rendered[colKey] = cell
  })
  return rendered
}

const getWorkbookRowDirective = (rowObj = {}) => {
  const colKeys = Object.keys(rowObj)
  if (!colKeys.length) return null
  let directive = null

  for (let i = 0; i < colKeys.length; i++) {
    const cell = rowObj[colKeys[i]] || {}
    const textList = getCellTextCandidates(cell)
    if (!textList.length) {
      continue
    }
    for (let j = 0; j < textList.length; j++) {
      const trimmed = String(textList[j] ?? '').trim()
      if (!trimmed) continue
      const parsed = parseDirectiveText(trimmed)
      if (!parsed) {
        return null
      }
      if (!directive) {
        directive = parsed
        continue
      }
      const sameDirective =
        directive.type === parsed.type &&
        directive.alias === parsed.alias &&
        directive.listExpr === parsed.listExpr
      if (!sameDirective) {
        return null
      }
    }
  }

  return directive
}

const rowHasLegacyColumnExpression = (rowObj = {}) => {
  return Object.keys(rowObj).some((colKey) => {
    const cell = rowObj[colKey] || {}
    return textHasLegacyColumnExpression(cell?.v)
      || textHasLegacyColumnExpression(cell?.m)
      || textHasLegacyColumnExpression(cell?.p?.body?.dataStream)
  })
}

const toInt = (value) => {
  if (value === null || value === undefined || value === '') return null
  const num = Number(value)
  if (!Number.isFinite(num)) return null
  return Math.trunc(num)
}

const buildRemappedMerge = (mergeObj, startRow, endRow, startColumn, endColumn) => {
  const out = deepClone(mergeObj || {})
  out.rangeType = out.rangeType ?? 0
  out.startRow = startRow
  out.endRow = endRow
  out.startColumn = startColumn
  out.endColumn = endColumn
  return out
}

const remapMergeDataForBlock = (templateMergeData = [], renderedRows = [], baseRow = 0) => {
  if (!Array.isArray(templateMergeData) || !templateMergeData.length) return []
  if (!Array.isArray(renderedRows) || !renderedRows.length) return []

  const rowPositionMap = new Map()
  renderedRows.forEach((entry, localRow) => {
    const src = toInt(entry?.sourceRowIndex)
    if (src === null) return
    if (!rowPositionMap.has(src)) {
      rowPositionMap.set(src, [])
    }
    rowPositionMap.get(src).push(localRow)
  })

  const out = []
  templateMergeData.forEach((mergeObj) => {
    const rawStartRow = toInt(mergeObj?.startRow)
    const rawEndRow = toInt(mergeObj?.endRow)
    const rawStartCol = toInt(mergeObj?.startColumn)
    const rawEndCol = toInt(mergeObj?.endColumn)
    if ([rawStartRow, rawEndRow, rawStartCol, rawEndCol].some((v) => v === null)) return

    const startRow = Math.min(rawStartRow, rawEndRow)
    const endRow = Math.max(rawStartRow, rawEndRow)
    const startColumn = Math.min(rawStartCol, rawEndCol)
    const endColumn = Math.max(rawStartCol, rawEndCol)

    if (startRow === endRow) {
      const rowList = rowPositionMap.get(startRow) || []
      rowList.forEach((localRow) => {
        const absRow = baseRow + localRow
        out.push(buildRemappedMerge(mergeObj, absRow, absRow, startColumn, endColumn))
      })
      return
    }

    const filtered = []
    renderedRows.forEach((entry, localRow) => {
      const src = toInt(entry?.sourceRowIndex)
      if (src === null) return
      if (src < startRow || src > endRow) return
      filtered.push({localRow, sourceRow: src})
    })

    if (!filtered.length) return

    let groupStart = 0
    for (let i = 1; i <= filtered.length; i++) {
      const prev = filtered[i - 1]
      const curr = filtered[i]
      const shouldSplit = i === filtered.length
        || curr.localRow !== prev.localRow + 1
        || curr.sourceRow < prev.sourceRow
      if (!shouldSplit) continue

      const group = filtered.slice(groupStart, i)
      const hasStart = group.some(item => item.sourceRow === startRow)
      const hasEnd = group.some(item => item.sourceRow === endRow)
      if (hasStart && hasEnd) {
        out.push(buildRemappedMerge(
          mergeObj,
          baseRow + group[0].localRow,
          baseRow + group[group.length - 1].localRow,
          startColumn,
          endColumn
        ))
      }
      groupStart = i
    }
  })

  return out
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
  const lines = String(templateContent).split(/\r?\n/)

  const blocks = tableInfoList.map((table, tableIndex) => {
    const tableCtx = buildTableContext(table, tableIndex + 1)
    const renderedLines = renderTemplateItems(lines, tableCtx, {
      getDirective: (line) => parseDirectiveText(line),
      renderItem: (line, ctx) => {
        if (!ctx._inFor && textHasLegacyColumnExpression(line)) {
          const columns = Array.isArray(ctx.columnList) ? ctx.columnList : []
          if (!columns.length) {
            return [renderText(line, ctx)]
          }
          return columns.map((column, columnIndex) => {
            const columnCtx = buildLoopContext(ctx, 'col', column, columnIndex + 1)
            return renderText(line, columnCtx)
          })
        }
        return renderText(line, ctx)
      },
    })
    return renderedLines.join('\n')
  })

  return blocks.join('\n\n')
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
  const result = deepClone(workbook || {})
  const sheets = result?.sheets || {}

  Object.keys(sheets).forEach((sheetId) => {
    const sheet = sheets[sheetId]
    const cellData = sheet?.cellData || {}
    const templateMergeData = Array.isArray(sheet?.mergeData) ? deepClone(sheet.mergeData) : []
    const rowIndexes = Object.keys(cellData).map(Number).filter((idx) => !Number.isNaN(idx)).sort((a, b) => a - b)
    const templateRows = rowIndexes.map((rowIndex) => {
      return {
        rowIndex,
        rowObj: cellData[String(rowIndex)] || {},
      }
    })
    const renderedCellData = {}
    const renderedMergeData = []
    let nextRow = 0

    tableInfoList.forEach((table, tableIndex) => {
      const tableCtx = buildTableContext(table, tableIndex + 1)
      const blockStartRow = nextRow
      const renderedRows = renderTemplateItems(templateRows, tableCtx, {
        getDirective: (rowItem) => getWorkbookRowDirective(rowItem?.rowObj || {}),
        renderItem: (rowItem, ctx) => {
          const rowObj = rowItem?.rowObj || {}
          if (!ctx._inFor && rowHasLegacyColumnExpression(rowObj)) {
            const columns = Array.isArray(ctx.columnList) ? ctx.columnList : []
            if (!columns.length) {
              return [{
                sourceRowIndex: rowItem?.rowIndex,
                rowObj: renderWorkbookRow(rowObj, ctx),
              }]
            }
            return columns.map((column, columnIndex) => {
              const columnCtx = buildLoopContext(ctx, 'col', column, columnIndex + 1)
              return {
                sourceRowIndex: rowItem?.rowIndex,
                rowObj: renderWorkbookRow(rowObj, columnCtx),
              }
            })
          }
          return [{
            sourceRowIndex: rowItem?.rowIndex,
            rowObj: renderWorkbookRow(rowObj, ctx),
          }]
        },
      })
      renderedRows.forEach((entry) => {
        renderedCellData[String(nextRow)] = entry?.rowObj || {}
        nextRow += 1
      })
      renderedMergeData.push(...remapMergeDataForBlock(templateMergeData, renderedRows, blockStartRow))
      if (tableIndex < tableInfoList.length - 1) {
        nextRow += 1
      }
    })

    sheet.cellData = renderedCellData
    sheet.rowCount = Math.max(100, nextRow + 10)
    sheet.mergeData = renderedMergeData
  })

  return result
}
