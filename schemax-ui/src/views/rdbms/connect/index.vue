<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="queryParams" ref="queryFormRef" :inline="true"
             label-width="auto">
      <el-form-item label="连接名称" prop="connectName">
        <el-input
          v-model="queryParams.connectName"
          placeholder="请输入连接名称"
          size="small"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="驱动" prop="jdbcId">
        <el-select v-model="queryParams.jdbcId"
                   clearable
                   size="small"
                   placeholder="请选择驱动"
                   style="width: 150px;">
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
      <!--            @keyup.enter="handleQuery"-->
      <!--        />-->
      <!--      </el-form-item>-->
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          :icon="Plus"
          @click="handleAdd"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          :icon="Edit"
          :disabled="single"
          @click="handleUpdate"
        >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          :icon="Delete"
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
      <right-toolbar :showSearch="showSearch"
                     @update:showSearch="value => showSearch = value"
                     @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading"
              :data="connectList"
              border stripe class="table"
              @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="连接ID" align="center" prop="connectId" width="100" show-overflow-tooltip/>
      <el-table-column label="连接名称" align="center" prop="connectName" show-overflow-tooltip/>
      <el-table-column label="驱动名称" align="center" prop="jdbcName" show-overflow-tooltip/>
      <el-table-column label="JDBC URL" align="center" prop="jdbcUrl" width="250" show-overflow-tooltip/>
      <el-table-column label="用户" align="center" prop="username" width="125" show-overflow-tooltip/>
      <el-table-column label="密码" align="center" prop="password" width="150" show-overflow-tooltip/>
      <el-table-column label="过滤" align="center" prop="wildcard" show-overflow-tooltip/>
      <el-table-column label="数量" align="center" prop="tableCount" width="100"/>
      <!--      <el-table-column label="创建时间" align="center" prop="createTime" width="160"/>-->
      <el-table-column label="操作" align="center" fixed="right"
                       class-name="small-padding fixed-width" width="180">
        <template #default="scope">
          <el-tooltip content="查看连接信息详情，基本信息列表，表结构信息，DDL语句信息">
            <el-button
              type="primary"
              link
              :icon="Help"
              @click="toPage(scope.row)"
            >详情
            </el-button>
          </el-tooltip>
          <el-tooltip content="测试数据库连接是否正常">
            <el-button
              type="primary"
              link
              :icon="Link"
              @click="handleCheck(scope.row)"
            >测试
            </el-button>
          </el-tooltip>
          <el-dropdown size="small"
                       @command="(command) => handleCommand(command, scope.row)" style="vertical-align: middle;">
            <el-button type="primary" link :icon="DArrowRight">更多</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="handleConnectFlush">
                  <el-tooltip content="刷新缓存，数据库表结构发生变更后需要重新加载信息到缓存" placement="left">
                    <el-button type="text" link :icon="Refresh">刷新</el-button>
                  </el-tooltip>
                </el-dropdown-item>
                <el-dropdown-item command="handleConnectExport">
                  <el-tooltip content="导出当前连接库的表结构信息" placement="left">
                    <el-button type="text" link :icon="Download">导出</el-button>
                  </el-tooltip>
                </el-dropdown-item>
                <el-dropdown-item command="handleConnectEdit">
                  <el-tooltip content="修改当前连接信息" placement="left">
                    <el-button type="text" link :icon="Edit">修改</el-button>
                  </el-tooltip>
                </el-dropdown-item>
                <el-dropdown-item command="handleConnectRemove">
                  <el-tooltip content="删除当前连接信息" placement="left">
                    <el-button type="text" link :icon="Delete">删除</el-button>
                  </el-tooltip>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="无数据"></el-empty>
      </template>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!--  表结构信息呈现  -->
    <el-dialog :title="tableInfo.title"
               v-model="tableInfo.open"
               :fullscreen="true"
               append-to-body
               class="table-info">
      <table-info ref="tableInfoRef"
                  :connect-id="tableInfo.connectId"
                  :driver-class="tableInfo.driverClass"
      />
    </el-dialog>

    <!-- 添加或修改连接配置对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="margin-top: 15px;">
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
        <el-form-item label="过滤类型" prop="filterType">
          <el-radio-group v-model="form.filterType">
            <el-radio :label="1">通配符</el-radio>
            <el-radio :label="2">正则表达式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="通配符/正则" prop="wildcard">
          <el-input v-model="form.wildcard" type="textarea" :rows="4" placeholder="请输入通配符"/>
          <span v-if="form.filterType === 1">
            <strong>注</strong>：通配符匹配，匹配包含，
            <strong>?</strong> 表示匹配任何单个，
            <strong>*</strong> 表示匹配任何多个，
            <strong>!</strong> 表示剔除，
            <strong>,</strong> 逗号分隔多个通配符
            <br/>
            <strong>例</strong>："sys_*,!tb_*"，表示以 sys_ 开头，和不以 tb_ 开头的表
          </span>
          <span v-else>
            <strong>注</strong>：正则匹配，匹配包含。
            <strong>例</strong>："^sys_.*"，表示以 sys_ 开头
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="success" @click="handleCheck(form)">测试</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导出表结构信息对话框 -->
    <el-dialog :title="exportInfo.title"
               v-model="exportInfo.open"
               width="500px" append-to-body>
      <el-form ref="exportFormRef" label-width="80px">
        <el-form-item label="过滤类型" prop="filterType">
          <el-radio-group v-model="exportInfo.filterType">
            <el-radio :label="1">通配符</el-radio>
            <el-radio :label="2">正则表达式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="通配符" prop="wildcard">
          <el-input v-model="exportInfo.wildcard" type="textarea" :rows="4" placeholder="请输入通配符"/>
          <span v-if="exportInfo.filterType === 1">
            <strong>注</strong>：通配符匹配，匹配包含，
            <strong>?</strong> 表示匹配任何单个，
            <strong>*</strong> 表示匹配任何多个，
            <strong>!</strong> 表示剔除，
            <strong>,</strong> 逗号分隔多个通配符
            <br/>
            <strong>例</strong>："sys_*,!tb_*"，表示以 sys_ 开头，和不以 tb_ 开头的表
          </span>
          <span v-else>
            <strong>注</strong>：正则匹配，匹配包含。
            <strong>例</strong>："^sys_.*"，表示以 sys_ 开头
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleExportInfo">导出</el-button>
          <el-button @click="exportInfo.open=false;">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {getCurrentInstance, onActivated, onDeactivated, onMounted, reactive, ref} from 'vue'
