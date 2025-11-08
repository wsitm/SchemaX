<template>
  <div>
    <svg-icon icon-class="gitee" @click="goto" style="cursor: pointer;"/>
  </div>
</template>

<script>
export default {
  name: 'Git',
  data() {
    return {
      url: 'https://gitee.com/wsitm/SchemaX.git'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
