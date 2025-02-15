import Vue from 'vue'
import Vuex from 'vuex'
import app from './modules/app'
// import tagsView from './modules/tagsView'
import settings from './modules/settings'
// import permission from './modules/permission'
import getters from './getters'

Vue.use(Vuex)

const store = new Vuex.Store({
  modules: {
    app,
    // tagsView,
    settings,
    // permission,
  },
  getters
})

export default store
