import router from './router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({showSpinner: false})


router.beforeEach((to, from, next) => {
  NProgress.start();
  // console.log(from);
  // console.log(to);
  next();
})

router.afterEach(() => {
  NProgress.done();

  let list = document.getElementsByClassName("el-tooltip__popper");
  if (list && list.length) {
    list[list.length - 1].style.display = "none";
  }
})
