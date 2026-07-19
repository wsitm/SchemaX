<template>
  <div class="sheet-wrapper">
    <div class="tpl-toolbar">
      <el-select
        v-model="activeTpId"
        clearable
        filterable
        placeholder="默认模板"
        style="width: 280px"
        @change="handleTemplateChange"
      >
        <el-option
          v-for="item in templateList"
          :key="item.tpId"
          :label="item.tpName"
          :value="item.tpId"
        />
      </el-select>
      <el-tag v-if="currentTemplate && currentTemplate.isDef === 1" type="success">默认</el-tag>
      <el-tag v-if="currentTemplate && currentTemplate.tpType === 1" type="success">excel</el-tag>
      <el-tag v-if="currentTemplate && currentTemplate.tpType === 3" type="info">markdown</el-tag>
      <el-tag v-if="currentTemplate && currentTemplate.tpType === 2" type="warning">word</el-tag>
    </div>
    <div class="tpl-content">
      <univer-sheet
        v-if="!currentTemplate || currentTemplate.tpType === 1"
        :workbook-data="displayWorkbookData"
      />
      <tiny-mce-document v-else-if="currentTemplate.tpType === 2"
                         v-loading="wordLoading"
                         class="word-preview"
                         :document-data="renderedDocumentData"
                         :readonly="true"/>
      <div v-else-if="currentTemplate.tpType === 3" class="md-preview" v-html="markdownHtml"/>
      <el-empty v-else description="当前模板类型暂不支持预览"/>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import {marked} from "marked";
import UniverSheet from "../../components/UniverSheet/index.vue";
import {tableInfoToWorkbookData} from "@/views/rdbms/connect/data";
import TinyMceDocument from "../../components/TinyMceDocument/index.vue";
import {renderMarkdownByTemplate, renderWorkbookByTemplate, resolveDefaultTemplate} from "@/views/rdbms/connect/render";
import XEUtils from "xe-utils";
import {previewWordTemplate} from "@/api/rdbms/connect";
import {ElMessage} from "element-plus";

const props = defineProps({
  tableInfoList: {
    type: Array,
    default: () => []
  },
  templateList: {
    type: Array,
    default: () => []
  },
  connectId: {
    type: Number,
    default: null
  },
  snapshotId: {
    type: [Number, String],
    default: null
  }
})

// const loading = ref(false)
// const tableInfoList = ref([])
// const templateList = ref([])
const activeTpId = ref(null)
const renderedWorkbookData = ref(null)
const renderedMarkdown = ref("")

const renderedDocumentData = ref(null)
const wordLoading = ref(false)
let wordRequestSequence = 0

const workbookData = computed(() => {
  return tableInfoToWorkbookData(props.tableInfoList);
})

const currentTemplate = computed(() => {
  if (!activeTpId.value) return null;
  return props.templateList.find(item => item.tpId === activeTpId.value) || null;
})

const displayWorkbookData = computed(() => {
  if (!currentTemplate.value || currentTemplate.value.tpType !== 1) {
    return workbookData.value;
  }
  return renderedWorkbookData.value || workbookData.value;
})

const markdownHtml = computed(() => {
  return marked.parse(renderedMarkdown.value || "");
})

const renderTemplate = async () => {
  const sequence = ++wordRequestSequence;
  const template = currentTemplate.value;
  renderedWorkbookData.value = null;
  renderedMarkdown.value = "";
  renderedDocumentData.value = null;
  wordLoading.value = false;

  if (!template) return;

  if (template.tpType === 1) {
    renderedWorkbookData.value = renderWorkbookByTemplate(template.tpContent, props.tableInfoList);
    return;
  }
  if (template.tpType === 3) {
    renderedMarkdown.value = renderMarkdownByTemplate(template.tpContent, props.tableInfoList);
    return;
  }
  if (template.tpType !== 2 || !props.connectId) return;

  wordLoading.value = true;
  try {
    const res = await previewWordTemplate(props.connectId, template.tpId, props.snapshotId);
    if (sequence === wordRequestSequence) {
      renderedDocumentData.value = res.data || null;
    }
  } catch (e) {
    if (sequence === wordRequestSequence) {
      console.error('渲染 Word 模板预览失败:', e);
      ElMessage.error('Word 模板预览失败：' + (e?.message || '未知错误'));
    }
  } finally {
    if (sequence === wordRequestSequence) {
      wordLoading.value = false;
    }
  }
}

const handleTemplateChange = XEUtils.debounce(() => {
  renderTemplate();
}, 200)

// const getTableInfoFunc = (connectId) => {
//   loading.value = true;
//   Promise.all([
//     getTableInfo(connectId),
//     listConnectTemplate(connectId)
//   ]).then(([tableRes, templateRes]) => {
//     tableInfoList.value = tableRes.data || [];
//     templateList.value = templateRes.data || [];
//     const def = resolveDefaultTemplate(templateList.value);
//     activeTpId.value = def?.tpId || null;
//     renderTemplate();
//   }).finally(() => {
//     loading.value = false;
//   });
// }

watch(() => props.tableInfoList, () => {
  handleTemplateChange();
}, {deep: true})

watch(() => props.templateList, (list) => {
  const selectedExists = list.some(item => item.tpId === activeTpId.value);
  if (!selectedExists) {
    const def = resolveDefaultTemplate(list);
    activeTpId.value = def?.tpId || null;
  }
  handleTemplateChange();
}, {deep: true})

watch(() => props.snapshotId, () => {
  handleTemplateChange();
})

watch(() => props.connectId, () => {
  handleTemplateChange();
})

onMounted(() => {
  const def = resolveDefaultTemplate(props.templateList);
  activeTpId.value = def?.tpId || null;
  handleTemplateChange();
});

</script>

<style scoped lang="scss">

.sheet-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tpl-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 8px 6px;
}

.tpl-content {
  flex: 1;
  min-height: 0;
}

.word-preview {
  width: 100%;
  height: 100%;
}

.md-preview {
  height: 100%;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    border-spacing: 0;
    margin: 8px 0;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #dcdfe6;
    padding: 6px 10px;
    vertical-align: top;
    line-height: 1.5;
  }

  :deep(thead th) {
    background: #f5f7fa;
    font-weight: 600;
  }
}
</style>