import {ElForm, ElMessage, ElMessageBox} from 'element-plus'
import {DArrowRight, Delete, Download, Edit, Help, Link, Plus, Refresh, Search} from '@element-plus/icons-vue'

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

const {proxy} = getCurrentInstance()

// 遮罩层
const loading = ref(true)
// 选中数组
const ids = ref([])
// 非单个禁用
const single = ref(true)
// 非多个禁用
const multiple = ref(true)
// 显示搜索条件
const showSearch = ref(true)
// 总条数
const total = ref(0)
// 连接配置表格数据
const connectList = ref([])

// 弹出层标题
const title = ref("")
// 是否显示弹出层
const open = ref(false)
// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  connectName: null,
  jdbcId: null,
  jdbcUrl: null,
  username: null,
  password: null,
  filterType: 1,
  wildcard: null
})

// 表单参数
const form = ref({})

// 表单校验
const rules = {
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
}

// 驱动列表
const jdbcList = ref([])

// 倒计时刷新列表
const timer = ref(null)

// 表结构信息
const tableInfo = reactive({
  open: false,
  title: "表结构信息",
  connectId: null,
  driverClass: null
})

// 导出表结构信息
const exportInfo = reactive({
  row: {},
  title: "",
  open: false,
  filterType: 1,
  wildcard: null,
})

const queryFormRef = ref()
const formRef = ref()
const exportFormRef = ref()
const tableInfoRef = ref()

/** 定时刷新列表数据 **/
const flushList = () => {
  clearInterval(timer.value);
  timer.value = setInterval(() => {
    getList(false);
  }, 10000);
}

/** 初始化驱动列表 **/
const initJdbcList = () => {
  listJdbc({pageNum: 1, pageSize: 10000}).then(response => {
    jdbcList.value = response.rows;
  });
}

