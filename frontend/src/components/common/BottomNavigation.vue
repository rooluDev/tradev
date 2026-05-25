<template>
  <nav class="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white border-t border-gray-100 safe-area-bottom">
    <div class="flex items-center justify-around h-14">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="flex flex-col items-center gap-0.5 px-4 py-2 text-xs text-gray-500"
        active-class="text-primary-600"
      >
        <component :is="item.icon" class="w-5 h-5" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </div>
  </nav>
</template>

<script setup>
import { defineComponent, h } from 'vue'

const HomeIcon = defineComponent({
  render: () => h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor' },
    [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' })]
  )
})

const HeartIcon = defineComponent({
  render: () => h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor' },
    [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z' })]
  )
})

const PlusCircleIcon = defineComponent({
  render: () => h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor' },
    [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M12 9v3m0 0v3m0-3h3m-3 0H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z' })]
  )
})

const ChatIcon = defineComponent({
  render: () => h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor' },
    [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z' })]
  )
})

const UserIcon = defineComponent({
  render: () => h('svg', { fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor' },
    [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' })]
  )
})

import { useAuthStore } from '@/stores/auth'
const authStore = useAuthStore()

const navItems = [
  { to: '/', label: '홈', icon: HomeIcon },
  { to: '/wishlist', label: '관심', icon: HeartIcon },
  { to: '/items/new', label: '팔기', icon: PlusCircleIcon },
  { to: '/chat', label: '채팅', icon: ChatIcon },
  { to: authStore.isLoggedIn ? `/users/${authStore.user?.id}` : '/login', label: '나', icon: UserIcon },
]
</script>
