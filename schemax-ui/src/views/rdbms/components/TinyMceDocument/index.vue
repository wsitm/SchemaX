<template>
  <div class="tiny-word-document" :class="{ 'is-readonly': readonly }">
    <template v-if="readonly">
      <div class="word-page">
        <header v-if="defaultHeaderHtml" class="word-header" v-html="defaultHeaderHtml"/>
        <main class="word-body" v-html="content.bodyHtml"/>
        <footer v-if="defaultFooterHtml" class="word-footer" v-html="defaultFooterHtml"/>
      </div>
    </template>
    <template v-else>
      <div class="word-toolbar">
        <el-select v-model="activeSection" class="section-select" aria-label="文档区域">
          <el-option v-for="item in sectionOptions" :key="item.value"
                     :label="item.label" :value="item.value"/>
        </el-select>
        <el-button :icon="Setting" title="页面设置" @click="openPageSettings">页面设置</el-button>
      </div>
      <div class="editor-shell">
        <textarea ref="editorTargetRef" class="editor-target" aria-label="Word 模板编辑区"/>
      </div>
    </template>

    <el-dialog v-model="pageDialogOpen" title="页面设置" width="560px"
               append-to-body :close-on-click-modal="false">
      <el-form label-width="96px">
        <el-form-item label="纸张方向">
          <el-radio-group v-model="pageDraft.orientation">
            <el-radio-button value="portrait">纵向</el-radio-button>
            <el-radio-button value="landscape">横向</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="纸张大小">
          <el-select v-model="pageSize" style="width: 180px">
            <el-option label="A4" value="A4"/>
            <el-option label="A3" value="A3"/>
            <el-option label="Letter" value="LETTER"/>
          </el-select>
        </el-form-item>
        <el-form-item label="默认字体">
          <el-select v-model="pageDraft.fontFamily" filterable allow-create style="width: 220px">
            <el-option label="微软雅黑" value="Microsoft YaHei"/>
            <el-option label="宋体" value="SimSun"/>
            <el-option label="黑体" value="SimHei"/>
            <el-option label="Arial" value="Arial"/>
          </el-select>
          <el-input-number v-model="pageDraft.fontSize" :min="5" :max="72"
                           :step="0.5" class="font-size-input"/>
          <span class="unit-label">磅</span>
        </el-form-item>
        <el-divider content-position="left">页边距（毫米）</el-divider>
        <div class="margin-grid">
          <el-form-item label="上">
            <el-input-number v-model="pageDraft.margins.top" :min="0" :max="100"/>
          </el-form-item>
          <el-form-item label="下">
            <el-input-number v-model="pageDraft.margins.bottom" :min="0" :max="100"/>
          </el-form-item>
          <el-form-item label="左">
            <el-input-number v-model="pageDraft.margins.left" :min="0" :max="100"/>
          </el-form-item>
          <el-form-item label="右">
            <el-input-number v-model="pageDraft.margins.right" :min="0" :max="100"/>
          </el-form-item>
          <el-form-item label="页眉">
            <el-input-number v-model="pageDraft.margins.header" :min="0" :max="100"/>
          </el-form-item>
          <el-form-item label="页脚">
            <el-input-number v-model="pageDraft.margins.footer" :min="0" :max="100"/>
          </el-form-item>
        </div>
        <el-form-item label="页眉页脚">
          <el-checkbox v-model="pageDraft.differentFirstPage">首页不同</el-checkbox>
          <el-checkbox v-model="pageDraft.differentOddEven">奇偶页不同</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pageDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="applyPageSettings">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch} from 'vue'
import {Setting} from '@element-plus/icons-vue'
import tinymce from 'tinymce/tinymce'

import 'tinymce/icons/default'
import 'tinymce/themes/silver'
import 'tinymce/models/dom'
import 'tinymce/plugins/advlist'
import 'tinymce/plugins/autolink'
import 'tinymce/plugins/charmap'
import 'tinymce/plugins/code'
import 'tinymce/plugins/fullscreen'
import 'tinymce/plugins/lists'
import 'tinymce/plugins/nonbreaking'
import 'tinymce/plugins/pagebreak'
import 'tinymce/plugins/preview'
import 'tinymce/plugins/searchreplace'
import 'tinymce/plugins/table'
import 'tinymce/plugins/visualblocks'
import 'tinymce/plugins/wordcount'
import 'tinymce/skins/ui/oxide/skin'
import 'tinymce/skins/ui/oxide/content'
import 'tinymce/skins/content/default/content'
import 'tinymce-i18n/langs8/zh-CN.js'

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

const createDefaultPage = () => ({
  size: 'A4',
  orientation: 'portrait',
  widthMm: 210,
  heightMm: 297,
  margins: {top: 20, bottom: 20, left: 25, right: 25, header: 10, footer: 10},
  differentFirstPage: false,
  differentOddEven: false,
  fontFamily: 'Microsoft YaHei',
  fontSize: 10.5,
})

