<template>
  <div class="max-w-screen-md mx-auto px-4 py-4">
    <div v-if="loading" class="space-y-4">
      <div class="skeleton h-24 rounded-xl" />
    </div>

    <template v-else-if="profile">
      <!-- Profile header -->
      <div class="bg-white rounded-xl border border-gray-100 p-6 mb-4">
        <div class="flex items-center gap-4">
          <div class="w-16 h-16 rounded-full overflow-hidden bg-primary-100 flex items-center justify-center">
            <img v-if="profile.profileImageUrl" :src="profile.profileImageUrl" class="w-full h-full object-cover" />
            <span v-else class="text-2xl font-bold text-primary-600">{{ profile.nickname[0] }}</span>
          </div>
          <div class="flex-1">
            <h1 class="text-lg font-bold text-gray-900">{{ profile.nickname }}</h1>
            <p class="text-sm text-gray-400">
              {{ profile.trustGradeIcon }} {{ profile.trustGradeLabel }}
              <span class="ml-1 text-xs">({{ profile.trustScore }}점)</span>
            </p>
          </div>
          <RouterLink v-if="isMe" to="/profile/edit" class="text-sm text-primary-600 font-medium">수정</RouterLink>
        </div>
        <p v-if="profile.bio" class="mt-3 text-sm text-gray-600">{{ profile.bio }}</p>
      </div>

      <!-- Items -->
      <h2 class="text-sm font-semibold text-gray-700 mb-3">판매 상품</h2>
      <div class="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-6">
        <ItemCard v-for="item in items" :key="item.id" :item="item" />
      </div>
      <div v-if="items.length === 0" class="text-center py-6">
        <p class="text-gray-400 text-sm">판매 중인 상품이 없습니다.</p>
      </div>

      <!-- 후기 섹션 -->
      <div class="mt-2">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-sm font-semibold text-gray-700">
            거래 후기
            <span v-if="reviewSummary" class="text-gray-400 font-normal ml-1">({{ reviewSummary.totalCount }})</span>
          </h2>
          <div v-if="reviewSummary && reviewSummary.totalCount > 0" class="flex items-center gap-1">
            <span class="text-yellow-400 text-sm">★</span>
            <span class="text-sm font-semibold text-gray-700">{{ reviewSummary.averageRating.toFixed(1) }}</span>
          </div>
        </div>

        <div v-if="reviews.length === 0" class="text-center py-10">
          <p class="text-gray-400 text-sm">아직 받은 후기가 없습니다.</p>
        </div>
        <div v-else class="space-y-3">
          <ReviewCard
            v-for="review in reviews"
            :key="review.id"
            :review="review"
            :is-owner="isMe"
            @replied="fetchReviews"
          />
        </div>
        <div v-if="reviewHasNext" class="mt-3 text-center">
          <button
            @click="fetchReviews(false)"
            class="text-sm text-orange-500 font-medium hover:text-orange-600"
          >더 보기</button>
        </div>
      </div>

      <!-- 신고 버튼 (타인 프로필) -->
      <div v-if="!isMe && authStore.isLoggedIn" class="mt-6 text-center">
        <button
          @click="showReport = true"
          class="text-xs text-gray-400 hover:text-red-400"
        >이 사용자 신고하기</button>
      </div>
    </template>
  </div>

  <ReportModal
    v-if="showReport && profile"
    target-type="USER"
    :target-id="profile.id"
    @close="showReport = false"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { userApi } from '@/api/user'
import { itemApi } from '@/api/item'
import { reviewApi } from '@/api/review'
import { useAuthStore } from '@/stores/auth'
import ItemCard from '@/components/item/ItemCard.vue'
import ReviewCard from '@/components/review/ReviewCard.vue'
import ReportModal from '@/components/report/ReportModal.vue'

const route = useRoute()
const authStore = useAuthStore()

const profile = ref(null)
const items = ref([])
const loading = ref(true)
const reviews = ref([])
const reviewCursor = ref(null)
const reviewHasNext = ref(false)
const reviewSummary = ref(null)
const showReport = ref(false)

const isMe = computed(() => authStore.user?.id === Number(route.params.userId))

async function fetchReviews(reset = true) {
  const params = { size: 5 }
  if (!reset && reviewCursor.value) params.cursor = reviewCursor.value
  try {
    const { data } = await reviewApi.getByUser(route.params.userId, params)
    if (reset) {
      reviews.value = data.data.items
      reviewSummary.value = { averageRating: data.data.averageRating, totalCount: data.data.totalCount }
    } else {
      reviews.value.push(...data.data.items)
    }
    reviewCursor.value = data.data.nextCursor
    reviewHasNext.value = data.data.hasNext
  } catch {
    // 리뷰 로딩 실패는 조용히 처리
  }
}

onMounted(async () => {
  try {
    const [profileRes, itemsRes] = await Promise.all([
      userApi.getProfile(route.params.userId),
      itemApi.getItems({ sellerId: route.params.userId, pageSize: 20 }),
    ])
    profile.value = profileRes.data.data
    items.value = itemsRes.data.data.items
    await fetchReviews(true)
  } finally {
    loading.value = false
  }
})
</script>
