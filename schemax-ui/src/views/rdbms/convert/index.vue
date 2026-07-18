<template>
  <div class="app-container">
    <el-row :gutter="10">
      <splitpanes class="default-theme">
        <pane size="50">
          <el-col class="panel-column">
            <el-form ref="queryForm" :inline="true" label-width="auto" class="toolbar-form">
              <el-form-item label="类型" prop="inputType">
                <el-select v-model="inputType"
                           size="small"
                           @change="onLeftTypeChange"
                           placeholder="请选择类型"
                           style="width: 100px;">
                  <el-option
                    v-for="item in ENUM.convertType"
                    :key="item.type"
                    :label="item.name"
                    :value="item.type">
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item v-if="inputType === 1" label="源方言" prop="sourceDatabase">
                <el-select v-model="sourceDatabase"
                           filterable
                           size="small"
                           @change="onDialectChange"
                           placeholder="请选择源方言"
                           style="width: 150px;">
                  <el-option
                    v-for="item in dialects"
                    :key="'source-' + item.database"
                    :label="item.database"
                    :value="item.database">
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item v-if="inputType === 1">
                <el-button type="primary"
                           size="small"
                           :icon="Search"
                           :loading="prechecking"
                           @click="runPrecheck">预检
                </el-button>
              </el-form-item>
              <el-form-item v-if="inputType === 2" class="fr mr10">
                <el-button type="primary" :icon="DArrowRight"
                           @click="excelDataToDDL">生成
                </el-button>
              </el-form-item>
              <el-form-item v-if="inputType === 2" class="fr mr10">
                <el-upload
                  :action="uploadURL"
                  :multiple="false"
                  :on-success="uploadSuccess"
                  :show-file-list="false"
                  class="upload-demo">
                  <el-button type="primary" :icon="Upload">导入</el-button>
                </el-upload>
              </el-form-item>
            </el-form>
            <div class="panel-body">
              <codemirror
                v-if="inputType === 1"
                ref="codeMirrorLeft"
                v-model="contentLeft"
                :tab-size="2"
                :extensions="extensions"
                class="code-mirror"
                @ready="handleLeftEditorReady"
              />
              <univer-sheet
                v-if="inputType === 2"
                :key="`sheet-left-${inputType}`"
                ref="sheetLeft"
                class="univer-sheet"
                :workbook-data="workbookDataLeft"/>
            </div>
          </el-col>
        </pane>
        <pane size="50">
          <el-col class="panel-column right-panel">
            <el-form ref="queryForm" :inline="true" class="toolbar-form right-toolbar">
              <el-form-item label="类型" prop="outputType" class="ml5">
                <el-select v-model="outputType"
                           size="small"
                           @change="onRightTypeChange"
                           placeholder="请选择类型"
                           style="width: 100px;">
                  <el-option
                    v-for="item in ENUM.convertType"
                    :key="item.type"
                    :label="item.name"
                    :value="item.type">
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item v-if="outputType === 1"
                            label="目标方言" prop="outputDatabase">
                <el-select v-model="outputDatabase"
                           filterable
                           size="small"
                           @change="onDialectChange"
                           placeholder="请选择目标方言"
                           style="width: 150px;">
                  <el-option
                    v-for="item in dialects"
                    :key="item.database"
                    :label="item.database"
                    :value="item.database">
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="视图" class="result-view-form-item">
                <el-select v-model="activeResultTab"
                           size="small"
                           aria-label="结果视图"
                           @change="onResultViewChange"
                           style="width: 130px;">
                  <el-option label="转换结果" value="result"/>
                  <el-option label="预检诊断" value="diagnosis"/>
                  <el-option label="按表统计" value="statistics"/>
                </el-select>
              </el-form-item>
            </el-form>

            <div class="result-content">
              <div v-if="activeResultTab === 'result'" class="result-pane">
                  <codemirror
                    v-if="outputType === 1"
                    ref="codeMirrorRight"
                    v-model="contentRight"
                    :tab-size="2"
                    :extensions="extensions"
                    class="code-mirror"
                    @ready="handleRightEditorReady"
                  />
                  <univer-sheet
                    v-if="outputType === 2"
                    :key="`sheet-right-${outputType}-${tableInfoListRight.length}`"
                    ref="sheetRight"
                    class="univer-sheet"
                    :worksheet-data="workbookDataRight"/>
              </div>

              <div v-else-if="activeResultTab === 'diagnosis'" v-loading="prechecking" class="diagnosis-pane">
                  <el-alert v-if="precheckStale"
                            title="DDL或数据库方言已变化，请重新预检"
                            type="warning"
                            :closable="false"
                            show-icon/>
                  <template v-if="precheckResult">
                    <div class="diagnosis-summary">
                      <div class="summary-item">
                        <span class="summary-label">转换成功率</span>
                        <span class="summary-value">{{ precheckResult.successRate }}%</span>
                      </div>
                      <div class="summary-item">
                        <span class="summary-label">语句</span>
                        <span class="summary-value">{{ precheckResult.successCount }}/{{ precheckResult.statementCount }}</span>
                      </div>
                      <div class="summary-item error-item">
                        <span class="summary-label">错误</span>
                        <span class="summary-value">{{ precheckResult.errorCount }}</span>
                      </div>
                      <div class="summary-item warning-item">
                        <span class="summary-label">警告</span>
                        <span class="summary-value">{{ precheckResult.warningCount }}</span>
                      </div>
                    </div>
                    <div class="diagnosis-toolbar">
                      <el-radio-group v-model="issueFilter" size="small">
                        <el-radio-button value="ALL">全部</el-radio-button>
                        <el-radio-button value="ERROR">错误</el-radio-button>
                        <el-radio-button value="WARNING">警告</el-radio-button>
                        <el-radio-button value="INFO">提示</el-radio-button>
                      </el-radio-group>
                    </div>
                    <el-table :data="filteredIssues"
                              height="100%"
                              class="issue-table"
                              @row-dblclick="locateIssue">
                      <el-table-column label="级别" width="78" align="center">
                        <template #default="scope">
                          <el-tag :type="issueTagType(scope.row.level)" size="small">
                            {{ issueLevelText(scope.row.level) }}
                          </el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column label="类别" width="96">
                        <template #default="scope">
                          {{ issueCategoryText(scope.row.category) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="对象" min-width="120" show-overflow-tooltip>
                        <template #default="scope">
                          {{ issueObjectText(scope.row) }}
                        </template>
                      </el-table-column>
                      <el-table-column label="位置" width="96" align="center">
                        <template #default="scope">
                          <el-button link type="primary" :icon="Location" @click="locateIssue(scope.row)">
                            第{{ scope.row.startLine }}行
                          </el-button>
                        </template>
                      </el-table-column>
                      <el-table-column label="问题与建议" min-width="330">
                        <template #default="scope">
                          <div class="issue-message">{{ scope.row.message }}</div>
                          <div class="issue-reason">{{ scope.row.reason }}</div>
                          <div v-if="scope.row.suggestion" class="issue-suggestion">
                            建议：{{ scope.row.suggestion }}
                          </div>
                        </template>
                      </el-table-column>
                    </el-table>
                  </template>
                  <el-empty v-else description="点击“预检”检查当前DDL"/>
              </div>

              <div v-else-if="activeResultTab === 'statistics'" class="statistics-pane">
                  <el-table v-if="precheckResult"
                            :data="precheckResult.tableStatistics"
                            height="100%">
                    <el-table-column label="表名" prop="tableName" min-width="160" show-overflow-tooltip/>
                    <el-table-column label="状态" width="90" align="center">
                      <template #default="scope">
                        <el-tag :type="tableStatusType(scope.row.status)" size="small">
                          {{ scope.row.status }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="语句" prop="statementCount" width="70" align="center"/>
                    <el-table-column label="错误" prop="errorCount" width="70" align="center"/>
                    <el-table-column label="警告" prop="warningCount" width="70" align="center"/>
                    <el-table-column label="转换成功率" min-width="180">
                      <template #default="scope">
                        <el-progress :percentage="scope.row.successRate"
                                     :status="tableProgressStatus(scope.row)"/>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-else description="暂无预检统计"/>
              </div>
            </div>

            <span v-if="converting && activeResultTab === 'result'" class="converting">
              <i class="el-icon-loading"></i>
              转换中...
            </span>
          </el-col>
        </pane>
      </splitpanes>
    </el-row>
  </div>
</template>

<script setup name="Convert">
import "splitpanes/dist/splitpanes.css";
import {Pane, Splitpanes} from "splitpanes";

import XEUtils from "xe-utils";
import {getDialects} from "@/api/rdbms/connect";
import {convertDDL, precheckDDL} from "@/api/rdbms/convert";
import {tableInfoToWorkbookData, workbookDataToTableInfo} from "@/views/rdbms/connect/data";
import UniverSheet from "@/views/rdbms/components/UniverSheet/index.vue";
import sqlFormatter from '@sqltools/formatter';

import {Codemirror} from 'vue-codemirror';
import {StandardSQL} from '@codemirror/lang-sql'
import {monokai} from '@uiw/codemirror-theme-monokai';

import {DEMO_SQL} from "./data";
import {computed, getCurrentInstance, nextTick, onActivated, onMounted, ref, watch} from 'vue'
import {DArrowRight, Location, Search, Upload} from '@element-plus/icons-vue'
import {DEFAULT_WORKBOOK_DATA} from '@/views/rdbms/components/UniverSheet/sheet-data'

const {proxy} = getCurrentInstance()

const extensions = [StandardSQL, monokai]

const ENUM = {
  convertType: [{
    type: 1, name: "DDL"
  }, {
    type: 2, name: "Excel"
  }]
}

const uploadURL = import.meta.env.VITE_APP_BASE_API + "/rdbms/convert/upload"

const codeMirrorLeft = ref(null)
const codeMirrorRight = ref(null)
const codeMirrorLeftView = ref(null)
const codeMirrorRightView = ref(null)
const sheetLeft = ref(null)
const sheetRight = ref(null)

const dialects = ref([])
const inputType = ref(1)
const outputType = ref(1)
const sourceDatabase = ref(null)
const outputDatabase = ref(null)
const contentLeft = ref(DEMO_SQL)
const contentRight = ref("")
const workbookDataLeft = ref({...DEFAULT_WORKBOOK_DATA})
const tableInfoListLeft = ref([])
const tableInfoListRight = ref([])
const converting = ref(false)
const prechecking = ref(false)
const precheckResult = ref(null)
const precheckStale = ref(false)
const activeResultTab = ref('result')
const issueFilter = ref('ALL')
let convertSequence = 0

const workbookDataRight = computed(() => {
  return tableInfoToWorkbookData(tableInfoListRight.value);
})

const precheckIssues = computed(() => precheckResult.value?.issues || [])

const filteredIssues = computed(() => {
  if (issueFilter.value === 'ALL') {
    return precheckIssues.value
  }
  return precheckIssues.value.filter(item => item.level === issueFilter.value)
})

watch(contentLeft, () => {
  markPrecheckStale()
  convertDDLFunc()
})

const getDialectsFunc = () => {
  getDialects().then(res => {
    dialects.value = res.data || [];
    if (dialects.value.length > 0) {
      sourceDatabase.value = sourceDatabase.value || dialects.value[0].database;
      outputDatabase.value = outputDatabase.value || dialects.value[0].database;
      convertDDLFunc();
    }
  });
}

const onLeftTypeChange = () => {
  if (inputType.value === 2 && outputType.value === 2) {
    outputType.value = 1;
  }
  if (inputType.value !== 1) {
    precheckResult.value = null
    precheckStale.value = false
    activeResultTab.value = 'result'
  }
  convertDDLFunc()
}

const onDialectChange = () => {
  markPrecheckStale()
  convertDDLFunc()
}

const markPrecheckStale = () => {
  if (precheckResult.value) {
    precheckStale.value = true
  }
}

const convertDDLFunc = XEUtils.debounce(function () {
  if (!contentLeft.value && (!tableInfoListLeft.value || tableInfoListLeft.value.length === 0)) {
    converting.value = false
    return;
  }
  const params = {
    inputType: inputType.value,
    outputType: outputType.value,
    sourceDatabase: sourceDatabase.value,
    outputDatabase: outputDatabase.value
  };
  if (inputType.value === 1) {
    if (!contentLeft.value) {
      return;
    }
    params.inputDDL = contentLeft.value;
  }
  if (inputType.value === 2) {
    if (!tableInfoListLeft.value || tableInfoListLeft.value.length === 0) {
      excelDataToTableInfo();
    }
    if (!tableInfoListLeft.value || tableInfoListLeft.value.length === 0) {
      return;
    }
    params.tableVOList = tableInfoListLeft.value;
  }

  const sequence = ++convertSequence
  converting.value = true;
  convertDDL(params).then(res => {
    if (sequence !== convertSequence || !res.data) {
      return
    }
    if (outputType.value === 1) {
      contentRight.value = Object.keys(res.data).map(tableName => {
        const list = res.data[tableName];
        return list.map(ddl => {
          try {
            return sqlFormatter.format(ddl) + ";";
          } catch (e) {
            console.error('格式化DDL失败:', e);
          }
          return ddl + ";";
        }).join("\n");
      }).join("\n\n");
    } else {
      tableInfoListRight.value = res.data;
    }
  }).finally(() => {
    if (sequence === convertSequence) {
      converting.value = false;
    }
  });
}, 200)

const runPrecheck = () => {
  if (inputType.value !== 1 || !contentLeft.value?.trim()) {
    proxy.$modal.notifyWarning('请输入需要预检的DDL')
    return
  }
  if (!sourceDatabase.value || !outputDatabase.value) {
    proxy.$modal.notifyWarning('请选择源数据库和目标数据库方言')
    return
  }
  prechecking.value = true
  precheckDDL({
    inputDDL: contentLeft.value,
    sourceDatabase: sourceDatabase.value,
    outputDatabase: outputDatabase.value
  }).then(res => {
    precheckResult.value = res.data
    precheckStale.value = false
    issueFilter.value = 'ALL'
    activeResultTab.value = 'diagnosis'
    if (res.data?.errorCount === 0 && res.data?.warningCount === 0) {
      proxy.$modal.notifySuccess('预检通过，未发现转换风险')
    }
  }).finally(() => {
    prechecking.value = false
  })
}

const handleLeftEditorReady = payload => {
  codeMirrorLeftView.value = payload.view
}

const handleRightEditorReady = payload => {
  codeMirrorRightView.value = payload.view
  nextTick(() => payload.view.requestMeasure())
}

const locateIssue = issue => {
  const view = codeMirrorLeftView.value
  if (!view) {
    return
  }
  const documentLength = view.state.doc.length
  const anchor = Math.min(Math.max(0, issue.startOffset || 0), documentLength)
  const head = Math.min(Math.max(anchor, issue.endOffset || anchor + 1), documentLength)
  view.dispatch({
    selection: {anchor, head},
    scrollIntoView: true
  })
  view.focus()
}

const onResultViewChange = name => {
  if (name === 'result') {
    codeMirrorRightView.value = null
  }
}

const issueLevelText = level => ({
  ERROR: '错误',
  WARNING: '警告',
  INFO: '提示'
}[level] || level)

const issueTagType = level => ({
  ERROR: 'danger',
  WARNING: 'warning',
  INFO: 'info'
}[level] || 'info')

const issueCategoryText = category => ({
  SYNTAX: '语法',
  UNSUPPORTED_SYNTAX: '不支持语法',
  TYPE: '字段类型',
  RESERVED_WORD: '保留字',
  DEFAULT_VALUE: '默认值',
  PARTITION: '分区表',
  DIALECT: '方言差异'
}[category] || category)

const issueObjectText = issue => {
  if (issue.tableName && issue.columnName) {
    return `${issue.tableName}.${issue.columnName}`
  }
  return issue.tableName || `第${issue.statementIndex}条语句`
}

const tableStatusType = status => ({
  '通过': 'success',
  '有风险': 'warning',
  '失败': 'danger'
}[status] || 'info')

const tableProgressStatus = row => {
  if (row.errorCount > 0) {
    return 'exception'
  }
  if (row.warningCount > 0) {
    return 'warning'
  }
  return 'success'
}

const excelDataToTableInfo = () => {
  try {
    const workbookData = sheetLeft.value?.getData()
    if (!workbookData) {
      proxy.$modal.notifyWarning('请先编辑Excel数据')
      return
    }
    tableInfoListLeft.value = workbookDataToTableInfo(workbookData)
  } catch (e) {
    console.error('Excel数据转换失败:', e)
    proxy.$modal.notifyError('Excel数据转换失败：' + (e.message || '未知错误'))
  }
}

const excelDataToDDL = () => {
  excelDataToTableInfo();
  convertDDLFunc();
}

const onRightTypeChange = () => {
  if (inputType.value === 2 && outputType.value === 2) {
    inputType.value = 1;
  }
  convertDDLFunc();
}

const uploadSuccess = res => {
  if (res.data) {
    workbookDataLeft.value = res.data;
  }
}

onMounted(() => {
  getDialectsFunc();
})

onActivated(() => {
  workbookDataLeft.value = DEFAULT_WORKBOOK_DATA
})
</script>

<style scoped lang="scss">
.app-container {
  height: calc(100vh - 40px);
  box-sizing: border-box;

  :deep(.splitpanes__splitter) {
    background-color: #d9e9fa;
  }

  :deep(.el-row),
  :deep(.el-col) {
    height: 100%;
  }

  :deep(.el-form-item) {
    margin-bottom: 10px;
  }
}

.panel-column {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.toolbar-form {
  flex: 0 0 auto;
}

.right-toolbar {
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.result-view-form-item {
  margin-left: auto;
}

.panel-body,
.result-pane,
.statistics-pane {
  flex: 1;
  min-height: 0;
  height: 100%;
}

.upload-demo {
  display: inline-block;
  margin-left: 10px;
}

.univer-sheet {
  height: 100%;
  border: 1px solid #dfe4ed;
}

.result-content {
  flex: 1;
  min-height: 0;
  height: 100%;
}

.diagnosis-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.diagnosis-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  padding: 9px 12px;
  border-right: 1px solid #ebeef5;
  color: #303133;

  &:last-child {
    border-right: 0;
  }
}

.summary-label {
  color: #606266;
  white-space: nowrap;
}

.summary-value {
  margin-left: 8px;
  font-size: 16px;
  font-weight: 600;
}

.error-item .summary-value {
  color: #d64545;
}

.warning-item .summary-value {
  color: #b7791f;
}

.diagnosis-toolbar {
  padding: 8px 0;
}

.issue-table {
  flex: 1;
  min-height: 0;
}

.issue-message {
  color: #303133;
  font-weight: 600;
}

.issue-reason,
.issue-suggestion {
  margin-top: 3px;
  color: #606266;
  line-height: 1.45;
  white-space: normal;
}

.issue-suggestion {
  color: #2f6f4e;
}

.converting {
  position: absolute;
  right: 14px;
  top: 46px;
  z-index: 3;
  color: #d64545;
  font-size: 12px;
}

.code-mirror {
  height: 100%;

  :deep(.cm-editor) {
    height: 100%;
    font-size: 14px;
    line-height: 150%;
    font-family: sans-serif, monospace;
  }
}
</style>
