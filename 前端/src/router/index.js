// src/router/index.js
import Vue from 'vue';
import Router from 'vue-router';
import GameView from '@/components/GameView.vue';
import LoginComponent from '@/components/LoginComponent.vue';
import RegisterComponent from '@/components/RegisterComponent.vue';

Vue.use(Router);

export default new Router({
  mode: 'history', // 可选：如果你想去掉 hash（#），可以保留这一行；不需要的话可以删掉
  routes: [
    {
      path: '/',         // 根路径
      redirect: '/login' // 自动重定向到登录页面
    },
    {
      path: '/login',
      name: 'Login',
      component: LoginComponent
    },
    {
      path: '/register',
      name: 'Register',
      component: RegisterComponent
    },
    {
      path: '/game',
      name: 'GameView',
      component: GameView
    },
    // 如果把真正的游戏页面放在 /game 路径下，也可以这样：
    // { path: '/game', component: GameView, name: 'GameView' }
    // 并且把“登录成功”之后 this.$router.push('/game')
    // 而不是 push('/')，以避免循环重定向。
  ]
});
