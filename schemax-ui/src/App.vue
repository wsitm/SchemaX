<template>
  <div id="app">
    <navbar/>
    <transition name="fade-transform" mode="out-in">
      <keep-alive :include="cachedViews">
        <router-view :key="key"/>
      </keep-alive>
    </transition>
    <theme-picker/>
  </div>
</template>

<script>
import ThemePicker from "@/components/ThemePicker";
import {Navbar} from "@/layout";
import {constantRoutes} from "@/router";

export default {
  name: "App",
  components: {Navbar, ThemePicker},
  computed: {
    key() {
      return this.$route.path
    },
    cachedViews() {
      return constantRoutes.filter(r => r.meta?.keepAlive).map(r => r.name);
    }
  },
  // metaInfo() {
  //   return {
  //     title: this.$store.state.settings.dynamicTitle && this.$store.state.settings.title,
  //     titleTemplate: title => {
  //       return title ? `${title} - ${process.env.VUE_APP_TITLE}` : process.env.VUE_APP_TITLE
  //     }
  //   }
  // }
};
</script>
<style scoped>
#app .theme-picker {
  display: none;
}
</style>
