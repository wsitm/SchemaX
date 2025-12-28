<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="queryParams" ref="queryFormRef" :inline="true" label-width="auto">
      <el-form-item label="模板名称" prop="tpName">
        <el-input v-model="queryParams.tpName" placeholder="请输入模板名称" size="small" clearable
                  @keyup.enter="handleQuery"/>
      </el-form-item>
      <el-form-item label="模板类型" prop="tpType">
        <el-select v-model="queryParams.tpType" clearable size="small" placeholder="请选择类型" style="width: 150px;">
          <el-option v-for="t in TEMPLATE_TYPE_LIST" :key="t.value" :label="t.label" :value="t.value"/>
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

    <el-table v-loading="loading" :data="templateList" border stripe class="table"
              @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="模板ID" align="center" prop="tpId" width="120"/>
      <el-table-column label="模板名称" align="center" prop="tpName" show-overflow-tooltip/>
      <el-table-column label="模板类型" align="center" prop="tpType" width="200">
        <template #default="scope">
          <el-tag v-if="scope.row.tpType === 1" type="success">excel</el-tag>
          <el-tag v-else-if="scope.row.tpType === 2" type="warning">word</el-tag>
          <el-tag v-else type="info">markdown</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="250"/>
      <el-table-column label="操作" align="center" fixed="right" class-name="small-padding fixed-width" width="220">
        <template #default="scope">
          <el-button type="primary" link :icon="View" @click="handlePreview(scope.row)">预览</el-button>
          <el-button type="primary" link :icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="无数据"></el-empty>
      </template>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page="queryParams.pageNum" :limit="queryParams.pageSize"
                @pagination="getList"/>

    <!-- 添加/编辑弹框 -->
    <el-dialog :title="title" v-model="open" :close-on-click-modal="false" width="1280px" append-to-body>
      <div class="dialog-body">
        <div class="left">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="模板名称" prop="tpName">
              <el-input v-model="form.tpName" placeholder="请输入模板名称"/>
            </el-form-item>
            <el-form-item label="模板类型" prop="tpType">
              <el-select v-model="form.tpType" placeholder="请选择类型" style="width: 100%;" :disabled="isEdit">
                <el-option v-for="t in TEMPLATE_TYPE_LIST" :key="t.value" :label="t.label" :value="t.value"/>
              </el-select>
            </el-form-item>
            <el-form-item label="预设表达式">
              <template #label>
                <span>
                  <el-tooltip content="可拖拽表达式到右侧模板中，或双击表达式回填。" placement="top">
                    <strong>
                      <!-- <el-icon><Warning /></el-icon> -->
                      <span>表达式</span>
                    </strong>
                  </el-tooltip>
                </span>
              </template>
              <el-tree ref="treeRef"
                       :data="PLACEHOLDER_TREE"
                       :props="{ label: 'label', children: 'children' }"
                       node-key="label"
                       default-expand-all
                       :expand-on-click-node="false"
                       @node-click="handleNodeClick">
                <template #default="{ node, data }">
                  <span class="tree-node"
                        :draggable="isLeaf(data)"
                        @dragstart="(e) => handleDragStart(e, data)">
                    {{ node.label }}
                    <el-icon v-if="isLeaf(data)"
                             class="tree-node-icon">
                      <DocumentCopy/>
                    </el-icon>
                  </span>
                </template>
              </el-tree>
            </el-form-item>
          </el-form>
        </div>
        <div class="right" @dragover.prevent @drop="handleDrop">
          <template v-if="form.tpType === 1">
            <div class="editor-title">Excel 模板编辑</div>
            <univer-sheet ref="sheetRef" :worksheet-data="sheetData"/>
          </template>
          <!--          <template v-else-if="form.tpType === 2">-->
          <!--            <div class="editor-title">Word 模板编辑</div>-->
          <!--            <univer-docs ref="docsRef" :work-docs-data="docsData"/>-->
          <!--          </template>-->
          <template v-else>
            <div class="editor-title">Markdown 模板编辑</div>
            <div class="md-editor">
              <!--              <el-input v-model="form.tpContent" type="textarea" :rows="25" placeholder="请输入 Markdown 模板内容"/>-->
              <splitpanes class="default-theme">
                <pane size="50">
                  <codemirror
                    ref="codeMirror"
                    v-model="form.tpContent"
                    :tab-size="2"
                    :extensions="extensions"
                    class="code-mirror"
                  />
                </pane>
                <pane size="50">
                  <div class="md-preview">
                    <!--                    <div class="md-preview-title">预览</div>-->
                    <div class="md-preview-body" v-html="mdHtml"/>
                  </div>
                </pane>
              </splitpanes>
            </div>
          </template>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 预览弹框 -->
    <el-dialog :title="preview.title" v-model="preview.open" width="1200px" append-to-body>
      <div class="preview-body">
        <template v-if="preview.row.tpType === 1">
          <univer-sheet ref="previewSheetRef" :worksheet-data="previewSheetData"/>
        </template>
        <template v-else-if="preview.row.tpType === 2">
          <el-input v-model="preview.row.tpContent" type="textarea" :rows="24" readonly/>
        </template>
        <template v-else>
          <div class="md-preview-body" v-html="previewMdHtml"/>
        </template>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="preview.open = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Template">
