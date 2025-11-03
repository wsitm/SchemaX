<template>
  <div>
    <svg-icon icon-class="gitee" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'Git',
  data() {
    return {
      url: 'https://gitee.com/wsitm/wsitm-RDBMS.git'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
