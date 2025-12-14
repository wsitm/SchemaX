<template>
  <div class="page-404">
    <div class="container">
      <div class="number">404</div>
      <div class="text">
        <h1>页面未找到</h1>
        <p>抱歉，您访问的页面不存在或已被删除</p>
        <router-link to="/" class="back-home">返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Page404'
}
</script>

<style lang="scss" scoped>

.page-404 {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f8f9fa;

  .container {
    text-align: center;
    max-width: 600px;
    padding: 20px;

    .number {
      font-size: 120px;
      font-weight: 700;
      color: #409eff;
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
        border-top: 5px solid #409eff;
        border-left: 5px solid #409eff;
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

      .back-home {
        display: inline-block;
        padding: 10px 24px;
        background-color: #409eff;
        color: white;
        text-decoration: none;
        border-radius: 0;
        font-size: 16px;
        transition: background-color 0.3s;

        &:hover {
          background-color: #66b1ff;
        }
      }
    }
  }
}
</style>
