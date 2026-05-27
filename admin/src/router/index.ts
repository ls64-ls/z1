import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/pages/dashboard/index.vue'),
      meta: { title: '数据大盘' }
    },
    {
      path: '/rooms',
      name: 'RoomManage',
      component: () => import('@/pages/room-manage/index.vue'),
      meta: { title: '会议室管理' }
    },
    {
      path: '/bookings',
      name: 'BookingManage',
      component: () => import('@/pages/booking-manage/index.vue'),
      meta: { title: '预订管理' }
    },
    {
      path: '/schedule',
      name: 'Schedule',
      component: () => import('@/pages/schedule/index.vue'),
      meta: { title: '排期管理' }
    },
    {
      path: '/reports',
      name: 'Reports',
      component: () => import('@/pages/reports/index.vue'),
      meta: { title: '数据报表' }
    }
  ]
})

export default router
