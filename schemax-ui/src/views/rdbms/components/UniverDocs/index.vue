<template>
  <div :key="key" ref="containerRef" class="univer-content"/>
</template>

<script setup>
import '@univerjs/preset-docs-core/lib/index.css'
import {createUniver, FUniver, LocaleType, mergeLocales, Univer} from '@univerjs/presets'
import {UniverDocsCorePreset} from '@univerjs/preset-docs-core'
import UniverPresetDocsCoreZhCN from '@univerjs/preset-docs-core/locales/zh-CN'
import {onBeforeUnmount, onMounted, ref, watch} from 'vue'
import XEUtils from "xe-utils";

import {DEFAULT_DOCS_DATA} from "./docs-data";

const key = ref(new Date().getTime())
const containerRef = ref(null)
let univerInstance = null // Type of Univer
let univerAPIInstance = null // Type of FUniver

const props = defineProps({
  workDocsData: Object
})

const updateWorkDocsData = XEUtils.debounce((value) => {
  // console.log("watch-data", value)
  if (univerAPIInstance) {

  }
}, 200)

watch(() => props.workDocsData, (value) => {
  updateWorkDocsData(value)
}, {deep: true, immediate: true})

onMounted(() => {
  const {univer, univerAPI} = createUniver({
    locale: LocaleType.ZH_CN,
    locales: {
      [LocaleType.ZH_CN]: mergeLocales(
        UniverPresetDocsCoreZhCN,
      ),
    },
    presets: [
      UniverDocsCorePreset({
        container: containerRef.value,
      }),
    ],
  })

  univerAPI.createUniverDoc({})
  univerInstance = univer
  univerAPIInstance = univerAPI
})

onBeforeUnmount(() => {
  univerInstance?.dispose()
  univerAPIInstance?.dispose()
  univerInstance = null
  univerAPIInstance = null
})


function getData() {
  if (!univerAPIInstance) {
    throw new Error('未初始化')
  }
  const document = univerAPIInstance.getActiveDocument();
  if (!document) {
    throw new Error('未初始化')
  }
  return document.getSnapshot();
}


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
