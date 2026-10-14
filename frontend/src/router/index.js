import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView
  },
  {
    path: '/mint',
    name: 'Mint',
    component: () => import('../views/MintView.vue')
  },
  {
    path: '/market',
    name: 'Market',
    component: () => import('../views/MarketView.vue')
  },
  {
    path: '/trace',
    name: 'Trace',
    component: () => import('../views/TraceView.vue')
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/AdminView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
