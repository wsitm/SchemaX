<template>
  <div :key="key" ref="containerRef" class="univer-content"/>
</template>

<script setup name="UniverSheet">
import '@univerjs/preset-sheets-core/lib/index.css'
import {createUniver, FUniver, LocaleType, mergeLocales} from '@univerjs/presets'
import {UniverSheetsCorePreset} from '@univerjs/preset-sheets-core'
import UniverPresetSheetsCoreZhCN from '@univerjs/preset-sheets-core/locales/zh-CN'
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {DEFAULT_WORKBOOK_DATA} from "./sheet-data";
import {ICommandService} from '@univerjs/core'
import {SetRangeValuesCommand} from '@univerjs/sheets'

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

const key = ref(new Date().getTime())
const containerRef = ref(null)
let univerInstance = null
let univerAPIInstance = null
let dropDisposable = null
let isDestroyed = false
const dragText = ref(null);

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
    return {
      ...DEFAULT_WORKBOOK_DATA,
      sheets: {
        'sheet-01': {
          ...DEFAULT_WORKBOOK_DATA.sheets['sheet-01'],
          ...props.worksheetData
        }
      }
    }
  }
  return DEFAULT_WORKBOOK_DATA
}

/**
 * 清理 Univer 实例
 * @param {boolean} setDestroyed 是否设置销毁标志（组件真正销毁时设置为 true）
 */
function cleanupUniver(setDestroyed = false) {
  if (univerInstance) {
    try {
      univerInstance.dispose()
    } catch (e) {
      console.warn('Dispose univer failed:', e)
    }
  }
  univerInstance = null
  univerAPIInstance = null
  // currentWorkbookId = null
  if (setDestroyed) {
    isDestroyed = true
  }
  if (dropDisposable) {
    dropDisposable.dispose()
  }
}

/**
 * 创建或更新 workbook
 */
function createOrUpdateWorkbook(workbookData, forceUpdate = false) {
  if (isDestroyed || !univerAPIInstance || !workbookData) {
    return
  }

  try {
    const workbook = univerAPIInstance.getActiveWorkbook()

    // 如果已有 workbook
    if (workbook) {
      // 如果强制更新或 workbook ID 不同，通过改变 key 来强制重新创建整个组件
      if (forceUpdate || workbook.getId() !== workbookData.id) {
        // 重置引用，防止后续操作访问已销毁的实例
        // 注意：不在这里清理，让 onBeforeUnmount 来处理清理
        univerInstance = null
        univerAPIInstance = null
        // currentWorkbookId = null

        // 改变 key 会触发组件重新创建，onBeforeUnmount 会自动清理旧实例
        // 使用 nextTick 确保当前操作完成后再改变 key
        nextTick(() => {
          if (!isDestroyed) {
            key.value = new Date().getTime()
          }
        })
        return
      }
      // 如果 ID 相同且不强制更新，不处理（避免不必要的重新创建）
      return
    }

    // 创建新的 workbook
    univerAPIInstance.createWorkbook(workbookData)
    // currentWorkbookId = workbookData.id
  } catch (e) {
    console.error('Create workbook failed:', e)
  }
}

/**
 * 获取当前 workbook 数据
 * 使用 prop+emit 方式，避免频繁渲染
 */
function getData() {
  if (isDestroyed || !univerAPIInstance) {
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

// 监听 workbookData 变化
watch(() => props.workbookData, (newValue, oldValue) => {
  if (isDestroyed || !univerAPIInstance) return

  // 如果数据从 null 变为有值，或者 workbook ID 发生变化，需要重新创建
  if (newValue && (!oldValue || oldValue.id !== newValue.id)) {
    nextTick(() => {
      if (!isDestroyed) {
        createOrUpdateWorkbook(newValue)
      }
    })
  }
}, {deep: true})

// 向后兼容：监听 worksheetData 变化
watch(() => props.worksheetData, (newValue, oldValue) => {
  if (isDestroyed || !univerAPIInstance) return

  // 检查数据是否真的发生了变化
  const hasData = newValue && Object.keys(newValue).length > 0
  const hadData = oldValue && Object.keys(oldValue).length > 0

  // 如果从无数据变为有数据，或者数据发生变化，需要强制更新
  if (hasData) {
    // 检查数据内容是否真的变化了（通过序列化比较关键字段）
    // 只比较 cellData 和 mergeData，避免其他字段变化导致不必要的更新
    const newCellData = JSON.stringify(newValue.cellData || {})
    const newMergeData = JSON.stringify(newValue.mergeData || [])
    const oldCellData = oldValue ? JSON.stringify(oldValue.cellData || {}) : ''
    const oldMergeData = oldValue ? JSON.stringify(oldValue.mergeData || []) : ''

    if (newCellData !== oldCellData || newMergeData !== oldMergeData) {
      nextTick(() => {
        if (!isDestroyed) {
          const workbookData = {
            ...DEFAULT_WORKBOOK_DATA,
            sheets: {
              'sheet-01': {
                ...DEFAULT_WORKBOOK_DATA.sheets['sheet-01'],
                ...newValue
              }
            }
          }
          // 强制更新，确保数据正确加载
          createOrUpdateWorkbook(workbookData, true)
        }
      })
    }
  } else if (hadData && !hasData) {
    // 从有数据变为无数据，重置为默认 workbook
    nextTick(() => {
      if (!isDestroyed) {
        createOrUpdateWorkbook(DEFAULT_WORKBOOK_DATA, true)
      }
    })
  }
}, {deep: true, immediate: false})

onMounted(() => {
  // 重置销毁标志
  isDestroyed = false

  const {univer, univerAPI} = createUniver({
    locale: LocaleType.ZH_CN,
    locales: {
      [LocaleType.ZH_CN]: mergeLocales(
        UniverPresetSheetsCoreZhCN,
      ),
    },
    presets: [
      UniverSheetsCorePreset({
        container: containerRef.value,
      }),
    ],
  })

  univerInstance = univer
  univerAPIInstance = univerAPI

  // 初始化时创建 workbook
  nextTick(() => {
    if (isDestroyed) return

    const workbookData = getWorkbookData()
    // 检查是否有有效数据
    const hasValidData = props.workbookData ||
      (props.worksheetData && Object.keys(props.worksheetData).length > 0)

    if (hasValidData) {
      createOrUpdateWorkbook(workbookData)
    } else {
      // 如果没有数据，创建默认的 workbook（空表格）
      createOrUpdateWorkbook(DEFAULT_WORKBOOK_DATA)
    }

    // 添加单元格指针释放事件监听
    dropDisposable = univerAPIInstance.getActiveWorkbook().onDrop((params) => {
      const {unitId, subUnitId, row, col} = params?.location;
      // 触发自定义事件
      console.log('onDrop', params)
      insertText(unitId, subUnitId, row, col);
    });
  })
})

onBeforeUnmount(() => {
  // 标记为已销毁，防止后续操作
  isDestroyed = true

  // 清理 univer 实例（会自动清理所有 workbook）
  // 使用 requestAnimationFrame 确保所有渲染操作完成后再清理
  requestAnimationFrame(() => {
    cleanupUniver(true) // 设置销毁标志
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
