<template>
  <div class="max-w-screen-md mx-auto px-4 py-4 pb-24 md:pb-8">
    <!-- Skeleton -->
    <div v-if="loading" class="space-y-4">
      <div class="skeleton aspect-square rounded-xl" />
      <div class="skeleton h-6 w-3/4" />
      <div class="skeleton h-8 w-1/3" />
    </div>

    <template v-else-if="item">
      <!-- Image gallery -->
      <div class="relative aspect-square bg-gray-100 rounded-xl overflow-hidden mb-4">
        <img
          v-if="currentImage"
          :src="currentImage"
          :alt="item.title"
          class="w-full h-full object-cover"
        />
        <div class="absolute bottom-3 right-3 bg-black/50 text-white text-xs px-2 py-1 rounded-full">
          {{ currentImageIndex + 1 }} / {{ item.images.length }}
        </div>
        <button
          v-if="currentImageIndex > 0"
          class="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 bg-white/80 rounded-full flex items-center justify-center"
          @click="currentImageIndex--"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <button
          v-if="currentImageIndex < item.images.length - 1"
          class="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 bg-white/80 rounded-full flex items-center justify-center"
          @click="currentImageIndex++"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>

      <!-- Thumbnail strip -->
      <div class="flex gap-2 mb-4 overflow-x-auto pb-1">
        <button
          v-for="(img, idx) in item.images"
          :key="img.id"
          :class="['w-14 h-14 rounded-lg overflow-hidden flex-shrink-0 border-2 transition-colors', idx === currentImageIndex ? 'border-primary-500' : 'border-transparent']"
          @click="currentImageIndex = idx"
        >
          <img :src="img.imageUrl" class="w-full h-full object-cover" />
        </button>
      </div>

      <!-- Seller info -->
      <RouterLink :to="`/users/${item.seller.id}`" class="flex items-center gap-3 bg-white rounded-xl border border-gray-100 p-4 mb-4 hover:shadow-sm transition-shadow">
        <div class="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center overflow-hidden">
          <img v-if="item.seller.profileImageUrl" :src="item.seller.profileImageUrl" class="w-full h-full object-cover" />
          <span v-else class="text-primary-700 font-medium text-sm">{{ item.seller.nickname[0] }}</span>
        </div>
        <div class="flex-1">
          <p class="text-sm font-medium text-gray-900">{{ item.seller.nickname }}</p>
          <p class="text-xs text-gray-400">{{ item.seller.trustGradeIcon }} {{ item.seller.trustGrade }}</p>
        </div>
        <svg class="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </RouterLink>

      <!-- Item info -->
      <div class="bg-white rounded-xl border border-gray-100 p-4 mb-4">
        <div class="flex items-start justify-between gap-2 mb-2">
          <h1 class="text-lg font-bold text-gray-900">{{ item.title }}</h1>
          <TradeStatusBadge :status="item.status" />
        </div>
        <p class="text-2xl font-bold text-gray-900 mb-3">{{ formatPrice(item.price) }}</p>
        <div class="flex gap-3 text-xs text-gray-400 mb-4">
          <span>{{ item.category.name }}</span>
          <span>{{ item.location }}</span>
          <span>조회 {{ item.viewCount }}</span>
          <span>{{ formatDate(item.createdAt) }}</span>
        </div>
        <p class="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">{{ item.description }}</p>
      </div>

      <!-- Bottom action bar -->
      <div class="fixed bottom-0 left-0 right-0 md:sticky md:bottom-auto bg-white border-t border-gray-100 px-4 py-3 flex items-center gap-3 z-30">
        <!-- Wishlist -->
        <button
          v-if="authStore.isLoggedIn"
          class="flex flex-col items-center gap-0.5 text-xs"
          :class="wished ? 'text-red-500' : 'text-gray-400'"
          @click="toggleWish"
        >
          <svg class="w-6 h-6" :fill="wished ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          {{ item.wishCount }}
        </button>

        <!-- 신고 버튼 -->
        <button
          v-if="authStore.isLoggedIn && !isOwner"
          @click="showReport = true"
          class="flex flex-col items-center gap-0.5 text-xs text-gray-400 hover:text-red-400"
          title="신고하기"
        >
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 21v-4m0 0V5a2 2 0 012-2h6.5l1 2H21l-3 6 3 6H13l-1-2H5a2 2 0 00-2 2z" />
          </svg>
          신고
        </button>

        <div class="flex-1">
          <BaseButton
            v-if="authStore.isLoggedIn && !isOwner"
            :disabled="item.status !== 'SALE'"
            class="w-full"
            @click="startChat"
          >
            채팅하기
          </BaseButton>
          <div v-else-if="isOwner" class="flex gap-2">
            <RouterLink :to="`/items/${item.id}/edit`" class="flex-1">
              <BaseButton variant="outline" class="w-full">수정</BaseButton>
            </RouterLink>
            <BaseButton variant="danger" class="flex-1" @click="deleteItem">삭제</BaseButton>
          </div>
          <RouterLink v-else to="/login">
            <BaseButton class="w-full">로그인하고 거래하기</BaseButton>
          </RouterLink>
        </div>
      </div>
    </template>

    <div v-else class="text-center py-20">
      <p class="text-gray-400 text-sm">상품을 찾을 수 없습니다.</p>
    </div>
  </div>

  <!-- 신고 모달 -->
  <ReportModal
    v-if="showReport && item"
    target-type="ITEM"
    :target-id="item.id"
    @close="showReport = false"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { itemApi } from '@/api/item'
import { chatApi } from '@/api/chat'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { useApiError } from '@/composables/useApiError'
import BaseButton from '@/components/common/BaseButton.vue'
import TradeStatusBadge from '@/components/common/TradeStatusBadge.vue'
import ReportModal from '@/components/report/ReportModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { success, error } = useToast()
const { handle } = useApiError()

const item = ref(null)
const loading = ref(true)
const currentImageIndex = ref(0)
const wished = ref(false)
const showReport = ref(false)

const currentImage = computed(() => item.value?.images[currentImageIndex.value]?.imageUrl)
const isOwner = computed(() => authStore.user?.id === item.value?.seller?.id)

onMounted(async () => {
  try {
    const { data } = await itemApi.getItem(route.params.itemId)
    item.value = data.data
    wished.value = item.value.wishedByMe
  } catch (err) {
    handle(err)
  } finally {
    loading.value = false
  }
})

async function toggleWish() {
  const prev = wished.value
  wished.value = !prev
  item.value.wishCount += wished.value ? 1 : -1
  try {
    await itemApi.toggleWishlist(item.value.id)
  } catch {
    wished.value = prev
    item.value.wishCount += prev ? 1 : -1
  }
}

async function deleteItem() {
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await itemApi.deleteItem(item.value.id)
    success('상품이 삭제되었습니다.')
    router.push('/')
  } catch (err) {
    handle(err)
  }
}

async function startChat() {
  if (!authStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const { data } = await chatApi.getOrCreateRoom(item.value.id)
    router.push(`/chat/${data.data.id}`)
  } catch (err) {
    handle(err)
  }
}

function formatPrice(price) {
  if (price === 0) return '무료나눔'
  return price.toLocaleString('ko-KR') + '원'
}

function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '방금'
  if (diff < 3600) return Math.floor(diff / 60) + '분 전'
  if (diff < 86400) return Math.floor(diff / 3600) + '시간 전'
  return Math.floor(diff / 86400) + '일 전'
}
</script>
