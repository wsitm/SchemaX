<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="驱动名称" prop="jdbcName">
        <el-input
          v-model="queryParams.jdbcName"
          placeholder="请输入驱动名称"
          size="small"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
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
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
          >修改
          </el-button>
          <el-button
            type="text"
            :icon="scope.row.isLoaded ? 'el-icon-remove-outline' : 'el-icon-circle-plus-outline'"
            @click="handleLoad(scope.row)"
          >{{ scope.row.isLoaded ? '卸载' : '安装' }}
          </el-button>
          <el-button
            type="text"
            icon="el-icon-delete"
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
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改驱动管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
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

<script>
import {addJdbc, delJdbc, getJdbc, listJdbc, loadJdbc, updateJdbc} from "@/api/rdbms/jdbc";

export default {
  name: "Jdbc",
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
      // 驱动管理表格数据
      jdbcList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        jdbcName: null,
        jdbcFile: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        jdbcName: [
          {required: true, message: "驱动名称不能为空", trigger: "change"}
        ],
        driverClass: [
          {required: true, message: "驱动类不能为空", trigger: "change"}
        ],
        jdbcFile: [
          {required: true, message: "驱动文件不能为空", trigger: "change"}
        ],
      },
      uploadUrl: import.meta.env.VITE_APP_BASE_API + "/rdbms/jdbc/upload", // 上传文件服务器地址
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询驱动管理列表 */
    getList() {
      this.loading = true;
      listJdbc(this.queryParams).then(response => {
        this.jdbcList = response.rows;
        this.total = response.total;
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
        jdbcId: null,
        jdbcName: null,
        jdbcFile: null,
        driverClass: null
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
      this.ids = selection.map(item => item.jdbcId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加驱动管理";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const jdbcId = row.jdbcId || this.ids
      getJdbc(jdbcId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改驱动管理";
      });
    },

    /** 装卸按钮操作 */
    handleLoad(row) {
      const jdbcId = row.jdbcId
      const action = row.isLoaded ? 'unload' : 'load';
      loadJdbc(jdbcId, action).then(response => {
        this.$modal.notifySuccess((row.isLoaded ? '卸载' : '安装') + "成功");
        this.getList();
      });
    },

    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.jdbcId != null) {
            updateJdbc(this.form).then(response => {
              this.$modal.notifySuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addJdbc(this.form).then(response => {
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
      const jdbcIds = row.jdbcId || this.ids;
      this.$modal.confirm('是否确认删除驱动管理编号为"' + jdbcIds + '"的数据项？').then(function () {
        return delJdbc(jdbcIds);
      }).then(() => {
        this.getList();
        this.$modal.notifySuccess("删除成功");
      }).catch(() => {
      });
    },
    /** 导出按钮操作 */
    // handleExport() {
    //   this.download('rdbms/jdbc/export', {
    //     ...this.queryParams
    //   }, `jdbc_${new Date().getTime()}.xlsx`)
    // }
  }
};
</script>
