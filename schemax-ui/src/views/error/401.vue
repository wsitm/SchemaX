<template>
  <div class="page-401">
    <div class="container">
      <div class="number">401</div>
      <div class="text">
        <h1>访问被拒绝</h1>
        <p>抱歉，您没有访问此页面的权限</p>
        <div class="actions">
          <el-button icon="arrow-left" size="large" @click="back" class="back-btn">
            返回上页
          </el-button>
          <router-link to="/" class="back-home">
            回首页
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

export default {
  name: 'Page401',
  methods: {
    back() {
      if (this.$route.query.noGoBack) {
        this.$router.push({path: '/'})
      } else {
        this.$router.go(-1)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-401 {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f8f9fa;

  .container {
    text-align: center;
    max-width: 600px;
    padding: 20px;

    .number {
      font-size: 120px;
      font-weight: 700;
      color: #e6a23c;
      line-height: 1;
      margin-bottom: 20px;
      position: relative;
      display: inline-block;

      &::before,
      &::after {
        content: "";
        position: absolute;
        width: 60px;
        height: 60px;
        border-top: 5px solid #e6a23c;
        border-left: 5px solid #e6a23c;
      }

      &::before {
        top: 50px;
        left: -20px;
        transform: translate(-30%, -30%) rotate(-45deg);
      }

      &::after {
        top: 14px;
        right: -20px;
        transform: translate(30%, 30%) rotate(135deg);
      }
    }

    .text {
      h1 {
        font-size: 32px;
        color: #303133;
        margin-bottom: 15px;
      }

      p {
        font-size: 16px;
        color: #606266;
        margin-bottom: 30px;
        line-height: 1.6;
      }

      .actions {
        display: flex;
        justify-content: center;
        gap: 20px;

        .back-btn {
          background-color: #e6a23c;
          border: none;
          color: white;
          border-radius: 0;

          &:hover {
            background-color: #ebb563;
          }
        }

        .back-home {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          padding: 10px 24px;
          background-color: var(--el-color-primary);
          color: white;
          text-decoration: none;
          border-radius: 0;
          font-size: 14px;
          transition: filter 0.3s;

          &:hover {
            filter: brightness(0.94);
          }
        }
      }
    }
  }
}
</style>
