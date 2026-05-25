import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  // Public
  {
    path: '/',
    component: () => import('@/views/item/ItemListView.vue'),
  },
  {
    path: '/login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/signup',
    component: () => import('@/views/auth/SignupView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/email-verify',
    component: () => import('@/views/auth/EmailVerifyView.vue'),
  },
  {
    path: '/password-reset',
    component: () => import('@/views/auth/PasswordResetView.vue'),
    meta: { guestOnly: true },
  },

  // Items
  {
    path: '/items/new',
    component: () => import('@/views/item/ItemFormView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/items/:itemId',
    component: () => import('@/views/item/ItemDetailView.vue'),
  },
  {
    path: '/items/:itemId/edit',
    component: () => import('@/views/item/ItemFormView.vue'),
    meta: { requiresAuth: true },
  },

  // User
  {
    path: '/users/:userId',
    component: () => import('@/views/user/ProfileView.vue'),
  },
  {
    path: '/profile/edit',
    component: () => import('@/views/user/ProfileEditView.vue'),
    meta: { requiresAuth: true },
  },

  // My
  {
    path: '/wishlist',
    component: () => import('@/views/item/WishlistView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/notifications',
    component: () => import('@/views/notification/NotificationView.vue'),
    meta: { requiresAuth: true },
  },

  // Trade
  {
    path: '/trades',
    component: () => import('@/views/trade/TradeListView.vue'),
    meta: { requiresAuth: true },
  },

  // Reservation
  {
    path: '/slots/manage',
    component: () => import('@/views/reservation/SlotManageView.vue'),
    meta: { requiresAuth: true },
  },

  // Chat
  {
    path: '/chat',
    component: () => import('@/views/chat/ChatListView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/chat/:roomId',
    component: () => import('@/views/chat/ChatRoomView.vue'),
    meta: { requiresAuth: true },
  },

  // Admin
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminDashboardView.vue'),
    meta: { requiresAuth: true, adminOnly: true },
  },
  {
    path: '/admin/users',
    component: () => import('@/views/admin/AdminUsersView.vue'),
    meta: { requiresAuth: true, adminOnly: true },
  },
  {
    path: '/admin/reports',
    component: () => import('@/views/admin/AdminReportsView.vue'),
    meta: { requiresAuth: true, adminOnly: true },
  },

  // 404
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  },
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    next('/')
    return
  }

  if (to.meta.adminOnly && !authStore.isAdmin) {
    next('/')
    return
  }

  next()
})

export default router
