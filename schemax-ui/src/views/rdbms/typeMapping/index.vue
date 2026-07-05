<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="queryParams" ref="queryFormRef" :inline="true" label-width="auto">
      <el-form-item label="源库" prop="sourceDatabase">
        <el-select v-model="queryParams.sourceDatabase" filterable clearable placeholder="全部" style="width: 170px">
          <el-option v-for="item in databases" :key="'q-s-' + item.database" :label="item.label || item.database" :value="item.database"/>
        </el-select>
      </el-form-item>
      <el-form-item label="目标库" prop="targetDatabase">
        <el-select v-model="queryParams.targetDatabase" filterable clearable placeholder="全部" style="width: 170px">
          <el-option v-for="item in databases" :key="'q-t-' + item.database" :label="item.label || item.database" :value="item.database"/>
        </el-select>
      </el-form-item>
      <el-form-item label="源类型" prop="sourceType">
        <el-input v-model="queryParams.sourceType" clearable placeholder="NUMBER" @keyup.enter="handleQuery"/>
      </el-form-item>
      <el-form-item label="启用" prop="enabled">
        <el-select v-model="queryParams.enabled" clearable placeholder="全部" style="width: 100px">
          <el-option label="是" :value="1"/>
          <el-option label="否" :value="0"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain :icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain :icon="Edit" :disabled="single" @click="handleUpdate">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain :icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
      <right-toolbar :showSearch="showSearch" @update:showSearch="value => showSearch = value" @queryTable="getList"/>
    </el-row>

    <el-table v-loading="loading" :data="ruleList" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="规则名称" prop="ruleName" min-width="190" show-overflow-tooltip/>
      <el-table-column label="源库" prop="sourceDatabase" min-width="140" show-overflow-tooltip/>
      <el-table-column label="目标库" prop="targetDatabase" min-width="140" show-overflow-tooltip/>
      <el-table-column label="源类型" prop="sourceType" width="120"/>
      <el-table-column label="目标类型" prop="targetType" width="120"/>
      <el-table-column label="长度" width="120">
        <template #default="scope">{{ strategyText(scope.row.lengthStrategy, scope.row.lengthValue) }}</template>
      </el-table-column>
      <el-table-column label="精度" width="120">
        <template #default="scope">{{ strategyText(scope.row.precisionStrategy, scope.row.precisionValue) }}</template>
      </el-table-column>
      <el-table-column label="小数位" width="120">
        <template #default="scope">{{ strategyText(scope.row.scaleStrategy, scope.row.scaleValue) }}</template>
      </el-table-column>
      <el-table-column label="优先级" prop="priority" width="90" align="center"/>
      <el-table-column label="状态" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'">
            {{ scope.row.enabled === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内置" width="90" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.builtin === 1" type="warning">是</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="220" align="center">
        <template #default="scope">
          <el-button type="primary" link :icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button type="primary" link :icon="CopyDocument" @click="handleCopy(scope.row)">复制</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>queryParams.pageSize"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"/>

    <el-dialog :title="title" v-model="open" width="760px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="115px">
        <el-row :gutter="12">
          <el-col :span="16">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model="form.ruleName" placeholder="请输入规则名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用" prop="enabled">
              <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="源数据库" prop="sourceDatabase">
              <el-select v-model="form.sourceDatabase" filterable placeholder="请选择" style="width: 100%">
                <el-option v-for="item in databases" :key="'f-s-' + item.database" :label="item.label || item.database" :value="item.database"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标数据库" prop="targetDatabase">
              <el-select v-model="form.targetDatabase" filterable placeholder="请选择" style="width: 100%">
                <el-option v-for="item in databases" :key="'f-t-' + item.database" :label="item.label || item.database" :value="item.database"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="源类型" prop="sourceType">
              <el-input v-model="form.sourceType" placeholder="NUMBER"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标类型" prop="targetType">
              <el-input v-model="form.targetType" placeholder="decimal"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="长度策略" prop="lengthStrategy">
              <el-select v-model="form.lengthStrategy" style="width: 100%">
                <el-option v-for="item in strategies" :key="'l-' + item.value" :label="item.label" :value="item.value"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label-width="0">
              <el-input-number v-model="form.lengthValue" :min="0" controls-position="right" style="width: 100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="精度策略" prop="precisionStrategy">
              <el-select v-model="form.precisionStrategy" style="width: 100%">
                <el-option v-for="item in strategies" :key="'p-' + item.value" :label="item.label" :value="item.value"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label-width="0">
              <el-input-number v-model="form.precisionValue" :min="0" controls-position="right" style="width: 100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="小数位策略" prop="scaleStrategy">
              <el-select v-model="form.scaleStrategy" style="width: 100%">
                <el-option v-for="item in strategies" :key="'s-' + item.value" :label="item.label" :value="item.value"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label-width="0">
              <el-input-number v-model="form.scaleValue" :min="0" controls-position="right" style="width: 100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="form.priority" :min="1" controls-position="right" style="width: 100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2"/>
        </el-form-item>
        <el-divider content-position="left">测试映射</el-divider>
        <el-row :gutter="12">
          <el-col :span="7">
            <el-input v-model="testForm.sourceType" placeholder="源类型"/>
          </el-col>
          <el-col :span="5">
            <el-input-number v-model="testForm.length" :min="0" placeholder="长度" style="width: 100%"/>
          </el-col>
          <el-col :span="5">
            <el-input-number v-model="testForm.scale" :min="0" placeholder="小数位" style="width: 100%"/>
          </el-col>
          <el-col :span="4">
            <el-button type="primary" :icon="MagicStick" @click="handleTest">测试</el-button>
          </el-col>
        </el-row>
        <el-alert v-if="testResult" class="test-result" :closable="false" type="success" show-icon
                  :title="testResult.matched ? `${testResult.sourceType} -> ${testResult.typeExpression}，命中规则：${testResult.ruleName}` : '未命中规则，将使用默认转换逻辑。'"/>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TypeMapping">
import {getCurrentInstance, onMounted, reactive, ref} from 'vue'
import {ElMessageBox} from 'element-plus'
import {CopyDocument, Delete, Edit, MagicStick, Plus, Refresh, Search} from '@element-plus/icons-vue'
import {
  addTypeMapping,
  delTypeMapping,
  getTypeMapping,
  getTypeMappingDatabases,
  listTypeMapping,
  testTypeMapping,
  updateTypeMapping
} from '@/api/rdbms/typeMapping'

const {proxy} = getCurrentInstance()

const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const ruleList = ref([])
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const open = ref(false)
const title = ref('')
const databases = ref([])
const testResult = ref(null)

const strategies = [
  {label: '保留', value: 'KEEP'},
  {label: '清空', value: 'DROP'},
  {label: '固定值', value: 'FIXED'},
  {label: '最大限制', value: 'LIMIT_MAX'}
]

const queryFormRef = ref()
const formRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sourceDatabase: null,
  targetDatabase: null,
  sourceType: null,
  enabled: null
})

