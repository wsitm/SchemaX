<template>
  <div :key="key" ref="containerRef" class="univer-content"/>
</template>

<script setup name="UniverSheet">
import '@univerjs/preset-sheets-core/lib/index.css'
import {createUniver, FUniver, LocaleType, mergeLocales} from '@univerjs/presets'
import {UniverSheetsCorePreset} from '@univerjs/preset-sheets-core'
import UniverPresetSheetsCoreZhCN from '@univerjs/preset-sheets-core/locales/zh-CN'
import {DEFAULT_WORKBOOK_DATA} from "./sheet-data";
import {ICommandService} from '@univerjs/core'
import {SetRangeValuesCommand} from '@univerjs/sheets'
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import XEUtils from "xe-utils";

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
  key.value = new Date().getTime();
  univerInstance = univer
  univerAPIInstance = univerAPI
}


/**
 * 创建或更新 workbook
 */
function createOrUpdateWorkbook(workbookData) {
  if (!univerAPIInstance || !workbookData) {
    return
  }
  // 创建新的 workbook
  console.log('createWorkbook', workbookData)
  univerAPIInstance.createWorkbook(workbookData)

  // 添加单元格指针释放事件监听
  dropDisposable = univerAPIInstance.getActiveWorkbook().onDrop((params) => {
    const {unitId, subUnitId, row, col} = params?.location;
    // 触发自定义事件
    console.log('onDrop', params)
    insertText(unitId, subUnitId, row, col);
  });
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

const reCreateUniver = XEUtils.debounce((workbookData) => {
  cleanupUniver();
  initUniver();
  nextTick(() => {
    // 强制更新：即使 id 相同也重建（避免渲染为空白）
    createOrUpdateWorkbook(workbookData, true)
  })
}, 200);

// 监听 workbookData 变化
watch(() => props.workbookData, (newValue, oldValue) => {
  if (!univerAPIInstance) return
  const hasData = newValue && Object.keys(newValue).length > 0
  if (hasData) {
    reCreateUniver(newValue);
  }
}, {deep: true})

// 向后兼容：监听 worksheetData 变化
watch(() => props.worksheetData, (newValue, oldValue) => {
  if (!univerAPIInstance) return

  // 检查数据是否真的发生了变化
  const hasData = newValue && Object.keys(newValue).length > 0

  // 如果从无数据变为有数据，或者数据发生变化，需要强制更新
  if (hasData) {
    const workbookData = {
      ...DEFAULT_WORKBOOK_DATA,
      sheets: {
        'sheet-01': {
          ...DEFAULT_WORKBOOK_DATA.sheets['sheet-01'],
          ...newValue
        }
      }
    }
    reCreateUniver(workbookData);
  } else {
    console.warn('worksheetData is empty')
  }
}, {deep: true, immediate: false})


onMounted(() => {
  const workbookData = getWorkbookData()
  reCreateUniver(workbookData);
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