import {computed, getCurrentInstance, onMounted, reactive, ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Delete, DocumentCopy, Edit, Plus, Refresh, Search, View} from '@element-plus/icons-vue'
import {addTemplate, delTemplate, getTemplate, listTemplate, updateTemplate} from '@/api/rdbms/template'
import UniverSheet from '@/views/rdbms/components/UniverSheet/index.vue'
// import UniverDocs from '@/views/rdbms/components/UniverDocs/index.vue'
import {Codemirror} from 'vue-codemirror';
import {markdown} from '@codemirror/lang-markdown'
import {monokai} from '@uiw/codemirror-theme-monokai';
import {PLACEHOLDER_TREE, TEMPLATE_TYPE_LIST} from './data'
import {marked} from 'marked'

import "splitpanes/dist/splitpanes.css";
import {Pane, Splitpanes} from "splitpanes";
import {copyTextToClipboard} from "@/utils/rdbms";

const extensions = [markdown(), monokai]

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

const templateList = ref([])

const title = ref('')
const open = ref(false)
const isEdit = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tpName: null,
  tpType: null,
})

const form = ref({})
const rules = {
  tpName: [{required: true, message: '模板名称不能为空', trigger: 'change'}],
  tpType: [{required: true, message: '模板类型不能为空', trigger: 'change'}],
}

const queryFormRef = ref()
const formRef = ref()
const treeRef = ref()

const sheetRef = ref()
const sheetData = ref({})

const previewSheetRef = ref()
const previewSheetData = ref({})


// const docsRef = ref()
// const docsData = ref({})

const preview = reactive({
  open: false,
  title: '',
  row: {},
})

const mdHtml = computed(() => {
  return marked.parse(form.value.tpContent || '')
})

const previewMdHtml = computed(() => {
  return marked.parse(preview.row.tpContent || '')
})

const isLeaf = (data) => {
  return !data.children || data.children.length === 0
}

const handleDragStart = (e, data) => {
  if (!isLeaf(data)) {
    e.preventDefault()
    return
  }
  e.dataTransfer.setData('text/plain', data.label)
}

const insertExpr = async (expr) => {
  if (!expr) return
  if (form.value.tpType === 1) {
    // TODO
    form.value.tpContent = (form.value.tpContent || '') + expr
    ElMessage.success('已插入到模板内容（Excel）')
  }

  if (form.value.tpType === 3) {
    form.value.tpContent = (form.value.tpContent || '') + expr
  }
}

const handleDrop = (e) => {
  const expr = e.dataTransfer.getData('text/plain')
  if (form.value.tpType !== 3) {
    //  TODO code-mirror 自带拖拽插入功能
    insertExpr(expr)
  }
}

const handleNodeClick = (data) => {
  if (!isLeaf(data)) return
  copyTextToClipboard(data.label)
  ElMessage.success('已复制到剪贴板')
}

