<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="驱动名称" prop="jdbcName">
        <el-input
          v-model="queryParams.jdbcName"
          placeholder="请输入驱动名称"
          size="small"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
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
      <!--            :icon="Download"-->
      <!--            @click="handleExport"-->
      <!--        >导出-->
      <!--        </el-button>-->
      <!--      </el-col>-->
      <right-toolbar :showSearch="showSearch" @update:showSearch="value => showSearch = value"
                     @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading"
              :data="jdbcList"
              border stripe
              @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="驱动ID" align="center" prop="jdbcId" width="100" show-overflow-tooltip/>
      <el-table-column label="驱动名称" align="center" prop="jdbcName" show-overflow-tooltip/>
      <el-table-column label="驱动类" align="center" prop="driverClass" show-overflow-tooltip/>
      <el-table-column label="驱动文件" align="center" prop="jdbcFile" show-overflow-tooltip/>
      <el-table-column label="装载" align="center" prop="isLoad" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.isLoaded" type="success">已装载</el-tag>
          <el-tag v-else type="info">未装载</el-tag>
        </template>
      </el-table-column>
      <!--      <el-table-column label="创建用户" align="center" prop="createBy"/>-->
      <el-table-column label="创建时间" align="center" prop="createTime" width="160"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button
            type="primary"
            link
            :icon="Edit"
            @click="handleUpdate(scope.row)"
          >修改
          </el-button>
          <el-button
            type="primary"
            link
            :icon="scope.row.isLoaded ? Remove : 'CirclePlus'"
            @click="handleLoad(scope.row)"
          >{{ scope.row.isLoaded ? '卸载' : '安装' }}
          </el-button>
          <el-button
            type="primary"
            link
            :icon="Delete"
            @click="handleDelete(scope.row)"
          >删除
          </el-button>
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

    <!-- 添加或修改驱动管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" style="margin-top: 15px;">
        <el-form-item label="驱动名称" prop="jdbcName">
          <el-input v-model="form.jdbcName" placeholder="请输入驱动名称"/>
        </el-form-item>
        <el-form-item label="驱动类" prop="driverClass">
          <el-input v-model="form.driverClass" placeholder="请输入驱动类"/>
        </el-form-item>
        <el-form-item label="驱动文件" prop="jdbcFile">
          <file-upload v-model="form.jdbcFile"
                       :upload-url="uploadUrl"
                       :file-size="100"
                       :file-type="['jar']"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Jdbc">
import {getCurrentInstance, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Delete, Edit, Plus, Refresh, Remove, Search} from '@element-plus/icons-vue'
import {addJdbc, delJdbc, getJdbc, listJdbc, loadJdbc, updateJdbc} from "@/api/rdbms/jdbc";

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
// 驱动管理表格数据
const jdbcList = ref([])
// 弹出层标题
const title = ref("")
// 是否显示弹出层
const open = ref(false)
// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  jdbcName: null,
  jdbcFile: null,
})

const queryFormRef = ref()
const formRef = ref()

// 表单参数
const form = ref({})

// 表单校验
const rules = {
  jdbcName: [
    {required: true, message: "驱动名称不能为空", trigger: "change"}
  ],
  driverClass: [
    {required: true, message: "驱动类不能为空", trigger: "change"}
  ],
  jdbcFile: [
    {required: true, message: "驱动文件不能为空", trigger: "change"}
  ],
}

const uploadUrl = import.meta.env.VITE_APP_BASE_API + "/rdbms/jdbc/upload" // 上传文件服务器地址

/** 查询驱动管理列表 */
const getList = () => {
  loading.value = true;
  listJdbc(queryParams).then(response => {
    jdbcList.value = response.rows;
    total.value = response.total;
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
    jdbcId: null,
    jdbcName: null,
    jdbcFile: null,
    driverClass: null
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
  ids.value = selection.map(item => item.jdbcId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  open.value = true;
  title.value = "添加驱动管理";
}

/** 修改按钮操作 */
const handleUpdate = (row) => {
  reset();
  const jdbcId = row.jdbcId || ids.value
  getJdbc(jdbcId).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改驱动管理";
  });
}

/** 装卸按钮操作 */
const handleLoad = (row) => {
  const jdbcId = row.jdbcId
  const action = row.isLoaded ? 'unload' : 'load';
  loadJdbc(jdbcId, action).then(response => {
    ElMessage.success((row.isLoaded ? '卸载' : '安装') + "成功");
    getList();
  });
}

/** 提交按钮 */
const submitForm = () => {
  if (formRef.value) {
    formRef.value.validate((valid) => {
      if (valid) {
        if (form.value.jdbcId != null) {
          updateJdbc(form.value).then(response => {
            ElMessage.success("修改成功");
            open.value = false;
            getList();
          });
        } else {
          addJdbc(form.value).then(response => {
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
  const jdbcIds = row.jdbcId || ids.value;
  ElMessageBox.confirm('是否确认删除驱动管理编号为"' + jdbcIds + '"的数据项？', "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(function () {
    return delJdbc(jdbcIds);
  }).then(() => {
    getList();
    ElMessage.success("删除成功");
  }).catch(() => {
  });
}

/** 导出按钮操作 */
// const handleExport = () => {
//   proxy.$download('rdbms/jdbc/export', {
//     ...queryParams
//   }, `jdbc_${new Date().getTime()}.xlsx`)
// }

// 页面加载时获取数据
getList();
</script>
