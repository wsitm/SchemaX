import router from './router'

router.beforeEach((to, from, next) => {
  // console.log(from);
  // console.log(to);
  next();
})

router.afterEach(() => {

})
