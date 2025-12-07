<template>
  <div class="navbar">
    <logo/>
    <!--    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container"-->
    <!--               @toggleClick="toggleSideBar"/>-->

    <!--    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" v-if="!topNav"/>-->
    <top-nav id="topmenu-container" class="topmenu-container"/>


    <div style="position: absolute; top: 12px; right: 40px;">
      <el-tooltip content="Gitee地址" effect="dark" placement="bottom">
        <gitee class="right-menu-item hover-effect"/>
      </el-tooltip>
    </div>

    <div style="position: absolute; top: 12px; right: 14px;">
      <el-tooltip content="Github地址" effect="dark" placement="bottom">
        <github class="right-menu-item hover-effect"/>
      </el-tooltip>
    </div>

    <!--    <div class="right-menu">-->
    <!--      <template v-if="device!=='mobile'">-->
    <!--        <el-tooltip content="全屏" effect="dark" placement="bottom">-->
    <!--          <screenfull id="screenfull" class="right-menu-item hover-effect"/>-->
    <!--        </el-tooltip>-->
    <!--        <el-tooltip content="布局大小" effect="dark" placement="bottom">-->
    <!--          <size-select id="size-select" class="right-menu-item hover-effect"/>-->
    <!--        </el-tooltip>-->
    <!--      </template>-->
    <!--    </div>-->
  </div>
</template>

<script>
import Logo from './Logo.vue'
import TopNav from '@/components/TopNav'
// import Screenfull from '@/components/Screenfull'
// import SizeSelect from '@/components/SizeSelect'
import Gitee from "@/components/Goto/Gitee.vue";
import Github from "@/components/Goto/Github.vue";


export default {
  components: {
    Logo,
    Gitee,
    Github,
    // Breadcrumb,
    TopNav,
    // Hamburger,
    // Screenfull,
    // SizeSelect,
  },
  computed: {
    // ...mapGetters([
    // 'sidebar',
    // 'avatar',
    // 'device'
    // ]),
    // setting: {
    //   get() {
    //     return this.$store.state.settings.showSettings
    //   },
    //   set(val) {
    //     this.$store.dispatch('settings/changeSetting', {
    //       key: 'showSettings',
    //       value: val
    //     })
    //   }
    // },
    // topNav: {
    //   get() {
    //     return this.$store.state.settings.topNav
    //   }
    // }
  },
  mounted() {
    // console.log(this.setting)
  },
  methods: {
    // toggleSideBar() {
    //   this.$store.dispatch('app/toggleSideBar')
    // },
    // async logout() {
    //   this.$confirm('确定注销并退出系统吗？', '提示', {
    //     confirmButtonText: '确定',
    //     cancelButtonText: '取消',
    //     type: 'warning'
    //   }).then(() => {
    //     this.$store.dispatch('LogOut').then(() => {
    //       location.href = '/index';
    //     })
    //   }).catch(() => {
    //   });
    // }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 40px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, .08);
  //text-align: center;

  //.hamburger-container {
  //  line-height: 46px;
  //  height: 100%;
  //  float: left;
  //  cursor: pointer;
  //  transition: background .3s;
  //  -webkit-tap-highlight-color: transparent;
  //
  //  &:hover {
  //    background: rgba(0, 0, 0, .025)
  //  }
  //}

  //.breadcrumb-container {
  //  float: left;
  //}

  .topmenu-container {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
  }

  //.errLog-container {
  //  display: inline-block;
  //  vertical-align: top;
  //}

  //.right-menu {
  //  float: right;
  //  height: 100%;
  //  line-height: 40px;
  //  margin-right: 15px;
  //
  //  &:focus {
  //    outline: none;
  //  }
  //
  //  .right-menu-item {
  //    display: inline-block;
  //    padding: 0 8px;
  //    height: 100%;
  //    font-size: 18px;
  //    color: #5a5e66;
  //    vertical-align: text-bottom;
  //
  //    &.hover-effect {
  //      cursor: pointer;
  //      transition: background .3s;
  //
  //      &:hover {
  //        background: rgba(0, 0, 0, .025)
  //      }
  //    }
  //  }
  //
  //.avatar-container {
  //  margin-right: 30px;
  //
  //  .avatar-wrapper {
  //    margin-top: 5px;
  //    position: relative;
  //
  //    .user-avatar {
  //      cursor: pointer;
  //      width: 40px;
  //      height: 40px;
  //      border-radius: 10px;
  //    }
  //
  //    .el-icon-caret-bottom {
  //      cursor: pointer;
  //      position: absolute;
  //      right: -20px;
  //      top: 25px;
  //      font-size: 12px;
  //    }
  //  }
  //}
  //}
}
</style>
