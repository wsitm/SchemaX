<template>
  <div ref="container" class="univer-content"/>
</template>

<script setup>
import '@univerjs/preset-sheets-core/lib/index.css'
import {createUniver, FUniver, LocaleType, mergeLocales, Univer} from '@univerjs/presets'
import {UniverSheetsCorePreset} from '@univerjs/preset-sheets-core'
import UniverPresetSheetsCoreZhCN from '@univerjs/preset-sheets-core/locales/zh-CN'
import {onBeforeUnmount, onMounted, ref} from 'vue'
import XEUtils from "xe-utils";

import {DEFAULT_WORKBOOK_DATA} from "./sheet-data";

const container = ref(null)
let univerInstance = null
let univerAPIInstance = null

const props = defineProps({
  workbookData: {
    type: Object,
    default: () => {
      return {...DEFAULT_WORKBOOK_DATA}
    }
  }
})

const updateWorkbookData = XEUtils.debounce((value) => {
  if (univerAPIInstance) {
    univerAPIInstance.createWorkbook({workbookData: value})
  }
})

watch(props.workbookData, (value) => {
  updateWorkbookData(value)
})


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
  univerAPI.createWorkbook({workbookData: props.workbookData})
  univerInstance = univer
  univerAPIInstance = univerAPI
})

onBeforeUnmount(() => {
  univerInstance?.dispose()
  univerAPIInstance?.dispose()
  univerInstance = null
  univerAPIInstance = null
})
</script>

<style scoped>
.univer-content {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