const createDefaultContent = () => ({
  version: 2,
  editor: 'tinymce',
  format: 'html',
  bodyHtml: '<p></p>',
  page: createDefaultPage(),
  headers: {},
  footers: {},
})

const clone = (value) => JSON.parse(JSON.stringify(value))

const normalizeEditableHtml = (html) => {
  if (!html || typeof document === 'undefined') return html || ''
  const container = document.createElement('div')
  container.innerHTML = html
  container.querySelectorAll('[data-expression], .schemax-variable').forEach((node) => {
    const expression = node.getAttribute('data-expression') || node.textContent || ''
    node.replaceWith(document.createTextNode(expression))
  })
  container.querySelectorAll('.schemax-directive').forEach((node) => {
    node.classList.remove('schemax-directive', 'mceNonEditable')
    node.removeAttribute('contenteditable')
    if (!node.classList.length) node.removeAttribute('class')
  })
  return container.innerHTML
}

const normalizeContent = (value) => {
  const normalized = value?.editor === 'tinymce' ? clone(value) : createDefaultContent()
  normalized.bodyHtml = normalizeEditableHtml(normalized.bodyHtml || '<p></p>')
  normalized.page = {...createDefaultPage(), ...(normalized.page || {})}
  normalized.page.margins = {
    ...createDefaultPage().margins,
    ...(normalized.page.margins || {}),
  }
  normalized.headers ||= {}
  normalized.footers ||= {}
  Object.keys(normalized.headers).forEach((key) => {
    normalized.headers[key] = normalizeEditableHtml(normalized.headers[key])
  })
  Object.keys(normalized.footers).forEach((key) => {
    normalized.footers[key] = normalizeEditableHtml(normalized.footers[key])
  })
  return normalized
}

const content = ref(normalizeContent(props.documentData))
const activeSection = ref('body')
const editorTargetRef = ref(null)
const editorInstance = shallowRef(null)
const pageDialogOpen = ref(false)
const pageDraft = ref(createDefaultPage())
let isUnmounted = false

const sectionOptions = computed(() => [
  {label: '正文', value: 'body'},
  {label: '默认页眉', value: 'headers.default'},
  {label: '首页页眉', value: 'headers.first'},
  {label: '偶数页页眉', value: 'headers.even'},
  {label: '默认页脚', value: 'footers.default'},
  {label: '首页页脚', value: 'footers.first'},
  {label: '偶数页页脚', value: 'footers.even'},
])

const getSectionHtml = (section) => {
  if (section === 'body') return content.value.bodyHtml || '<p></p>'
  const [group, type] = section.split('.')
  return content.value[group]?.[type] || '<p></p>'
}

const setSectionHtml = (section, value) => {
  if (section === 'body') {
    content.value.bodyHtml = value
    return
  }
  const [group, type] = section.split('.')
  content.value[group] ||= {}
  content.value[group][type] = value
}

const defaultHeaderHtml = computed(() => content.value.headers?.default || '')
const defaultFooterHtml = computed(() => content.value.footers?.default || '')

const createExpressionText = (expression) => tinymce.DOM.encode(expression)

const editorOptions = {
  license_key: 'gpl',
  language: 'zh-CN',
  height: '100%',
  min_height: 420,
  resize: false,
  skin: 'oxide',
  content_css: 'default',
  promotion: false,
  branding: false,
  setup: (editor) => {
    editor.on('drop', (event) => {
      const expression = event.dataTransfer?.getData('text/plain') || ''
      if (!expression.startsWith('${')) return
      event.preventDefault()
      event.stopPropagation()
      editor.insertContent(createExpressionText(expression))
      editor.focus()
    })
  },
  menubar: 'edit view insert format table',
  plugins: [
    'advlist', 'autolink', 'charmap', 'code', 'fullscreen', 'lists',
    'nonbreaking', 'pagebreak', 'preview', 'searchreplace', 'table',
    'visualblocks', 'wordcount',
  ],
  toolbar: [
    'undo redo | blocks fontfamily fontsize',
    'bold italic underline strikethrough | forecolor backcolor',
    'alignleft aligncenter alignright alignjustify',
    'bullist numlist outdent indent | table pagebreak',
    'removeformat code fullscreen',
  ].join(' | '),
  font_family_formats: [
    '微软雅黑=Microsoft YaHei',
    '宋体=SimSun',
    '黑体=SimHei',
    'Arial=Arial',
    'Times New Roman=Times New Roman',
  ].join(';'),
  font_size_formats: '8pt 9pt 10pt 10.5pt 11pt 12pt 14pt 16pt 18pt 22pt 26pt 32pt',
  pagebreak_separator: '<div class="schemax-page-break mceNonEditable" data-schemax-block="page-break" contenteditable="false"></div>',
  pagebreak_split_block: true,
  noneditable_class: 'mceNonEditable',
  extended_valid_elements: [
    'span[class|style]',
    'div[class|style|data-schemax-block|data-directive|data-alias|data-source|contenteditable]',
  ].join(','),
  invalid_elements: 'img,video,audio,iframe,object,embed,svg,canvas,script',
  content_style: `
    body {
      margin: 0 auto;
      padding: 28px 38px 60px;
      max-width: 820px;
      min-height: 100%;
      color: #303133;
      background: #fff;
      font-family: "Microsoft YaHei", Arial, sans-serif;
      line-height: 1.6;
    }
    .schemax-page-break {
      height: 0;
      margin: 22px 0;
      border-top: 2px dashed #909399;
    }
  `,
}

