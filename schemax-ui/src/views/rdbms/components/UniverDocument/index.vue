<template>
  <div ref="containerRef" class="univer-document"/>
</template>

<script setup>
// @deprecated Word模板已由TinyMCE接管，保留该组件供未来Univer Docs增强后切回。
import '@univerjs/preset-sheets-core/lib/index.css'
import {createUniver, LocaleType, mergeLocales} from '@univerjs/presets'
import {UniverSheetsCorePreset} from '@univerjs/preset-sheets-core'
import UniverPresetSheetsCoreZhCN from '@univerjs/preset-sheets-core/locales/zh-CN'
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import XEUtils from 'xe-utils'
import {DEFAULT_DOCS_DATA} from '../UniverDocs/docs-data'
import {applyUniverFacadeCompatibilityPatch} from '../univerFacadeCompat'

applyUniverFacadeCompatibilityPatch()

const props = defineProps({
  documentData: {
    type: Object,
    default: null,
  },
  readonly: {
    type: Boolean,
    default: false,
  },
})

const containerRef = ref(null)
let mounted = false
let univerInstance = null
let univerAPIInstance = null

function cloneDocumentData() {
  const source = props.documentData || DEFAULT_DOCS_DATA
  const snapshot = XEUtils.clone(source, true)
  snapshot.id = `${snapshot.id || 'word-template'}-${Date.now()}`
  snapshot.disabled = props.readonly
  return snapshot
}

function cleanup() {
  try {
    univerAPIInstance?.dispose()
  } catch (error) {
    console.warn('销毁Word编辑器 API 实例失败：', error)
  }
  try {
    univerInstance?.dispose()
  } catch (error) {
    console.warn('销毁Word编辑器失败：', error)
  }
  univerInstance = null
  univerAPIInstance = null
}

function init() {
  if (!containerRef.value) return
  cleanup()
  // Univer 0.25.1 的门面扩展是全局的，使用 Sheets 预设补齐同页 Excel/Word 场景依赖。
  const {univer, univerAPI} = createUniver({
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
  univerAPI.createUniverDoc(cloneDocumentData())
}

async function rebuild() {
  if (!mounted) return
  await nextTick()
  init()
}

function getData() {
  const document = univerAPIInstance?.getActiveDocument()
  if (!document) {
    throw new Error('Word编辑器尚未初始化')
  }
  const snapshot = XEUtils.clone(document.getSnapshot(), true)
  snapshot.disabled = false
  return snapshot
}

async function insertText(text) {
  if (props.readonly || !text) return false
  const document = univerAPIInstance?.getActiveDocument()
  if (!document) {
    throw new Error('Word编辑器尚未初始化')
  }
  return document.insertText(text)
}

watch(() => props.documentData, rebuild)
watch(() => props.readonly, rebuild)

onMounted(() => {
  mounted = true
  init()
})

onBeforeUnmount(() => {
  mounted = false
  cleanup()
})

defineExpose({
  getData,
  insertText,
})
</script>

<style scoped>
.univer-document {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #f3f4f6;
}
</style>
