<template>
  <div class="app-container">
    <el-row :gutter="10">
      <splitpanes class="default-theme">
        <pane size="50">
          <el-col>
            <el-form ref="queryForm" :inline="true" label-width="auto">
              <el-form-item label="输入类型" prop="inputType">
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
              <el-form-item v-if="inputType===2" class="fr mr10">
                <el-button type="primary" :icon="DArrowRight"
                           @click="excelDataToDDL">生成
                </el-button>
              </el-form-item>
              <el-form-item v-if="inputType===2" class="fr mr10">
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
            <div style="height: calc(100% - 35px);">
              <codemirror
                v-if="inputType===1"
                ref="codeMirrorLeft"
                v-model="contentLeft"
                :tab-size="2"
                :extensions="extensions"
                class="code-mirror"
              />
              <univer-sheet
                v-if="inputType===2"
                :key="`sheet-left-${inputType}`"
                ref="sheetLeft"
                class="univer-sheet"
                :workbook-data="workbookDataLeft"/>
            </div>
          </el-col>
        </pane>
        <pane size="50">
          <el-col>
            <el-form ref="queryForm" :inline="true">
              <el-form-item label="输出类型" prop="outputType" class="ml5">
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
              <el-form-item v-if="outputType===1"
                            label="数据库方言" prop="outputDatabase">
                <el-select v-model="outputDatabase"
                           filterable
                           size="small"
                           @change="convertDDLFunc"
                           placeholder="请选择数据库方言"
                           style="width: 150px;">
                  <el-option
                    v-for="item in dialects"
                    :key="item.database"
                    :label="item.database"
                    :value="item.database">
                  </el-option>
                </el-select>
              </el-form-item>
            </el-form>
            <div id="right_cont" style="height: calc(100% - 35px);">
              <codemirror
                v-if="outputType===1"
                ref="codeMirrorRight"
                v-model="contentRight"
                :tab-size="2"
                :extensions="extensions"
                class="code-mirror"
              />
              <univer-sheet
                v-if="outputType===2"
                :key="`sheet-right-${outputType}-${tableInfoListRight.length}`"
                ref="sheetRight"
                class="univer-sheet"
                :worksheet-data="workbookDataRight"/>
            </div>
            <span v-if="converting" class="converting">
              <i class="el-icon-loading" style="font-weight: bold;"></i>
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
import {convertDDL} from "@/api/rdbms/convert";
import {tableInfoToWorkbookData, workbookDataToTableInfo} from "@/views/rdbms/connect/data";
import UniverSheet from "@/views/rdbms/components/UniverSheet/index.vue";
import sqlFormatter from '@sqltools/formatter';

import {Codemirror} from 'vue-codemirror';
import {StandardSQL} from '@codemirror/lang-sql'
import {monokai} from '@uiw/codemirror-theme-monokai';

