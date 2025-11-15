<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="连接名称" prop="connectName">
        <el-input
          v-model="queryParams.connectName"
          placeholder="请输入连接名称"
          size="mini"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="驱动" prop="jdbcId">
        <el-select v-model="queryParams.jdbcId"
                   clearable
                   size="mini"
                   placeholder="请选择驱动">
          <el-option
            v-for="dict in jdbcList"
            :key="dict.jdbcId"
            :label="dict.jdbcName"
            :value="dict.jdbcId"
          ></el-option>
        </el-select>
      </el-form-item>
      <!--      <el-form-item label="用户" prop="username">-->
      <!--        <el-input-->
      <!--            v-model="queryParams.username"-->
      <!--            placeholder="请输入用户"-->
      <!--            clearable-->
      <!--            @keyup.enter.native="handleQuery"-->
      <!--        />-->
      <!--      </el-form-item>-->
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          @click="handleAdd"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          :disabled="single"
          @click="handleUpdate"
        >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          :disabled="multiple"
          @click="handleDelete"
        >删除
        </el-button>
      </el-col>
      <!--      <el-col :span="1.5">-->
      <!--        <el-button-->
      <!--            type="warning"-->
      <!--            plain-->
      <!--            icon="el-icon-download"-->
      <!--            @click="handleExport"-->
      <!--        >导出-->
      <!--        </el-button>-->
      <!--      </el-col>-->
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading"
              :data="connectList"
              border stripe class="table"
              @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="连接ID" align="center" prop="connectId" width="155" show-overflow-tooltip/>
      <el-table-column label="连接名称" align="center" prop="connectName" show-overflow-tooltip/>
      <el-table-column label="驱动名称" align="center" prop="jdbcName" show-overflow-tooltip/>
      <el-table-column label="JDBC URL" align="center" prop="jdbcUrl" width="250" show-overflow-tooltip/>
      <el-table-column label="用户" align="center" prop="username" show-overflow-tooltip/>
      <el-table-column label="密码" align="center" prop="password" show-overflow-tooltip/>
      <el-table-column label="通配符" align="center" prop="wildcard" show-overflow-tooltip/>
      <el-table-column label="缓存" align="center" prop="cacheType" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.cacheType===1" type="success">已加载</el-tag>
          <el-tag v-else-if="scope.row.cacheType===2">加载中</el-tag>
          <el-tag v-else type="info">无缓存</el-tag>
        </template>
      </el-table-column>
      <!--      <el-table-column label="创建时间" align="center" prop="createTime" width="160"/>-->
      <el-table-column label="操作" align="center" fixed="right"
                       class-name="small-padding fixed-width" width="180">
        <template slot-scope="scope">
          <el-tooltip content="查看连接信息详情，基本信息列表，表结构信息，DDL语句信息">
            <el-button
              type="text"
              icon="el-icon-help"
              @click="toPage(scope.row)"
            >详情
            </el-button>
          </el-tooltip>
          <el-tooltip content="测试数据库连接是否正常">
            <el-button
              type="text"
              icon="el-icon-link"
              @click="handleCheck(scope.row)"
            >测试
            </el-button>
          </el-tooltip>
          <el-dropdown size="mini"
                       @command="(command) => handleCommand(command, scope.row)">
            <el-button type="text" icon="el-icon-d-arrow-right">更多</el-button>
            <el-dropdown-menu slot="dropdown">
              <el-tooltip content="刷新缓存，数据库表结构发生变更后需要重新加载信息到缓存" placement="left">
                <el-dropdown-item icon="el-icon-refresh"
                                  command="handleConnectFlush">刷新
                </el-dropdown-item>
              </el-tooltip>
              <el-tooltip content="导出当前连接库的表结构信息" placement="left">
                <el-dropdown-item icon="el-icon-download"
                                  command="handleConnectExport">导出
                </el-dropdown-item>
              </el-tooltip>
              <el-dropdown-item icon="el-icon-edit"
                                command="handleConnectEdit">修改
              </el-dropdown-item>
              <el-dropdown-item icon="el-icon-delete"
                                command="handleConnectRemove">删除
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <!--    <pagination-->
    <!--        v-show="total>0"-->
    <!--        :total="total"-->
    <!--        :page.sync="queryParams.pageNum"-->
    <!--        :limit.sync="queryParams.pageSize"-->
    <!--        @pagination="getList"-->
    <!--    />-->

    <!--  表结构信息呈现  -->
    <el-dialog :title="tableInfo.title"
               :visible.sync="tableInfo.open"
               :fullscreen="true"
               append-to-body
               class="table-info">
      <table-info ref="tableInfo"
                  :connect-id="tableInfo.connectId"
                  :driver-class="tableInfo.driverClass"
      />
    </el-dialog>

    <!-- 添加或修改连接配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="连接名称" prop="connectName">
          <el-input v-model="form.connectName" placeholder="请输入连接名称"/>
        </el-form-item>
        <el-form-item label="驱动" prop="jdbcId">
          <el-select v-model="form.jdbcId" placeholder="请选择驱动" style="width: 100%;">
            <el-option
              v-for="dict in jdbcList"
              :key="dict.jdbcId"
              :label="dict.jdbcName"
              :value="dict.jdbcId"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="Jdbc Url" prop="jdbcUrl">
          <el-input v-model="form.jdbcUrl" type="textarea" placeholder="请输入内容"/>
        </el-form-item>
        <el-form-item label="用户" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户"/>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入密码"/>
        </el-form-item>
        <el-form-item label="通配符" prop="wildcard">
          <el-input v-model="form.wildcard" type="textarea" :rows="4" placeholder="请输入通配符"/>
          <span>
            <strong>注</strong>：通配符匹配，匹配包含，
            <strong>?</strong> 表示匹配任何单个，
            <strong>*</strong> 表示匹配任何多个，
            <strong>!</strong> 表示剔除，
            <strong>,</strong> 逗号分隔多个通配符
            <br/>
            <strong>例</strong>："sys_*,!tb_*"，表示以 sys_ 开头，和不以 tb_ 开头的表
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="success" @click="handleCheck(form)">测试</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 导出表结构信息对话框 -->
    <el-dialog :title="exportInfo.title"
               :visible.sync="exportInfo.open"
               width="500px" append-to-body>
      <el-form ref="form" label-width="80px">
        <el-form-item label="通配符" prop="skipStrs">
          <el-input v-model="exportInfo.skipStrs" type="textarea" :rows="4" placeholder="请输入通配符"/>
          <span>
            <strong>注</strong>：通配符匹配，匹配包含，
            <strong>?</strong> 表示匹配任何单个，
            <strong>*</strong> 表示匹配任何多个，
            <strong>!</strong> 表示剔除，
            <strong>,</strong> 逗号分隔多个通配符
            <br/>
            <strong>例</strong>："sys_*,!tb_*"，表示以 sys_ 开头，和不以 tb_ 开头的表
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleExportInfo">导出</el-button>
        <el-button @click="exportInfo.open=false;">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  addConnect,
  checkConnect,
  delConnect,
  flushCache,
  getConnect,
  listConnect,
  updateConnect
} from "@/api/rdbms/connect";
import {listJdbc} from "@/api/rdbms/jdbc";
import TableInfo from "@/views/rdbms/connect/TableInfo.vue";