const form = ref({})
const testForm = reactive({
  sourceType: null,
  length: null,
  scale: null
})

const rules = {
  ruleName: [{required: true, message: '规则名称不能为空', trigger: 'blur'}],
  sourceDatabase: [{required: true, message: '源数据库不能为空', trigger: 'change'}],
  targetDatabase: [{required: true, message: '目标数据库不能为空', trigger: 'change'}],
  sourceType: [{required: true, message: '源类型不能为空', trigger: 'blur'}],
  targetType: [{required: true, message: '目标类型不能为空', trigger: 'blur'}]
}

const reset = () => {
  form.value = {
    ruleId: null,
    ruleName: null,
    sourceDatabase: '*',
    targetDatabase: '*',
    sourceType: null,
    targetType: null,
    lengthStrategy: 'KEEP',
    lengthValue: null,
    precisionStrategy: 'KEEP',
    precisionValue: null,
    scaleStrategy: 'KEEP',
    scaleValue: null,
    priority: 100,
    enabled: 1,
    builtin: 0,
    remark: null
  }
  testForm.sourceType = null
  testForm.length = null
  testForm.scale = null
  testResult.value = null
  formRef.value?.resetFields()
}

const getDatabases = () => {
  getTypeMappingDatabases().then(res => {
    databases.value = res.data || []
  })
}

const getList = () => {
  loading.value = true
  listTypeMapping(queryParams).then(res => {
    ruleList.value = res.rows
    total.value = res.total
  }).finally(() => {
    loading.value = false
  })
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleSelectionChange = (selection) => {
  ids.value = selection.map(item => item.ruleId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

const handleAdd = () => {
  reset()
  title.value = '新增类型映射'
  open.value = true
}

const handleUpdate = (row) => {
  reset()
  const ruleId = row.ruleId || ids.value[0]
  getTypeMapping(ruleId).then(res => {
    form.value = res.data
    title.value = '编辑类型映射'
    open.value = true
  })
}

const handleCopy = (row) => {
  reset()
  form.value = {...row, ruleId: null, ruleName: row.ruleName + ' copy', builtin: 0}
  title.value = '复制类型映射'
  open.value = true
}

const handleDelete = (row) => {
  const ruleIds = row.ruleId || ids.value
  ElMessageBox.confirm('是否确认删除类型映射规则：' + ruleIds + '？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => delTypeMapping(ruleIds)).then(() => {
    getList()
    proxy.$modal.notifySuccess('删除成功')
  }).catch(() => {})
}

const submitForm = () => {
  formRef.value?.validate(valid => {
    if (!valid) return
    const action = form.value.ruleId ? updateTypeMapping : addTypeMapping
    action(form.value).then(() => {
      proxy.$modal.notifySuccess(form.value.ruleId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

const cancel = () => {
  open.value = false
  reset()
}

const handleTest = () => {
  const payload = {
    sourceDatabase: form.value.sourceDatabase,
    targetDatabase: form.value.targetDatabase,
    sourceType: testForm.sourceType || form.value.sourceType,
    length: testForm.length,
    scale: testForm.scale
  }
  testTypeMapping(payload).then(res => {
    testResult.value = res.data
  })
}

const strategyText = (strategy, value) => {
  const item = strategies.find(s => s.value === strategy)
  if (!item) return strategy || ''
  if (strategy === 'FIXED' || strategy === 'LIMIT_MAX') {
    return `${item.label}: ${value ?? '-'}`
  }
  return item.label
}

onMounted(() => {
  getDatabases()
  getList()
})
</script>

<style scoped lang="scss">
.test-result {
  margin-top: 12px;
}
</style>
