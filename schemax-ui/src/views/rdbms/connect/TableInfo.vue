<template>
  <div class="app-container">
    <el-tabs tab-position="left">
      <el-tab-pane label="表信息" :lazy="true">
        <base-info v-loading="loading" :table-info-list="tableInfoList"/>
      </el-tab-pane>
      <el-tab-pane label="表结构" :lazy="true">
        <univer-sheet v-loading="loading" :workbook-data="workbookData"/>
      </el-tab-pane>
      <el-tab-pane label="DDL语句" :lazy="true">
        <DDL :connect-id="connectId" :driverClass="driverClass"/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import {getTableInfo} from "@/api/rdbms/connect";
import BaseInfo from "./BaseInfo/index.vue";
import UniverSheet from "../components/UniverSheet/index.vue";
import DDL from "./DDL/index.vue";
import {tableInfoToWorkbookData} from "@/views/rdbms/connect/data";

export default {
  name: "TableInfo",
  components: {BaseInfo, UniverSheet, DDL},
  props: {
    connectId: String,
    driverClass: String
  },
  data() {
    return {
      loading: false,
      // connectId: null,
      // driverClass: null,
      tableInfoList: []
    };
  },
  computed: {
    workbookData() {
      return tableInfoToWorkbookData(this.tableInfoList);
    }
  },
  created() {
    // let connectId = this.$route.query?.connectId;
    // if (!connectId) {
    //   this.$modal.notifyError("连接ID不能为空！");
    //   return;
    // }
    // this.connectId = connectId;
    // this.driverClass = this.$route.query?.driverClass;
    // console.log(connectId, this.driver)
    // this.getTableInfo(this.connectId);
  },
  methods: {
    getTableInfo(connectId) {
      this.loading = true;
      getTableInfo(connectId).then(res => {
        this.tableInfoList = res.data;
      }).finally(() => {
        this.loading = false;
      });
    }
  }
};
</script>

<style scoped lang="scss">
.app-container {
  height: calc(100vh - 55px);

  .el-tabs {
    height: 100%;

    ::v-deep .el-tabs__content {
      height: 100%;
    }

    ::v-deep .el-tab-pane {
      height: 100%;
    }
  }
}
</style>