export default {
  name: "Connect",
  components: {TableInfo},
  dicts: ['sys_common_status'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 连接配置表格数据
      connectList: [],

      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        connectName: null,
        jdbcId: null,
        jdbcUrl: null,
        username: null,
        password: null,
        wildcard: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        connectName: [
          {required: true, message: "连接名称不能为空", trigger: "change"}
        ],
        jdbcId: [
          {required: true, message: "驱动ID不能为空", trigger: "change"}
        ],
        jdbcUrl: [
          {required: true, message: "Jdbc Url不能为空", trigger: "change"}
        ],
        username: [
          {required: true, message: "用户不能为空", trigger: "change"}
        ],
        password: [
          {required: true, message: "密码不能为空", trigger: "change"}
        ],
      },
      // 驱动列表
      jdbcList: [],

      // 倒计时刷新列表
      timer: null,

      // 表结构信息
      tableInfo: {
        open: false,
        title: "表结构信息",
        connectId: null,
        driverClass: null
      },

      // 导出表结构信息
      exportInfo: {
        row: {},
        title: "",
        open: false,
        skipStrs: null,
      }
    };
  },
  created() {
    this.initJdbcList();
    this.getList();
    this.flushList();
  },
  activated() {
    this.flushList();
  },
  methods: {
    /** 定时刷新列表数据 **/
    flushList() {
      clearInterval(this.timer);
      this.timer = setInterval(() => {
        this.getList(false);
      }, 10000);
    },
    /** 初始化驱动列表 **/
    initJdbcList() {
      listJdbc({pageNum: 1, pageSize: 10000})
        .then(response => {
          this.jdbcList = response.data;
        });
    },
    /** 查询连接配置列表 */
    getList(loading = true) {
      this.loading = loading;
      listConnect(this.queryParams).then(response => {
        this.connectList = response.data;
        // this.total = response.total;
      }).finally(() => {
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        connectId: null,
        connectName: null,
        jdbcId: null,
        jdbcUrl: null,
        username: null,
        password: null,
        wildcard: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.connectId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加连接配置";
    },
    /** 操作事件 */
    handleCommand(command, row) {
      switch (command) {
        case "handleConnectFlush":
          this.flushCache(row);
          break;
        case "handleConnectEdit":
          this.handleUpdate(row);
          break;
        case "handleConnectExport":
          this.exportInfo.row = {...row};
          this.exportInfo.title = `${row.connectName}-表结构信息导出`;
          this.exportInfo.open = true;
          break;
        case "handleConnectRemove":
          this.handleDelete(row);
          break;
        default:
          break;
      }
    },

    /** 测试数据库连通性 */
    handleCheck(data) {
      checkConnect(data).then(response => {
        this.$modal.notifySuccess("测试通过");
      });
    },

    /** 测试数据库连通性 */
    flushCache(row) {
      flushCache(row.connectId).then(response => {
        row.cacheType = 2; // 标记成加载中
        this.$modal.notifySuccess("刷新缓存已提交");
      });
    },

    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const connectId = row.connectId || this.ids
      getConnect(connectId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改连接配置";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.connectId != null) {
            updateConnect(this.form).then(response => {
              this.$modal.notifySuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addConnect(this.form).then(response => {
              this.$modal.notifySuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const connectIds = row.connectId || this.ids;
      this.$modal.confirm('是否确认删除连接配置编号为"' + connectIds + '"的数据项？').then(function () {
        return delConnect(connectIds);
      }).then(() => {
        this.getList();
        this.$modal.notifySuccess("删除成功");
      }).catch(() => {
      });
    },
    /** 导出按钮操作 */
    // handleExport() {
    //   this.download('rdbms/connect/export', {
    //     ...this.queryParams
    //   }, `connect_${new Date().getTime()}.xlsx`);
    // },

    /** 跳转页面 **/
    toPage(row) {
      // this.$router.push({
      //   path: "/connect/table-info",
      //   meta: {title: row.name},
      //   query: {
      //     connectId: row.connectId,
      //     driverClass: row.driverClass
      //   }
      // });
      this.tableInfo.open = true;
      this.tableInfo.title = row.connectName + "(" + row.connectId + ")";
      this.tableInfo.connectId = row.connectId;
      this.tableInfo.driverClass = row.driverClass;
      this.$nextTick(() => {
        this.$refs.tableInfo?.getTableInfo(row.connectId);
      })
    },
    /** 导出表结构信息 */
    handleExportInfo() {
      const row = this.exportInfo.row;
      this.download(`rdbms/connect/export/${row.connectId}/tableInfo`,
        {skipStrs: this.exportInfo.skipStrs},
        `表结构信息_${row.connectName}_${new Date().getTime()}.xlsx`,
        {timeout: 60000});
    },
  },
  deactivated() {
    clearInterval(this.timer);
  },
  destroyed() {
    clearInterval(this.timer);
  }
};
</script>

<style scoped lang="scss">
.table {
  // 表格高度不一致，将高度撑开
  ::v-deep .el-table__fixed-right {
    height: 100% !important;
  }
}

.table-info {
  ::v-deep .el-dialog__header {
    border-bottom: 1px solid #dcdee4;
  }

  ::v-deep .el-dialog__body {
    margin: 0 auto;
    padding: 0;
  }
}
</style>
