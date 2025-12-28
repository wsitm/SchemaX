<template>
  <div :key="key" ref="containerRef" class="univer-content"/>
</template>

<script setup>
import '@univerjs/preset-sheets-core/lib/index.css'
import {createUniver, FUniver, LocaleType, mergeLocales, Univer} from '@univerjs/presets'
import {UniverSheetsCorePreset} from '@univerjs/preset-sheets-core'
import UniverPresetSheetsCoreZhCN from '@univerjs/preset-sheets-core/locales/zh-CN'
import {onBeforeUnmount, onMounted, ref, watch} from 'vue'
import XEUtils from "xe-utils";

import {DEFAULT_WORKBOOK_DATA} from "./sheet-data";

const key = ref(new Date().getTime())
const containerRef = ref(null)
let univerInstance = null // Type of Univer
let univerAPIInstance = null // Type of FUniver

const props = defineProps({
  worksheetData: Object
})

const updateWorksheetData = XEUtils.debounce((value) => {
  console.log("watch-data", value)

  if (univerAPIInstance) {
    const workbook = univerAPIInstance.getActiveWorkbook();
    if (workbook) {
      const worksheet = workbook.getActiveSheet();

      if (!value.rowCount) {
        value.rowCount = 100;
      }

      try {
        // console.log("getLastRow", worksheet.getLastRow())
        if (worksheet.getLastRow() > 0) {
          worksheet.deleteRows(0, worksheet.getLastRow() + 1);
        }
        worksheet.insertRows(0, value.rowCount + 1);
      } catch (e) {
        console.error(e)
      }

      const range = worksheet.getRange("A1:J" + value.rowCount)
      range.setValues(value.cellData || {});

      if (value.mergeData) {
        for (let i = 0; i < value.mergeData.length; i++) {
          const merge = value.mergeData[i]
          const mr = worksheet.getRange("A" + (merge.startRow + 1) + ":J" + (merge.endRow + 1))
          mr.merge();
          // console.log("merge", mr.isMerged())
        }
      }

      // 通过 FRange 设置选区 A1:A1
      const range2 = worksheet.getRange('A1:A1')
      range2.activate()
    }
  }
}, 200)

watch(() => props.worksheetData, (value) => {
  updateWorksheetData(value)
}, {deep: true, immediate: true})

function getData() {
  if (!univerAPIInstance) {
    throw new Error('未初始化')
  }
  const workbook = univerAPIInstance.getActiveWorkbook();
  if (!workbook) {
    throw new Error('未初始化')
  }
  return workbook.save()
}

onMounted(() => {
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
  // console.log(props.workbookData)
  univerAPI.createWorkbook(DEFAULT_WORKBOOK_DATA)
  univerInstance = univer
  univerAPIInstance = univerAPI
})

onBeforeUnmount(() => {
  univerInstance?.dispose()
  univerAPIInstance?.dispose()
  univerInstance = null
  univerAPIInstance = null
})

defineExpose({
  getData: getData
})
</script>

<style scoped>
.univer-content {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