const initializeEditor = async () => {
  await nextTick()
  if (props.readonly || !editorTargetRef.value || isUnmounted) return

  editorTargetRef.value.value = getSectionHtml(activeSection.value)
  const editors = await tinymce.init({
    ...editorOptions,
    target: editorTargetRef.value,
  })
  const editor = editors?.[0]
  if (!editor) {
    if (isUnmounted) return
    throw new Error('Word 编辑器初始化失败')
  }
  if (isUnmounted) {
    editor.remove()
    return
  }
  editorInstance.value = editor
}

const saveEditorSection = (section = activeSection.value) => {
  if (!editorInstance.value) return
  setSectionHtml(section, editorInstance.value.getContent({format: 'html'}))
}

const loadEditorSection = (section) => {
  if (!editorInstance.value) return
  editorInstance.value.resetContent(getSectionHtml(section))
  editorInstance.value.focus()
}

const insertExpression = (expression) => {
  if (!expression || props.readonly || !editorInstance.value) return false
  editorInstance.value.insertContent(createExpressionText(expression))
  editorInstance.value.focus()
  return true
}

const openPageSettings = () => {
  pageDraft.value = clone(content.value.page || createDefaultPage())
  pageDialogOpen.value = true
}

const pageSize = computed({
  get: () => pageDraft.value.size || 'A4',
  set: (value) => {
    const sizes = {
      A4: [210, 297],
      A3: [297, 420],
      LETTER: [215.9, 279.4],
    }
    const [width, height] = sizes[value] || sizes.A4
    pageDraft.value.size = value
    pageDraft.value.widthMm = width
    pageDraft.value.heightMm = height
  },
})

const applyPageSettings = () => {
  content.value.page = clone(pageDraft.value)
  pageDialogOpen.value = false
}

const getData = () => {
  saveEditorSection()
  return clone(content.value)
}
const insertText = (text) => insertExpression(text)

watch(activeSection, (section, previousSection) => {
  saveEditorSection(previousSection)
  loadEditorSection(section)
})

watch(() => props.documentData, (value) => {
  content.value = normalizeContent(value)
  loadEditorSection(activeSection.value)
}, {deep: true})

onMounted(() => {
  initializeEditor().catch((error) => {
    console.error('初始化 Word 编辑器失败：', error)
  })
})

onBeforeUnmount(() => {
  isUnmounted = true
  const editor = editorInstance.value
  editorInstance.value = null
  editor?.remove()
})

defineExpose({
  getData,
  insertText,
  insertExpression,
})
</script>

<style scoped lang="scss">
.tiny-word-document {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #eef1f5;
}

.word-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-bottom: 0;
  background: #fff;

  .section-select {
    width: 180px;
  }
}

.editor-shell {
  flex: 1;
  min-height: 0;

  .editor-target {
    width: 100%;
    min-height: 420px;
  }

  :deep(.tox-tinymce) {
    height: 100% !important;
    border-radius: 0;
  }
}

.word-page {
  width: min(794px, calc(100% - 32px));
  min-height: 1123px;
  margin: 16px auto;
  padding: 36px 56px;
  background: #fff;
  box-shadow: 0 2px 12px rgb(0 0 0 / 10%);
  color: #303133;
  font-family: "Microsoft YaHei", Arial, sans-serif;
  line-height: 1.6;
  box-sizing: border-box;
}

.word-header,
.word-footer {
  min-height: 36px;
  color: #606266;
  font-size: 12px;
}

.word-header {
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 20px;
}

.word-footer {
  border-top: 1px solid #ebeef5;
  margin-top: 24px;
}

.word-body,
.word-header,
.word-footer {

  :deep(.schemax-page-break) {
    margin: 22px 0;
    border-top: 2px dashed #909399;
  }
}

.is-readonly {
  overflow: auto;
}
.font-size-input {
  width: 110px;
  margin-left: 12px;
}

.unit-label {
  margin-left: 6px;
  color: #606266;
}

.margin-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 12px;

  :deep(.el-input-number) {
    width: 150px;
  }
}
</style>
