<template>
  <div id="app">
    <navbar/>
    <router-view v-slot="{ Component,route  }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :key="route.path" :is="Component"/>
        </keep-alive>
      </transition>
    </router-view>
    <!--    <theme-picker/>-->
  </div>
</template>

<script setup>
import {computed} from 'vue'
// import ThemePicker from "@/components/ThemePicker";
import {Navbar} from "@/layout";
import {constantRoutes} from "@/router";

// const route = useRoute();

// const key = computed(() => {
//   return route.path
// })

const cachedViews = computed(() => {
  return constantRoutes.filter(r => r.meta?.keepAlive).map(r => r.name);
})
</script>
<!--<style scoped>-->
<!--#app .theme-picker {-->
<!--  display: none;-->
<!--}-->
<!--</style>-->
