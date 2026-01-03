<template>
  <div ref="containerRef" class="univer-content" />
</template>

<script setup name="UniverSheet">
import '@univerjs/preset-sheets-core/lib/index.css'
import { createUniver, LocaleType, mergeLocales } from '@univerjs/presets'
import {UniverSheetsCorePreset} from '@univerjs/preset-sheets-core'
import UniverPresetSheetsCoreZhCN from '@univerjs/preset-sheets-core/locales/zh-CN'
import { DEFAULT_WORKBOOK_DATA } from './sheet-data'
import {ICommandService} from '@univerjs/core'
import {SetRangeValuesCommand} from '@univerjs/sheets'
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import XEUtils from 'xe-utils'

const props = defineProps({
  // 支持完整的 workbook 数据（推荐使用）
  workbookData: {
    type: Object,
    default: null
  },
  // 向后兼容：如果传了 worksheetData，会转换为 workbook 数据
  worksheetData: {
    type: Object,
    default: null
  }
})

const containerRef = ref(null)
let univerInstance = null
let univerAPIInstance = null
let dropDisposable = null
const dragText = ref(null)

/**
 * 获取 workbook 数据
 * 优先使用 workbookData，其次使用 worksheetData（向后兼容）
 */
function getWorkbookData() {
  if (props.workbookData) {
    return props.workbookData
  }
  // 向后兼容：如果传的是 worksheetData，转换为完整的 workbook 数据
  if (props.worksheetData) {
    const base = XEUtils.clone(DEFAULT_WORKBOOK_DATA, true)
    return {
      ...base,
      sheets: {
        'sheet-01': {
          ...base.sheets['sheet-01'],
          ...props.worksheetData,
        },
      },
    }
  }
  // 重要：不能直接返回 DEFAULT_WORKBOOK_DATA（它是模块级单例对象），否则多个页面/多个组件实例会共享同一份默认数据引用。
  // 必须深拷贝一份，确保实例隔离。
  return XEUtils.clone(DEFAULT_WORKBOOK_DATA, true)
}

/**
 * 清理 Univer 实例
 * @param {boolean} setDestroyed 是否设置销毁标志（组件真正销毁时设置为 true）
 */
function cleanupUniver() {
  if (dropDisposable) {
    dropDisposable.dispose()
  }
  if (univerInstance) {
    try {
      univerInstance.dispose()
    } catch (e) {
      console.warn('Dispose univer failed:', e)
    }
  }
  univerInstance = null
  univerAPIInstance = null
}

/**
 * 初始化 Univer
 */
function initUniver() {
  if (!containerRef.value) {
    throw new Error('Univer container is not ready')
  }

  const { univer, univerAPI } = createUniver({
    locale: LocaleType.ZH_CN,
    locales: {
      [LocaleType.ZH_CN]: mergeLocales(UniverPresetSheetsCoreZhCN),
    },
    presets: [
      UniverSheetsCorePreset({
        container: containerRef.value,
      }),
    ],
  })

  univerInstance = univer
  univerAPIInstance = univerAPI
}


/**
 * 创建或更新 workbook
 */
function bindWorkbookEvents() {
  if (dropDisposable) {
    dropDisposable.dispose()
    dropDisposable = null
  }

  const workbook = univerAPIInstance?.getActiveWorkbook()
  if (!workbook) return

  // 单元格 drop 事件（用于拖拽填充）
  dropDisposable = workbook.onDrop((params) => {
    const { unitId, subUnitId, row, col } = params?.location || {}
    insertText(unitId, subUnitId, row, col)
  })
}

function createOrReplaceWorkbook(workbookData) {
  if (!univerAPIInstance || !workbookData) return

  // 简单粗暴策略：更新即“重建 workbook”，但不重建 Univer 实例。
  // 注意：univerAPI.createWorkbook 会以 workbookData.id 作为 unitId，重复会报错。
  // 因此这里为每次重建生成唯一 id，避免 unitId 冲突。

  // 释放旧 workbook 上的事件监听
  if (dropDisposable) {
    dropDisposable.dispose()
    dropDisposable = null
  }

  // 尝试销毁当前 active workbook（不同版本可能无此 API，做兼容兜底）
  const active = univerAPIInstance.getActiveWorkbook?.()
  if (active) {
    try {
      active.dispose?.()
      active.destroy?.()
    } catch (e) {
      console.warn('Dispose active workbook failed:', e)
    }
  }

  // 避免外部继续持有并复用同一份对象引用，这里也做一次深拷贝再喂给 Univer
  const nextWorkbookData = XEUtils.clone(workbookData, true)
  const baseId = nextWorkbookData.id || 'workbook'
  nextWorkbookData.id = `${baseId}-${Date.now()}`

  univerAPIInstance.createWorkbook(nextWorkbookData)
  bindWorkbookEvents()
}

