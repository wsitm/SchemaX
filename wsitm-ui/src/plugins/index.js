import cache from './cache'
import modal from './modal'
import download from './download'

// 禁用react devtools，因为 univerjs 间接引用了 react-devtools-core，导致开发时阻塞卡顿
if (process.env.NODE_ENV === 'development') {
  window.__REACT_DEVTOOLS_GLOBAL_HOOK__ = { isDisabled: true };
}

export default {
  install(Vue) {
    // 缓存对象
    Vue.prototype.$cache = cache
    // 模态框对象
    Vue.prototype.$modal = modal
    // 下载文件
    Vue.prototype.$download = download
  }
}
