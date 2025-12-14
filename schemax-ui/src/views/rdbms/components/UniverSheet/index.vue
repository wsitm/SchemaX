<template>
  <div ref="container" class="univer-content"/>
</template>

<script setup>
import '@univerjs/preset-sheets-core/lib/index.css'
import {createUniver, FUniver, LocaleType, mergeLocales, Univer} from '@univerjs/presets'
import {UniverSheetsCorePreset} from '@univerjs/preset-sheets-core'
import UniverPresetSheetsCoreZhCN from '@univerjs/preset-sheets-core/locales/zh-CN'
import {onBeforeUnmount, onMounted, ref, watch} from 'vue'
import XEUtils from "xe-utils";

import {DEFAULT_WORKBOOK_DATA} from "./sheet-data";

const container = ref(null)
let univerInstance = null // Type of Univer
let univerAPIInstance = null // Type of FUniver

const props = defineProps({
  worksheetData: {
    type: Object,
    // default: () => {
    //   return {...DEFAULT_WORKBOOK_DATA}
    // }
  }
})

const updateWorksheetData = XEUtils.debounce((value) => {
  if (univerAPIInstance) {
    const workbook = univerAPIInstance.getActiveWorkbook();
    if (workbook) {
      workbook.setActiveSheet(value);
    }
  }
})

watch(props.worksheetData, (value) => {
  console.log('props.worksheetData', value)
  updateWorksheetData(value)
})

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
        container: container.value,
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
