<template>
  <div style="height: 100%;">
    <el-form ref="queryFormRef" :inline="true" label-width="auto">
      <el-form-item label="关键字" prop="search">
        <el-input
          v-model="search"
          placeholder="请输入关键字"
          clearable
          style="width: 240px"
        >
          <template #suffix>
            <i class="el-icon-search el-input__icon"></i>
          </template>
        </el-input>
      </el-form-item>
    </el-form>

    <el-table
      ref="tableRef"
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

<script setup>
import {computed, ref} from 'vue'

const props = defineProps({
  tableInfoList: {
    type: Array,
    default: () => []
  }
})

const search = ref('')
const tableRef = ref()
const queryFormRef = ref()

const result = computed(() => {
  if (search.value) {
    return props.tableInfoList.filter(item => {
      let item2 = {...item};
      let flag1 = item2.schema?.toLowerCase().indexOf(search.value.toLowerCase()) >= 0;
      let flag2 = item2.tableName?.toLowerCase().indexOf(search.value.toLowerCase()) >= 0;
      let flag3 = item2.comment?.toLowerCase().indexOf(search.value.toLowerCase()) >= 0;
      return flag1 || flag2 || flag3;
    })
  }
  return props.tableInfoList;
})

const height = computed(() => {
  return window.innerHeight - 130;
})
</script>
