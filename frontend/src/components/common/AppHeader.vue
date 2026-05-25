<template>
  <header class="sticky top-0 z-40 bg-white border-b border-gray-100 shadow-sm">
    <div class="max-w-screen-lg mx-auto px-4 h-14 flex items-center justify-between">
      <!-- Logo -->
      <RouterLink to="/" class="text-xl font-bold text-primary-600">Tradev</RouterLink>

      <!-- Search (desktop) -->
      <div class="hidden md:flex flex-1 mx-8">
        <div class="relative w-full max-w-md">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="상품 검색"
            class="w-full border border-gray-200 rounded-full px-4 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            @keyup.enter="onSearch"
          />
          <button class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" @click="onSearch">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex items-center gap-1">
        <template v-if="authStore.isLoggedIn">
          <!-- Notification dropdown -->
          <NotificationDropdown />

          <!-- Sell button -->
          <RouterLink
            to="/items/new"
            class="hidden md:inline-flex items-center gap-1 bg-primary-600 hover:bg-primary-700 text-white text-sm font-medium px-3 py-1.5 rounded-lg transition-colors"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            팔기
          </RouterLink>

          <!-- Profile -->
          <RouterLink :to="`/users/${authStore.user?.id}`" class="p-1">
            <img
              v-if="authStore.user?.profileImageUrl"
              :src="authStore.user.profileImageUrl"
              class="w-7 h-7 rounded-full object-cover"
            />
            <div v-else class="w-7 h-7 rounded-full bg-primary-100 flex items-center justify-center">
              <span class="text-xs font-medium text-primary-700">{{ authStore.user?.nickname?.[0] }}</span>
            </div>
          </RouterLink>
        </template>

        <template v-else>
          <RouterLink to="/login" class="text-sm text-gray-700 hover:text-primary-600 px-3 py-1.5">로그인</RouterLink>
          <RouterLink to="/signup" class="text-sm bg-primary-600 hover:bg-primary-700 text-white px-3 py-1.5 rounded-lg transition-colors">회원가입</RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NotificationDropdown from '@/components/common/NotificationDropdown.vue'

const router = useRouter()
const authStore = useAuthStore()
const searchQuery = ref('')

function onSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/', query: { keyword: searchQuery.value.trim() } })
  }
}
</script>