import {DEMO_SQL} from "./data";
import {computed, onActivated, onMounted, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {DArrowRight, Upload} from '@element-plus/icons-vue'

const extensions = [StandardSQL, monokai]

import {DEFAULT_WORKBOOK_DATA} from '@/views/rdbms/components/UniverSheet/sheet-data'

const ENUM = {
  convertType: [{
    type: 1, name: "DDL"
  }, {
    type: 2, name: "Excel"
  }]
}

const uploadURL = import.meta.env.VITE_APP_BASE_API + "/rdbms/convert/upload"

// refs
const codeMirrorLeft = ref(null)
const codeMirrorRight = ref(null)
const sheetLeft = ref(null)
const sheetRight = ref(null)

// reactive data
const dialects = ref([])
const inputType = ref(1)
const outputType = ref(1)
const outputDatabase = ref(null)
const contentLeft = ref(DEMO_SQL)
const contentRight = ref("")
const workbookDataLeft = ref({})
const tableInfoListLeft = ref([])
const tableInfoListRight = ref([])
const converting = ref(false)

// computed
const workbookDataRight = computed(() => {
  return tableInfoToWorkbookData(tableInfoListRight.value);
})

// watch
watch(contentLeft, (value) => {
  convertDDLFunc();
})

// methods
const getDialectsFunc = () => {
  getDialects().then(res => {
    dialects.value = res.data;
    outputDatabase.value = res.data[0].database;
  });
}

// 左侧类型切换
const onLeftTypeChange = () => {
  if (inputType.value === 2 && outputType.value === 2) {
    outputType.value = 1;
  }
}

// 转换DDL
const convertDDLFunc = XEUtils.debounce(function () {
  if (!contentLeft.value && (!tableInfoListLeft.value || tableInfoListLeft.value.length === 0)) {
    return;
  }
  converting.value = true;
  const params = {
    inputType: inputType.value,
    outputType: outputType.value,
    outputDatabase: outputDatabase.value
  };
  if (inputType.value === 1) {
    params.inputDDL = contentLeft.value;
  }
  if (inputType.value === 2) {
    params.tableVOList = tableInfoListLeft.value;
  }
  convertDDL(params).then(res => {
    if (res.data) {
      if (outputType.value === 1) {
        contentRight.value = Object.keys(res.data).map(tableName => {
          const list = res.data[tableName];
          return list.map(ddl => {
            try {
              return sqlFormatter.format(ddl) + ";";
            } catch (e) {
              console.error(e);
            }
            return ddl + ";";
          }).join("\n");
        }).join("\n\n");
      } else {
        tableInfoListRight.value = res.data;
      }
    }
  }).finally(() => {
    converting.value = false;
  });
}, 200)

// excel数据转换为DDL
const excelDataToDDL = () => {
  try {
    // getData() 返回完整的 workbook 数据（包含 sheets 属性）
    const workbookData = sheetLeft.value?.getData()
    if (!workbookData) {
      ElMessage.warning('请先编辑 Excel 数据')
      return
    }
    // workbookDataToTableInfo 期望接收完整的 workbook 数据格式
    tableInfoListLeft.value = workbookDataToTableInfo(workbookData)
    convertDDLFunc()
  } catch (e) {
    console.error('Excel 数据转换失败:', e)
    ElMessage.error('Excel 数据转换失败: ' + (e.message || '未知错误'))
  }
}

// 右侧类型切换
const onRightTypeChange = () => {
  if (inputType.value === 2 && outputType.value === 2) {
    inputType.value = 1;
  }
  convertDDLFunc();
}

// 上传成功
const uploadSuccess = (res, file) => {
  if (res.data) {
    workbookDataLeft.value = {
      ...DEFAULT_WORKBOOK_DATA,
      // id: new Date().getTime().toString(),
      sheets: res.data
    };
    console.log("workbookDataLeft", workbookDataLeft.value)
  }
}

// lifecycle
onMounted(() => {
  getDialectsFunc();
  convertDDLFunc();
})

onActivated(() => {
  // debugger
  workbookDataLeft.value = {}
})
</script>

<style scoped lang="scss">

.app-container {
  height: calc(100vh - 40px);
  //padding: 10px;
  box-sizing: border-box;

  :deep(.splitpanes__splitter) {
    background-color: #d9e9fa;
  }

  .upload-demo {
    display: inline-block;
    margin-left: 10px;
  }

  :deep(.el-row) {
    height: 100%;

    .el-col {
      height: 100%;

    }
  }

  :deep(.el-form-item) {
    margin-bottom: 10px;
  }

  .univer-sheet {
    border: 1px solid #dfe4ed;
  }

  .converting {
    display: inline-block;
    position: absolute;
    transform: scale(0.7);
    left: 50%;
    bottom: -16px;
    margin-left: -3px;
    color: red;
  }
}

.code-mirror {
  //border: 1px solid rgb(228, 228, 228);
  //flex-shrink: 0;
  height: 100%;

  :deep(.cm-editor) {
    height: 100%;
    font-size: 14px;
    line-height: 150%;
    font-family: sans-serif, monospace;
  }
}
</style>
