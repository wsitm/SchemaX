<template>
  <div style="height: 100%">
    <el-form ref="queryForm" size="small" :inline="true" label-width="100px">
      <el-form-item label="数据库方言" prop="search">
        <el-select v-model="database"
                   filterable
                   @change="getTableDDL"
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

    <div style="height: calc(100% - 60px)">
      <codemirror
        v-loading="loading"
        ref="codeMirror"
        v-model="content"
        :extensions="extensions"
        class="code-mirror"
      />
    </div>
  </div>
</template>

<script>

import {getDialects, getTableDDL} from "@/api/rdbms/connect";
import sqlFormatter from '@sqltools/formatter';

// import 'codemirror/lib/codemirror.css';
import {Codemirror} from 'vue-codemirror';
// // language
// import 'codemirror/mode/sql/sql.js';
// // theme css
// import 'codemirror/theme/monokai.css';
// // keyMap
// import 'codemirror/mode/clike/clike.js'
// import 'codemirror/addon/edit/matchbrackets.js'
// import 'codemirror/addon/comment/comment.js'
// import 'codemirror/addon/dialog/dialog.js'
// import 'codemirror/addon/dialog/dialog.css'
// import 'codemirror/addon/search/searchcursor.js'
// import 'codemirror/addon/search/search.js'
// import 'codemirror/keymap/sublime.js'
import {StandardSQL} from "@codemirror/lang-sql";
import {monokai} from "@uiw/codemirror-theme-monokai";

export default {
  name: "DDL",
  components: {Codemirror},
  props: {
    connectId: Number,
    driverClass: String
  },
  data() {
    return {
      extensions: [StandardSQL, monokai],
      // cmOption: {
      //   tabSize: 4,
      //   styleActiveLine: true,
      //   lineNumbers: true,
      //   line: true,
      //   mode: 'text/x-sql',
      //   theme: "monokai",
      //   //快捷键 可提供三种模式 sublime、emacs、vim
      //   keyMap: "sublime",
      //   // 对于长行是否应该滚动或换行。默认为false(滚动)
      //   lineWrapping: true
      // },
      loading: false,
      database: null,
      dialects: [],
      content: ""
    }
  },
  // watch: {
  //   tableInfoList: function (value) {
  //     console.log(value)
  //   }
  // },
  created() {
    this.getDialects();
  },
  methods: {
    getDialects() {
      getDialects().then(res => {
        this.dialects = res.data;
        let item = res.data.find(item => item.driver === this.driverClass);
        // console.log(item)
        if (item) {
          this.database = item.database;
        } else {
          this.database = res.data[0].database;
        }
        this.getTableDDL();
      });
    },
    getTableDDL() {
      this.loading = true;
      getTableDDL(this.connectId, this.database).then(res => {
        if (res.data) {
          this.content = Object.keys(res.data).map(tableName => {
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
        }
      }).finally(() => {
        this.loading = false;
      });
    }
  }
};
</script>

<style scoped lang="scss">
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
