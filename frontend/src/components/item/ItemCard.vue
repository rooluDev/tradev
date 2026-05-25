<template>
  <RouterLink :to="`/items/${item.id}`" class="block">
    <div class="bg-white rounded-xl overflow-hidden hover:shadow-md transition-shadow duration-150">
      <!-- Thumbnail -->
      <div class="relative aspect-square bg-gray-100 overflow-hidden">
        <img
          v-if="item.thumbnailUrl"
          :src="item.thumbnailUrl"
          :alt="item.title"
          class="w-full h-full object-cover"
          loading="lazy"
        />
        <div v-else class="w-full h-full flex items-center justify-center text-gray-300">
          <svg class="w-12 h-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </div>

        <!-- Status overlay -->
        <div
          v-if="item.status !== 'SALE'"
          class="absolute inset-0 bg-black/50 flex items-center justify-center"
        >
          <span class="text-white text-sm font-medium">
            {{ item.status === 'RESERVED' ? '예약중' : '판매완료' }}
          </span>
        </div>

        <!-- Wishlist button -->
        <button
          v-if="showWishBtn && authStore.isLoggedIn"
          class="absolute top-2 right-2 p-1.5 bg-white/80 backdrop-blur-sm rounded-full hover:bg-white transition-colors"
          @click.prevent="toggleWish"
        >
          <svg
            class="w-4 h-4 transition-colors"
            :class="wished ? 'text-red-500 fill-red-500' : 'text-gray-400'"
            viewBox="0 0 24 24"
            stroke="currentColor"
            fill="none"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
        </button>
      </div>

      <!-- Info -->
      <div class="p-3">
        <p class="text-sm font-medium text-gray-900 line-clamp-2 leading-snug">{{ item.title }}</p>
        <p class="mt-1 text-base font-bold text-gray-900">{{ formatPrice(item.price) }}</p>
        <div class="mt-1 flex items-center gap-2 text-xs text-gray-400">
          <span>{{ item.location }}</span>
          <span v-if="item.wishCount > 0" class="flex items-center gap-0.5">
            <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd" />
            </svg>
            {{ item.wishCount }}
          </span>
        </div>
      </div>
    </div>
  </RouterLink>
</template>

<script setup>
import { ref } from 'vue'
import { itemApi } from '@/api/item'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  item: { type: Object, required: true },
  showWishBtn: { type: Boolean, default: true },
})

const authStore = useAuthStore()
const wished = ref(props.item.wishedByMe || false)

async function toggleWish() {
  const prev = wished.value
  wished.value = !wished.value
  try {
    await itemApi.toggleWishlist(props.item.id)
  } catch {
    wished.value = prev
  }
}

function formatPrice(price) {
  if (price === 0) return '무료나눔'
  return price.toLocaleString('ko-KR') + '원'
}
</script>
