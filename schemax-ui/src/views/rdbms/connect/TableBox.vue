<template>
  <div class="app-container">
    <el-tabs tab-position="left">
      <el-tab-pane label="表信息" :lazy="true">
        <base-info v-loading="loading" :tableInfoList="tableInfoList"/>
      </el-tab-pane>
      <el-tab-pane label="表结构" :lazy="true">
        <table-info v-loading="loading" :tableInfoList="tableInfoList" :template-list="templateList"/>
      </el-tab-pane>
      <el-tab-pane label="DDL语句" :lazy="true">
        <DDL :connect-id="connectId" :driverClass="driverClass"/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {getTableInfo, listConnectTemplate} from "@/api/rdbms/connect";
import BaseInfo from "./BaseInfo/index.vue";
import TableInfo from "@/views/rdbms/connect/TableInfo/index.vue";
import DDL from "./DDL/index.vue";

const props = defineProps({
  connectId: Number,
  driverClass: String
})

const loading = ref(false)
const tableInfoList = ref([])
const templateList = ref([])
// const ddlRef = ref()

const getTableInfoFunc = (connectId) => {
  loading.value = true;
  Promise.all([
    getTableInfo(connectId),
    listConnectTemplate(connectId)
  ]).then(([tableRes, templateRes]) => {
    tableInfoList.value = tableRes.data || [];
    templateList.value = templateRes.data || [];
  }).finally(() => {
    loading.value = false;
  });
}

onMounted(() => {
  getTableInfoFunc(props.connectId)
})

// defineExpose({
//   getTableInfo: getTableInfoFunc
// })
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
</style>