/**
 * 获取当前 workbook 数据
 * 使用 prop+emit 方式，避免频繁渲染
 */
function getData() {
  if (!univerAPIInstance) {
    throw new Error('Univer 未初始化或已销毁')
  }
  try {
    const workbook = univerAPIInstance.getActiveWorkbook()
    if (!workbook) {
      throw new Error('Workbook 未初始化')
    }

    // 使用 workbook.save() 获取序列化的 workbook 数据
    return workbook.save()
  } catch (e) {
    console.error('Get workbook data failed:', e)
    throw e
  }
}

function setDragText(text) {
  dragText.value = text
}

/**
 * 插入文本到指定单元格
 */
async function insertText(unitId, subUnitId, row = null, column = null) {
  // try {
  // 构建新值
  const newValue = dragText.value;
  if (!newValue) {
    return;
  }

  let startRow = row !== null ? row : 0
  let startColumn = column !== null ? column : 0

  // 优先使用命令方式设置值（支持撤销/重做）
  try {
    const injector = univerInstance.__getInjector()
    const commandService = injector.get(ICommandService)
    if (commandService && SetRangeValuesCommand) {
      // 使用 SetRangeValuesCommand 设置值
      const result = await commandService.executeCommand(
        SetRangeValuesCommand.id,
        {
          unitId: unitId,
          subUnitId: subUnitId,
          range: {
            startRow,
            startColumn,
            endRow: startRow,
            endColumn: startColumn
          },
          value: {
            [startRow]: {
              [startColumn]: {v: newValue, m: newValue}
            }
          }
        }
      )

      if (result) {
        dragText.value = null
      }
    }
  } catch (e) {
    console.error('插入单元格失败:', e)
  }

}

// 最新一次收到的数据（用于在初始化完成后补一次渲染）
let pendingWorkbookData = null

const applyWorkbookDebounced = XEUtils.debounce((workbookData) => {
  pendingWorkbookData = workbookData
  if (!univerAPIInstance) return
  createOrReplaceWorkbook(workbookData)
}, 200)

// 监听 workbookData 变化
watch(
  () => props.workbookData,
  (newValue) => {
    const hasData = newValue && Object.keys(newValue).length > 0
    if (hasData) {
      applyWorkbookDebounced(newValue)
    }
  },
  { deep: true }
)

// 向后兼容：监听 worksheetData 变化
watch(
  () => props.worksheetData,
  (newValue) => {
    const hasData = newValue && Object.keys(newValue).length > 0
    if (!hasData) return

    // 向后兼容：worksheetData -> workbookData
    const base = XEUtils.clone(DEFAULT_WORKBOOK_DATA, true)
    const workbookData = {
      ...base,
      sheets: {
        'sheet-01': {
          ...base.sheets['sheet-01'],
          ...newValue,
        },
      },
    }

    applyWorkbookDebounced(workbookData)
  },
  { deep: true, immediate: false }
)


onMounted(async () => {
  // 确保容器已挂载且有尺寸（页面流 / dialog 下都更稳）
  await nextTick()

  initUniver()

  // 首次渲染：优先取当前 props 组合后的数据
  const workbookData = getWorkbookData()
  pendingWorkbookData = workbookData
  createOrReplaceWorkbook(workbookData)
})

onBeforeUnmount(() => {
  // 清理 univer 实例（会自动清理所有 workbook）
  // 使用 requestAnimationFrame 确保所有渲染操作完成后再清理
  requestAnimationFrame(() => {
    cleanupUniver()
  })
})

defineExpose({
  getData,
  setDragText
})
</script>

<style scoped>
.univer-content {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
