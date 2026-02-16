<template>
  <div class="app-container">
    <el-tabs tab-position="left">
      <el-tab-pane label="表信息" :lazy="true">
        <base-info v-loading="loading" :table-info-list="tableInfoList"/>
      </el-tab-pane>
      <el-tab-pane label="表结构" :lazy="true">
        <div class="sheet-wrapper" v-loading="loading">
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
          </div>
          <div class="tpl-content">
            <univer-sheet
              v-if="!currentTemplate || currentTemplate.tpType === 1"
              :workbook-data="displayWorkbookData"
            />
            <div v-else-if="currentTemplate.tpType === 3" class="md-preview" v-html="markdownHtml"/>
            <el-empty v-else description="当前模板类型暂不支持预览"/>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="DDL语句" :lazy="true">
        <DDL ref="ddlRef" :connect-id="connectId" :driverClass="driverClass"/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import {computed, ref} from 'vue'
import {marked} from "marked";
import {getTableInfo, listConnectTemplate} from "@/api/rdbms/connect";
import BaseInfo from "./BaseInfo/index.vue";
import UniverSheet from "../components/UniverSheet/index.vue";
import DDL from "./DDL/index.vue";
import {tableInfoToWorkbookData} from "@/views/rdbms/connect/data";
import {renderMarkdownByTemplate, renderWorkbookByTemplate, resolveDefaultTemplate} from "@/views/rdbms/connect/render";

const props = defineProps({
  connectId: Number,
  driverClass: String
})

const loading = ref(false)
const tableInfoList = ref([])
const ddlRef = ref()
const templateList = ref([])
const activeTpId = ref(null)
const renderedWorkbookData = ref(null)
const renderedMarkdown = ref("")

const workbookData = computed(() => {
  return tableInfoToWorkbookData(tableInfoList.value);
})

const currentTemplate = computed(() => {
  if (!activeTpId.value) return null;
  return templateList.value.find(item => item.tpId === activeTpId.value) || null;
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

const renderTemplate = () => {
  if (!currentTemplate.value) {
    renderedWorkbookData.value = null;
    renderedMarkdown.value = "";
    return;
  }
  if (currentTemplate.value.tpType === 1) {
    renderedWorkbookData.value = renderWorkbookByTemplate(currentTemplate.value.tpContent, tableInfoList.value);
    renderedMarkdown.value = "";
    return;
  }
  if (currentTemplate.value.tpType === 3) {
    renderedMarkdown.value = renderMarkdownByTemplate(currentTemplate.value.tpContent, tableInfoList.value);
    renderedWorkbookData.value = null;
    return;
  }
  renderedWorkbookData.value = null;
  renderedMarkdown.value = "";
}

const handleTemplateChange = () => {
  renderTemplate();
}

const getTableInfoFunc = (connectId) => {
  loading.value = true;
  Promise.all([
    getTableInfo(connectId),
    listConnectTemplate(connectId)
  ]).then(([tableRes, templateRes]) => {
    tableInfoList.value = tableRes.data || [];
    templateList.value = templateRes.data || [];
    const def = resolveDefaultTemplate(templateList.value);
    activeTpId.value = def?.tpId || null;
    renderTemplate();
  }).finally(() => {
    loading.value = false;
  });
}

defineExpose({
  getTableInfo: getTableInfoFunc
})
</script>

<style scoped lang="scss">
.app-container {
  padding: 0;
  height: calc(100vh - 75px);

  .el-tabs {
    height: 100%;

    :deep(.el-tabs__content) {
      height: 100%;
    }

    :deep(.el-tab-pane) {
      height: 100%;
    }
  }
}

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
