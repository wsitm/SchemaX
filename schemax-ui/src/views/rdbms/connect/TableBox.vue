<template>
  <div class="app-container">
    <el-tabs tab-position="left" type="border-card">
      <el-tab-pane label="连接概览" :lazy="true">
        <div style="padding: 10px; height: 100%; display: flex; flex-direction: column;">
          <el-descriptions title="基本信息" :column="2" border >
            <el-descriptions-item label="连接ID">{{ connectDetail.connectId }}</el-descriptions-item>
            <el-descriptions-item label="连接名称">{{ connectDetail.connectName }}</el-descriptions-item>
            <el-descriptions-item label="驱动名称">{{ connectDetail.jdbcName }}</el-descriptions-item>
            <el-descriptions-item label="JDBC URL">{{ connectDetail.jdbcUrl }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ connectDetail.username }}</el-descriptions-item>
            <el-descriptions-item label="密码">{{ connectDetail.password }}</el-descriptions-item>
            <el-descriptions-item label="表数量">{{ connectDetail.tableCount }}</el-descriptions-item>
            <el-descriptions-item label="过滤条件">{{ connectDetail.wildcard }}</el-descriptions-item>
<!--            <el-descriptions-item label="创建时间">{{ connectDetail.createTime }}</el-descriptions-item>-->
          </el-descriptions>
          <el-divider />
          <div style="flex: 1; overflow: hidden;">
             <base-info v-loading="loading" :tableInfoList="tableInfoList"/>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="结构信息" :lazy="true">
        <table-info v-loading="loading" :tableInfoList="tableInfoList" :template-list="templateList"/>
      </el-tab-pane>
      <el-tab-pane label="SQL脚本" :lazy="true">
        <DDL :connect-id="connectId" :driverClass="driverClass"/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {getConnect, getTableInfo, listConnectTemplate} from "@/api/rdbms/connect";
import BaseInfo from "./BaseInfo/index.vue";
import TableInfo from "@/views/rdbms/connect/TableInfo/index.vue";
import DDL from "./DDL/index.vue";

const props = defineProps({
  connectId: Number,
  driverClass: String,
  connectInfo: {
    type: Object,
    default: () => ({})
  }
})

const loading = ref(false)
const tableInfoList = ref([])
const templateList = ref([])
const connectDetail = ref({})
// const ddlRef = ref()

const getTableInfoFunc = (connectId) => {
  loading.value = true;
  connectDetail.value = props.connectInfo || {};

  Promise.all([
    getTableInfo(connectId),
    listConnectTemplate(connectId),
    getConnect(connectId)
  ]).then(([tableRes, templateRes, connectRes]) => {
    tableInfoList.value = tableRes.data || [];
    templateList.value = templateRes.data || [];
    if (connectRes.data) {
      connectDetail.value = { ...props.connectInfo, ...connectRes.data };
    }
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