/** 查询连接配置列表 */
const getList = (isLoading = true) => {
  loading.value = isLoading;
  listConnect(queryParams).then(response => {
    connectList.value = response.rows;
    total.value = response.total;
  }).finally(() => {
    loading.value = false;
  });
}

// 取消按钮
const cancel = () => {
  open.value = false;
  reset();
}

// 表单重置
const reset = () => {
  form.value = {
    connectId: null,
    connectName: null,
    jdbcId: null,
    jdbcUrl: null,
    username: null,
    password: null,
    filterType: 1,
    wildcard: null,
    // createBy: null,
    createTime: null,
    // updateBy: null,
    // updateTime: null
  };
  formRef.value?.resetFields();
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
}

// 多选框选中数据
const handleSelectionChange = (selection) => {
  ids.value = selection.map(item => item.connectId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  open.value = true;
  title.value = "添加连接配置";
}

/** 操作事件 */
const handleCommand = (command, row) => {
  switch (command) {
    case "handleConnectFlush":
      flushCacheFunc(row);
      break;
    case "handleConnectEdit":
      handleUpdate(row);
      break;
    case "handleConnectExport":
      exportInfo.row = {...row};
      exportInfo.title = `${row.connectName}-表结构信息导出`;
      exportInfo.open = true;
      break;
    case "handleConnectRemove":
      handleDelete(row);
      break;
    default:
      break;
  }
}

/** 测试数据库连通性 */
const handleCheck = (data) => {
  checkConnect(data).then(response => {
    ElMessage.success("测试通过");
  });
}

/** 测试数据库连通性 */
const flushCacheFunc = (row) => {
  flushCache(row.connectId).then(response => {
    row.cacheType = 2; // 标记成加载中
    ElMessage.success("刷新缓存已提交");
  });
}

/** 修改按钮操作 */
const handleUpdate = (row) => {
  reset();
  const connectId = row.connectId || ids.value
  getConnect(connectId).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改连接配置";
  });
}

/** 提交按钮 */
const submitForm = () => {
  if (formRef.value) {
    formRef.value.validate((valid) => {
      if (valid) {
        if (form.value.connectId != null) {
          updateConnect(form.value).then(response => {
            ElMessage.success("修改成功");
            open.value = false;
            getList();
          });
        } else {
          addConnect(form.value).then(response => {
            ElMessage.success("新增成功");
            open.value = false;
            getList();
          });
        }
      }
    });
  }
}

/** 删除按钮操作 */
const handleDelete = (row) => {
  const connectIds = row.connectId || ids.value;
  ElMessageBox.confirm('是否确认删除连接配置编号为"' + connectIds + '"的数据项？', "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(function () {
    return delConnect(connectIds);
  }).then(() => {
    getList();
    ElMessage.success("删除成功");
  }).catch(() => {
  });
}

/** 跳转页面 **/
const toPage = (row) => {
  // this.$router.push({
  //   path: "/connect/table-info",
  //   meta: {title: row.name},
  //   query: {
  //     connectId: row.connectId,
  //     driverClass: row.driverClass
  //   }
  // });
  tableInfo.open = true;
  tableInfo.title = "[ID:" + row.connectId + "] " + row.connectName;
  tableInfo.connectId = row.connectId;
  tableInfo.driverClass = row.driverClass;
  proxy.$nextTick(() => {
    tableInfoRef.value?.getTableInfo(row.connectId);
  })
}

/** 导出表结构信息 */
const handleExportInfo = () => {
  const row = exportInfo.row;
  proxy.$download(`rdbms/connect/export/${row.connectId}/tableInfo`,
    {
      filterType: exportInfo.filterType,
      wildcard: exportInfo.wildcard
    },
    `表结构信息_${row.connectName}_${new Date().getTime()}.xlsx`,
    {timeout: 60000});
}

onMounted(() => {
  initJdbcList();
  getList();
  flushList();
})

onActivated(() => {
  flushList();
})

onDeactivated(() => {
  clearInterval(timer.value);
})
</script>

<style scoped lang="scss">
.table {
  // 表格高度不一致，将高度撑开
  :deep(.el-table__fixed-right) {
    height: 100% !important;
  }
}


.table-info {

  :deep(.el-dialog__body) {
    margin: 0 auto;
    padding: 0;
  }
}
</style>
