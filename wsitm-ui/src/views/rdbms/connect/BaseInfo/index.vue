<template>
  <div>
    <el-form ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="关键字" prop="search">
        <el-input
            v-model="search"
            placeholder="请输入关键字"
            clearable
            style="width: 240px"
        >
          <i
              class="el-icon-search el-input__icon"
              slot="suffix">
          </i>
        </el-input>
      </el-form-item>
    </el-form>

    <el-table
        ref="table"
        :data="result"
        :height="height"
        stripe
        style="width: 100%">
      <el-table-column
          type="index"
          label="序号"
          width="100">
      </el-table-column>
      <el-table-column
          prop="schema"
          width="200"
          label="模式">
      </el-table-column>
      <el-table-column
          prop="tableName"
          label="表名">
      </el-table-column>
      <el-table-column
          prop="comment"
          label="描述">
      </el-table-column>
    </el-table>
  </div>
</template>

<script>

export default {
  name: "BaseInfo",
  props: {
    tableInfoList: {
      type: Array,
      default: []
    }
  },
  data() {
    return {
      search: ''
    }
  },
  // watch: {
  //   tableInfoList: function (value) {
  //     console.log(value)
  //   }
  // },
  computed: {
    result() {
      if (this.search) {
        return this.tableInfoList.filter(item => {
          let item2 = {...item};
          let flag1 = item2.schema?.toLowerCase().indexOf(this.search.toLowerCase()) >= 0;
          let flag2 = item2.tableName?.toLowerCase().indexOf(this.search.toLowerCase()) >= 0;
          let flag3 = item2.comment?.toLowerCase().indexOf(this.search.toLowerCase()) >= 0;
          return flag1 || flag2 || flag3;
        })
      }
      return this.tableInfoList;
    },
    height(){
      return window.innerHeight - 140;
    }
  },
  created() {

  },
  methods: {}
};
</script>
