import { createApp } from 'vue'
import Cookies from 'js-cookie'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import locale from 'element-plus/es/locale/lang/zh-cn'

import '@/assets/styles/index.scss' // global css
import '@/assets/styles/rdbms.scss'
import App from './App.vue'
import store from './store'
import router from './router'
import directive from './directive' // directive
// 注册指令
import plugins from './plugins' // plugins
import {download} from '@/utils/request'

// svg图标
import 'virtual:svg-icons-register'
import SvgIcon from '@/components/SvgIcon'
import elementIcons from '@/components/SvgIcon/svgicon'

import './permission' // permission control
import {addDateRange, handleTree, resetForm} from "@/utils/rdbms";

// 分页组件
import Pagination from "@/components/Pagination";
// 自定义表格工具组件
import RightToolbar from "@/components/RightToolbar"
// 文件上传组件
import FileUpload from "@/components/FileUpload"
// 图片上传组件
// import ImageUpload from "@/components/ImageUpload"
// 图片预览组件
// import ImagePreview from "@/components/ImagePreview"


const app = createApp(App)

// 挂载全局方法
app.config.globalProperties.$resetForm = resetForm
app.config.globalProperties.$addDateRange = addDateRange
app.config.globalProperties.$download = download
app.config.globalProperties.$handleTree = handleTree

// 全局组件挂载
app.component('Pagination', Pagination)
app.component('RightToolbar', RightToolbar)
app.component('FileUpload', FileUpload)
// app.component('ImageUpload', ImageUpload)
// app.component('ImagePreview', ImagePreview)

app.use(router)
app.use(store)
app.use(plugins)
app.use(elementIcons)
app.component('svg-icon', SvgIcon)

directive(app)

app.use(ElementPlus, {
  locale: locale,
  size: Cookies.get('size') || 'small' // set element-plus default size
})

app.mount('#app')

export default app;
