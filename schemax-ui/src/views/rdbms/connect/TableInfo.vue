<template>
  <div class="app-container">
    <el-tabs tab-position="left">
      <el-tab-pane label="表信息" :lazy="true">
        <base-info v-loading="loading" :table-info-list="tableInfoList"/>
      </el-tab-pane>
      <el-tab-pane label="表结构" :lazy="true">
        <univer-sheet v-loading="loading" :worksheet-data="workbookData"/>
      </el-tab-pane>
      <el-tab-pane label="DDL语句" :lazy="true">
        <DDL ref="ddlRef" :connect-id="connectId" :driverClass="driverClass"/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import {computed, ref} from 'vue'
import {getTableInfo} from "@/api/rdbms/connect";
import BaseInfo from "./BaseInfo/index.vue";
import UniverSheet from "../components/UniverSheet/index.vue";
import DDL from "./DDL/index.vue";
import {tableInfoToWorkbookData} from "@/views/rdbms/connect/data";

const props = defineProps({
  connectId: Number,
  driverClass: String
})

const loading = ref(false)
const tableInfoList = ref([])
const ddlRef = ref()

const workbookData = computed(() => {
  return tableInfoToWorkbookData(tableInfoList.value);
})

const getTableInfoFunc = (connectId) => {
  loading.value = true;
  getTableInfo(connectId).then(res => {
    tableInfoList.value = res.data;
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
</style>
