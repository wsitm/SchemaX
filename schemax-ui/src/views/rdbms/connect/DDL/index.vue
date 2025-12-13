<template>
  <div style="height: 100%">
    <el-form size="small" :inline="true" label-width="100px">
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

    <div style="height: calc(100% - 40px)">
      <codemirror
        v-loading="loading"
        ref="codeMirrorRef"
        v-model="content"
        :extensions="extensions"
        class="code-mirror"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getDialects, getTableDDL } from "@/api/rdbms/connect";
import sqlFormatter from '@sqltools/formatter';

// import 'codemirror/lib/codemirror.css';
import { Codemirror } from 'vue-codemirror';
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
import { StandardSQL } from "@codemirror/lang-sql";
import { monokai } from "@uiw/codemirror-theme-monokai";

const props = defineProps({
  connectId: Number,
  driverClass: String
})

const extensions = [StandardSQL, monokai]
const loading = ref(false)
const database = ref(null)
const dialects = ref([])
const content = ref("")
const codeMirrorRef = ref()

const getDialectsFunc = () => {
  getDialects().then(res => {
    dialects.value = res.data;
    let item = res.data.find(item => item.driver === props.driverClass);
    // console.log(item)
    if (item) {
      database.value = item.database;
    } else {
      database.value = res.data[0].database;
    }
    getTableDDLFunc();
  });
}

const getTableDDLFunc = () => {
  loading.value = true;
  getTableDDL(props.connectId, database.value).then(res => {
    if (res.data) {
      content.value = Object.keys(res.data).map(tableName => {
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
    loading.value = false;
  });
}

onMounted(() => {
  getDialectsFunc();
})

defineExpose({
  getTableDDL: getTableDDLFunc
})
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
