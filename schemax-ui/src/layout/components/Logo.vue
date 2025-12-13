<template>
  <div class="sidebar-logo-container">
    <transition name="sidebarLogoFade">
      <!--      <router-link key="collapse" class="sidebar-logo-link" to="">-->
      <div key="collapse" class="sidebar-logo-link" @click="goto">
        <img v-if="logo" :src="logo" class="sidebar-logo"/>
        <h1 v-else class="sidebar-title">
          {{ title }} </h1>
      </div>
      <!--      </router-link>-->
    </transition>
  </div>
</template>

<script>
// import variables from '@/assets/styles/variables.module.scss'

export default {
  name: 'SidebarLogo',
  computed: {
    // variables() {
    //   return variables;
    // },
    sideTheme() {
      return this.$pinia.state.value.settings.sideTheme
    }
  },
  data() {
    return {
      title: import.meta.env.VITE_APP_TITLE,
      // logo: logoImg
      logo: null
    }
  },
  methods: {
    goto() {
      // window.open('https://gitee.com/wsitm/SchemaX.git');
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  float: left;
  width: auto;
  height: 40px;
  padding: 0 15px;
  line-height: 40px;
  //background: #2b2f3a;
  text-align: center;
  overflow: hidden;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;
    //cursor: pointer;

    & .sidebar-logo {
      width: 32px;
      height: 32px;
      vertical-align: middle;
      margin-right: 12px;
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: #000;
      font-weight: 600;
      line-height: 40px;
      font-size: 16px;
      font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
      vertical-align: middle;
    }
  }

  &.collapse {
    .sidebar-logo {
      margin-right: 0px;
    }
  }
}
</style>
