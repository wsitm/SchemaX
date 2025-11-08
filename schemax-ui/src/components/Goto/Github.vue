<template>
  <div>
    <svg-icon icon-class="github" @click="goto" style="cursor: pointer;"/>
  </div>
</template>

<script>
export default {
  name: 'Git',
  data() {
    return {
      url: 'https://github.com/wsitm/SchemaX.git'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