/** 查询列表 */
const getList = (isLoading = true) => {
  loading.value = isLoading
  listTemplate(queryParams).then(res => {
    templateList.value = res.rows
    total.value = res.total
  }).finally(() => {
    loading.value = false
  })
}

const reset = () => {
  form.value = {
    tpId: null,
    tpName: null,
    tpType: 1,
    tpContent: '',
    createTime: null,
  }
  formRef.value?.resetFields()
  sheetData.value = {}
}

const cancel = () => {
  open.value = false
  reset()
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
  ids.value = selection.map(item => item.tpId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

const handleAdd = () => {
  reset()
  isEdit.value = false
  title.value = '添加模板'
  open.value = true
}

const handleUpdate = (row) => {
  reset()
  isEdit.value = true
  const tpId = row.tpId || ids.value
  getTemplate(tpId).then(res => {
    form.value = res.data
    open.value = true
    title.value = '编辑模板'

    if (form.value.tpType === 1) {
      // Excel 内容若为 JSON，尝试解析
      try {
        sheetData.value = JSON.parse(form.value.tpContent || '{}')?.sheets['sheet-01'];
      } catch (e) {
        sheetData.value = {}
      }
    }
  })
}

const submitForm = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return

    const payload = {...form.value}

    // excel: 保存为 univer workbook json
    if (payload.tpType === 1) {
      try {
        const wb = sheetRef.value?.getData?.()
        payload.tpContent = JSON.stringify(wb || {})
      } catch (e) {
        // fallback
        payload.tpContent = payload.tpContent || ''
      }
    }

    if (payload.tpId != null) {
      updateTemplate(payload).then(() => {
        ElMessage.success('修改成功')
        open.value = false
        getList()
      })
    } else {
      addTemplate(payload).then(() => {
        ElMessage.success('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

const handleDelete = (row) => {
  const tpIds = row.tpId || ids.value
  ElMessageBox.confirm('是否确认删除模板编号为"' + tpIds + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function () {
    return delTemplate(tpIds)
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {
  })
}

const handlePreview = (row) => {
  preview.row = {...row}
  preview.title = `预览：${row.tpName}`
  preview.open = true

  if (row.tpType === 1) {
    try {
      previewSheetData.value = JSON.parse(row.tpContent || '{}')?.sheets['sheet-01'];
    } catch (e) {
      previewSheetData.value = {}
    }
  }
}

watch(() => open.value, (val) => {
  if (!val) return
  // 默认类型为 excel 时，初始化一个空的 sheetData
  if (form.value.tpType === 1 && !sheetData.value) {
    sheetData.value = {}
  }
})

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.dialog-body {
  display: flex;
  height: 620px;
}

.left {
  width: 25%;
  padding-right: 12px;
  border-right: 1px solid #eee;
  overflow: auto;
}

.right {
  width: 75%;
  padding-left: 12px;
  overflow: hidden;
}

.editor-title {
  font-weight: 600;
  margin-bottom: 10px;
}

.tree-node {
  user-select: none;

  .tree-node-icon {
    display: none;
  }

  &:hover .tree-node-icon {
    display: inline-block;
  }
}

.expr-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

.word-placeholder {
  height: calc(100% - 32px);
}

.md-editor {
  display: flex;
  gap: 12px;
  height: calc(100% - 32px);

  .code-mirror {
    //border: 1px solid rgb(228, 228, 228);
    //flex-shrink: 0;
    height: 100%;

    :deep(.cm-editor) {
      height: 100%;
      font-size: 14px;
      line-height: 150%;
      font-family: sans-serif, monospace;
    }
  }

  .md-preview {
    flex: 1;
    border: 1px solid #eee;
    border-radius: 4px;
    overflow: auto;
    height: 100%;

    .md-preview-title {
      padding: 8px 10px;
      border-bottom: 1px solid #eee;
      font-weight: 600;
    }

    .md-preview-body {
      padding: 10px;
    }
  }
}

.preview-body {
  height: 620px;
}
</style>
