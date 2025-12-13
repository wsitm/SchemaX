<template>
  <div class="top-right-btn" :style="style">
    <el-row>
      <el-tooltip class="item" effect="dark" :content="showSearch ? '隐藏搜索' : '显示搜索'" placement="top"
                  v-if="search">
        <el-button size="small" circle icon="el-icon-search" @click="toggleSearch()"/>
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="刷新" placement="top">
        <el-button size="small" circle icon="el-icon-refresh" @click="refresh()"/>
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="显隐列" placement="top" v-if="columns">
        <el-button size="small" circle icon="el-icon-menu" @click="showColumn()" v-if="showColumnsType == 'transfer'"/>
        <el-dropdown trigger="click" :hide-on-click="false" style="padding-left: 12px"
                     v-if="showColumnsType == 'checkbox'">
          <el-button size="small" circle icon="el-icon-menu"/>
          <template #dropdown>
            <el-dropdown-menu>
              <template v-for="item in columns" :key="item.key">
                <el-dropdown-item>
                  <el-checkbox :checked="item.visible" @change="checkboxChange($event, item.label)"
                               :label="item.label"/>
                </el-dropdown-item>
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-tooltip>
    </el-row>
    <el-dialog :title="title" v-model="open" append-to-body>
      <el-transfer
        :titles="['显示', '隐藏']"
        v-model="value"
        :data="columns"
        @change="dataChange"
      ></el-transfer>
    </el-dialog>
  </div>
</template>
<script>
import { ref, reactive, computed, onMounted } from 'vue'

export default {
  name: "RightToolbar",
  props: {
    /* 是否显示检索条件 */
    showSearch: {
      type: Boolean,
      default: true,
    },
    /* 显隐列信息 */
    columns: {
      type: Array,
    },
    /* 是否显示检索图标 */
    search: {
      type: Boolean,
      default: true,
    },
    /* 显隐列类型（transfer穿梭框、checkbox复选框） */
    showColumnsType: {
      type: String,
      default: "checkbox",
    },
    /* 右外边距 */
    gutter: {
      type: Number,
      default: 10,
    },
  },
  emits: ['update:showSearch', 'queryTable'],
  setup(props, { emit }) {
    // 显隐数据
    const value = ref([])
    // 弹出层标题
    const title = ref("显示/隐藏")
    // 是否显示弹出层
    const open = ref(false)
    
    const style = computed(() => {
      const ret = {}
      if (props.gutter) {
        ret.marginRight = `${props.gutter / 2}px`
      }
      return ret
    })
    
    // 显隐列初始默认隐藏列
    onMounted(() => {
      if (props.showColumnsType == 'transfer' && props.columns) {
        // 显隐列初始默认隐藏列
        props.columns.forEach((item, index) => {
          if (item.visible === false) {
            value.value.push(index)
          }
        })
      }
    })
    
    // 搜索
    const toggleSearch = () => {
      emit("update:showSearch", !props.showSearch)
    }
    
    // 刷新
    const refresh = () => {
      emit("queryTable")
    }
    
    // 右侧列表元素变化
    const dataChange = (data) => {
      if (props.columns) {
        props.columns.forEach((item, index) => {
          const key = item.key
          item.visible = !data.includes(key)
        })
      }
    }
    
    // 打开显隐列dialog
    const showColumn = () => {
      open.value = true
    }
    
    // 勾选
    const checkboxChange = (event, label) => {
      if (props.columns) {
        const item = props.columns.find(item => item.label === label)
        if (item) {
          item.visible = event
        }
      }
    }
    
    return {
      value,
      title,
      open,
      style,
      toggleSearch,
      refresh,
      dataChange,
      showColumn,
      checkboxChange
    }
  }
}
</script>
<style lang="scss" scoped>
:deep(.el-transfer__button) {
  border-radius: 50%;
  padding: 12px;
  display: block;
  margin-left: 0px;
}

:deep(.el-transfer__button:first-child) {
  margin-bottom: 10px;
}
</style>
