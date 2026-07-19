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
        <Editor
          v-model="activeHtml"
          license-key="gpl"
          :init="editorOptions"
          :model-events="'change input undo redo'"
          @init="handleEditorInit"
        />
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
import {computed, ref, watch} from 'vue'
import {Setting} from '@element-plus/icons-vue'
import Editor from '@tinymce/tinymce-vue'
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
import 'tinymce/skins/ui/oxide/skin.min.css'
import 'tinymce/skins/content/default/content.min.css'
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
const normalizeContent = (value) => {
  const normalized = value?.editor === 'tinymce' ? clone(value) : createDefaultContent()
  normalized.bodyHtml ||= '<p></p>'
  normalized.page = {...createDefaultPage(), ...(normalized.page || {})}
  normalized.page.margins = {
    ...createDefaultPage().margins,
    ...(normalized.page.margins || {}),
  }
  normalized.headers ||= {}
  normalized.footers ||= {}
  return normalized
}

const content = ref(normalizeContent(props.documentData))
const activeSection = ref('body')
const editorInstance = ref(null)
const pageDialogOpen = ref(false)
const pageDraft = ref(createDefaultPage())

const sectionOptions = computed(() => [
  {label: '正文', value: 'body'},
  {label: '默认页眉', value: 'headers.default'},
  {label: '首页页眉', value: 'headers.first'},
  {label: '偶数页页眉', value: 'headers.even'},
  {label: '默认页脚', value: 'footers.default'},
  {label: '首页页脚', value: 'footers.first'},
  {label: '偶数页页脚', value: 'footers.even'},
])

const activeHtml = computed({
  get() {
    if (activeSection.value === 'body') return content.value.bodyHtml || '<p></p>'
    const [group, type] = activeSection.value.split('.')
    return content.value[group]?.[type] || '<p></p>'
  },
  set(value) {
    if (activeSection.value === 'body') {
      content.value.bodyHtml = value
      return
    }
    const [group, type] = activeSection.value.split('.')
    content.value[group] ||= {}
    content.value[group][type] = value
  },
})

const defaultHeaderHtml = computed(() => content.value.headers?.default || '')
const defaultFooterHtml = computed(() => content.value.footers?.default || '')

const createExpressionHtml = (expression) => {
  const encoded = tinymce.DOM.encode(expression)
  return `<span class="schemax-variable mceNonEditable" data-expression="${encoded}" contenteditable="false">${encoded}</span>&nbsp;`
}

const editorOptions = {
  license_key: 'gpl',
  language: 'zh-CN',
  height: '100%',
  min_height: 420,
  resize: false,
  skin: false,
  content_css: false,
  promotion: false,
  branding: false,
  setup: (editor) => {
    editor.on('drop', (event) => {
      const expression = event.dataTransfer?.getData('text/plain') || ''
      if (!expression.startsWith('${')) return
      event.preventDefault()
      event.stopPropagation()
      editor.insertContent(createExpressionHtml(expression))
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
    'span[class|style|data-expression|contenteditable]',
    'div[class|style|data-schemax-block|data-directive|data-alias|data-source|contenteditable]',
  ].join(','),
  invalid_elements: 'img,video,audio,iframe,object,embed,svg,canvas,script',
  table_default_attributes: {border: '1'},
  table_default_styles: {
    width: '100%',
    borderCollapse: 'collapse',
  },
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
    table {
      width: 100%;
      max-width: 100%;
      table-layout: fixed;
      border-collapse: collapse;
      margin: 10px 0;
      box-sizing: border-box;
    }
    th, td {
      min-width: 0;
      border: 1px solid #b7bdc7;
      padding: 6px 8px;
      vertical-align: top;
      overflow-wrap: anywhere;
      word-break: break-word;
      box-sizing: border-box;
    }
    th p, td p { margin: 0; }
    th { background: #f2f5f8; font-weight: 600; }
    .schemax-variable {
      display: inline;
      max-width: 100%;
      padding: 0 4px;
      white-space: normal;
      overflow-wrap: anywhere;
      word-break: break-all;
      color: #125cad;
      background: #eaf3ff;
      border: 1px solid #a8c9ef;
      border-radius: 3px;
      line-height: 1.5;
    }
    .schemax-directive {
      padding: 3px 7px;
      color: #8a4b08;
      background: #fff4df;
      border-left: 3px solid #d99a35;
    }
    .schemax-page-break {
      height: 0;
      margin: 22px 0;
      border-top: 2px dashed #909399;
    }
  `,
}

const handleEditorInit = (_event, editor) => {
  editorInstance.value = editor
}

const insertExpression = (expression) => {
  if (!expression || props.readonly || !editorInstance.value) return false
  editorInstance.value.insertContent(createExpressionHtml(expression))
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

const getData = () => clone(content.value)
const insertText = (text) => insertExpression(text)

watch(() => props.documentData, (value) => {
  content.value = normalizeContent(value)
}, {deep: true})

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
  :deep(table) {
    width: 100%;
    max-width: 100%;
    table-layout: fixed;
    border-collapse: collapse;
    margin: 10px 0;
    box-sizing: border-box;
  }

  :deep(th),
  :deep(td) {
    min-width: 0;
    border: 1px solid #b7bdc7;
    padding: 6px 8px;
    vertical-align: top;
    overflow-wrap: anywhere;
    word-break: break-word;
    box-sizing: border-box;
  }

  :deep(th > p),
  :deep(td > p) {
    margin: 0;
  }

  :deep(th) {
    background: #f2f5f8;
  }

  :deep(.schemax-variable) {
    display: inline;
    max-width: 100%;
    color: #125cad;
    background: #eaf3ff;
    white-space: normal;
    overflow-wrap: anywhere;
    word-break: break-all;
  }

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