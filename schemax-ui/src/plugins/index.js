import cache from './cache'
import modal from './modal'
import download from './download'

// 禁用react devtools，因为 univerjs 间接引用了 react-devtools-core，导致开发时阻塞卡顿
if (import.meta.env.VITE_APP_ENV === 'development') {
  window.__REACT_DEVTOOLS_GLOBAL_HOOK__ = {isDisabled: true};
}

export default function installPlugins(app) {
  // 缓存对象
  app.config.globalProperties.$cache = cache
  // 模态框对象
  app.config.globalProperties.$modal = modal
  // 下载文件
  // app.config.globalProperties.$download = download
}

