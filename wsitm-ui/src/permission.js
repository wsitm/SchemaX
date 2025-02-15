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
})
