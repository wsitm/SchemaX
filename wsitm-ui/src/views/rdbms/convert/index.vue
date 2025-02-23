<template>
  <div class="app-container">
    <el-row :gutter="10">
      <splitpanes class="default-theme">
        <pane size="50">
          <el-col>
            <el-form ref="queryForm" size="small" :inline="true" label-width="80px">
              <el-form-item label="输入类型" prop="inputType">
                <el-select v-model="inputType"
                           filterable
                           disabled
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
              <el-form-item v-if="inputType===2" class="fr">
                <el-button type="primary" icon="el-icon-d-arrow-right" size="small"
                           @click="excelDataToDDL">生成
                </el-button>
                <el-upload
                  class="upload-demo"
                  :action="uploadURL"
                  :multiple="false"
                  :on-success="uploadSuccess"
                  :show-file-list="false">
                  <el-button type="info" icon="el-icon-upload2" size="small">导入</el-button>
                </el-upload>
              </el-form-item>
            </el-form>
            <div style="height: calc(100% - 40px)">
              <codemirror
                v-if="inputType===1"
                ref="codeMirrorLeft"
                v-model="contentLeft"
                :options="cmOption"
                class="code-mirror"
              />
              <univer-sheet
                v-if="inputType===2"
                ref="sheetLeft"
                class="univer-sheet"
                :workbook-data="workbookDataLeft"/>
            </div>
          </el-col>
        </pane>
        <pane size="50">
          <el-col>
            <el-form ref="queryForm" size="small" :inline="true" label-width="100px">
              <el-form-item label="输出类型" prop="outputType" label-width="80px">
                <el-select v-model="outputType"
                           filterable
                           @change="convertDDL"
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
                           @change="convertDDL"
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
            <div id="right_cont" style="height: calc(100% - 40px)">
              <codemirror
                v-if="outputType===1"
                ref="codeMirrorRight"
                v-model="contentRight"
                :options="cmOption"
                class="code-mirror"
              />
              <univer-sheet
                v-if="outputType===2"
                ref="sheetRight"
                class="univer-sheet"
                :workbook-data="workbookDataRight"/>
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

<script>
import {Pane, Splitpanes} from "splitpanes";
import "splitpanes/dist/splitpanes.css";

import XEUtils from "xe-utils";
import UniverSheet from "../components/UniverSheet/index.vue";
import {getDialects} from "@/api/rdbms/connect";
import {convertDDL} from "@/api/rdbms/convert";
import {tableInfoToWorkbookData, workbookDataToTableInfo} from "@/views/rdbms/connect/data";
import {DEFAULT_SHEET_DATA, DEFAULT_WORKBOOK_DATA} from "@/views/rdbms/components/UniverSheet/sheet-data";
import sqlFormatter from '@sqltools/formatter';

import 'codemirror/lib/codemirror.css';
import {codemirror} from 'vue-codemirror';
// language
import 'codemirror/mode/sql/sql.js';
// theme css
import 'codemirror/theme/monokai.css';
// keyMap
import 'codemirror/mode/clike/clike.js'
import 'codemirror/addon/edit/matchbrackets.js'
import 'codemirror/addon/comment/comment.js'
import 'codemirror/addon/dialog/dialog.js'
import 'codemirror/addon/dialog/dialog.css'
import 'codemirror/addon/search/searchcursor.js'
import 'codemirror/addon/search/search.js'
import 'codemirror/keymap/sublime.js'

import {DEMO_SQL} from "./data";

export default {
  name: "Convert",
  components: {Splitpanes, Pane, UniverSheet, codemirror},
  data() {
    return {
      cmOption: {
        tabSize: 4,
        styleActiveLine: true,
        lineNumbers: true,
        line: true,
        mode: 'text/x-sql',
        theme: "monokai",
        //快捷键 可提供三种模式 sublime、emacs、vim
        keyMap: "sublime",
        // 对于长行是否应该滚动或换行。默认为false(滚动)
        lineWrapping: true,
      },
      ENUM: {
        convertType: [{
          type: 1, name: "DDL"
        }, {
          type: 2, name: "Excel"
        }]
      },
      uploadURL: process.env.VUE_APP_BASE_API + "/rdbms/ddl/upload",
      dialects: [],

      inputType: 1,
      outputType: 1,
      outputDatabase: null,

      contentLeft: DEMO_SQL,
      contentRight: "",

      workbookDataLeft: {...DEFAULT_WORKBOOK_DATA},
      tableInfoListLeft: [],
      tableInfoListRight: [],

      converting: false
    }
  },
  watch: {
    contentLeft: function (value) {
      this.convertDDL();
    }
  },
  computed: {
    workbookDataRight() {
      return tableInfoToWorkbookData(this.tableInfoListRight);
    }
  },
  created() {
    this.getDialects();
  },
  mounted() {
    this.convertDDL();
  },
  methods: {
    getDialects() {
      getDialects().then(res => {
        this.dialects = res.data;
        this.outputDatabase = res.data[0].database;
      });
    },

    onLeftTypeChange() {
      if (this.outputType === 2) {
        this.outputType = 1;
      }
    },

    convertDDL: XEUtils.debounce(function () {
      if (this.inputType === 2) {
        this.inputType = 1;
      }
      if (!this.contentLeft) {
        return;
      }
      this.converting = true;
      const params = {
        inputType: this.inputType,
        inputDDL: this.contentLeft,
        tableVOList: this.tableInfoListLeft,
        outputType: this.outputType,
        outputDatabase: this.outputDatabase
      };
      convertDDL(params).then(res => {
        if (res.data) {
          if (this.outputType === 1) {
            this.contentRight = Object.keys(res.data).map(tableName => {
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
            this.tableInfoListRight = res.data;
          }
        }
      }).finally(() => {
        this.converting = false;
      });
    }, 200),

    excelDataToDDL() {
      // console.log(this.$refs.sheetLeft.getData());
      this.tableInfoListLeft = workbookDataToTableInfo(this.$refs.sheetLeft.getData());
      this.convertDDL();
    },

    uploadSuccess(res, file) {
      if (res.data) {
        this.workbookDataLeft = {
          ...DEFAULT_WORKBOOK_DATA,
          sheets: XEUtils.objectMap(res.data, item => {
            return {...DEFAULT_SHEET_DATA, ...item};
          })
        };
      }
    }
  }
};
</script>

<style scoped lang="scss">

.app-container {
  height: calc(100vh - 50px);
  //padding: 10px;
  box-sizing: border-box;

  ::v-deep .splitpanes__splitter {
    background-color: #d9e9fa;
  }

  .upload-demo {
    display: inline-block;
    margin-left: 10px;
  }

  ::v-deep .el-row {
    height: 100%;

    .el-col {
      height: 100%;

    }
  }

  ::v-deep .el-form-item {
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
  }
}


.code-mirror {
  //border: 1px solid rgb(228, 228, 228);
  //flex-shrink: 0;
  height: 100%;

  ::v-deep .CodeMirror {
    height: 100%;
    font-size: 14px;
    line-height: 150%;
    font-family: sans-serif, monospace;
  }
}
</style>
