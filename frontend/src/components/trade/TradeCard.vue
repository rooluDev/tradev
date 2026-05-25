<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { tradeApi } from '@/api/trade'
import { useApiError } from '@/composables/useApiError'
import { useToast } from '@/composables/useToast'
import ReviewForm from '@/components/review/ReviewForm.vue'

const props = defineProps({
  trade: { type: Object, required: true },
})
const emit = defineEmits(['updated'])

const showReviewForm = ref(false)

const authStore = useAuthStore()
const { handle } = useApiError()
const { showToast } = useToast()

const isSeller = computed(() => props.trade.seller.id === authStore.user?.id)
const isBuyer = computed(() => props.trade.buyer.id === authStore.user?.id)

const statusLabel = {
  PENDING: '요청 중',
  RESERVED: '진행 중',
  COMPLETED: '거래 완료',
  CANCELLED: '취소됨',
  REJECTED: '거절됨',
}

const statusColor = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  RESERVED: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-gray-100 text-gray-500',
  REJECTED: 'bg-red-100 text-red-600',
}

async function accept() {
  try {
    await tradeApi.accept(props.trade.id)
    showToast('거래 요청을 수락했습니다.')
    emit('updated')
  } catch (e) { handle(e) }
}

async function reject() {
  try {
    await tradeApi.reject(props.trade.id)
    showToast('거래 요청을 거절했습니다.')
    emit('updated')
  } catch (e) { handle(e) }
}

async function confirm() {
  try {
    await tradeApi.confirm(props.trade.id)
    showToast('거래 완료를 확인했습니다.')
    emit('updated')
  } catch (e) { handle(e) }
}

async function cancel() {
  if (!window.confirm('거래를 취소하시겠습니까?')) return
  try {
    await tradeApi.cancel(props.trade.id)
    showToast('거래가 취소되었습니다.')
    emit('updated')
  } catch (e) { handle(e) }
}
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-200 p-4 hover:shadow-sm transition-shadow">
    <!-- 상품 정보 -->
    <div class="flex gap-3">
      <img
        :src="trade.item.thumbnailUrl || '/placeholder.png'"
        :alt="trade.item.title"
        class="w-16 h-16 rounded-lg object-cover flex-shrink-0 bg-gray-100"
      />
      <div class="flex-1 min-w-0">
        <div class="flex items-start justify-between gap-2">
          <p class="font-medium text-gray-900 truncate">{{ trade.item.title }}</p>
          <span :class="['px-2 py-0.5 rounded-full text-xs font-medium', statusColor[trade.status]]">
            {{ statusLabel[trade.status] }}
          </span>
        </div>
        <p class="text-sm text-orange-500 font-semibold mt-1">
          {{ trade.price.toLocaleString() }}원
        </p>
        <p class="text-xs text-gray-400 mt-1">
          {{ isSeller ? '구매자' : '판매자' }}:
          {{ isSeller ? trade.buyer.nickname : trade.seller.nickname }}
        </p>
      </div>
    </div>

    <!-- 액션 버튼 -->
    <div v-if="trade.status === 'PENDING' && isSeller" class="flex gap-2 mt-3">
      <button
        @click="accept"
        class="flex-1 py-2 text-sm font-medium bg-orange-500 text-white rounded-lg hover:bg-orange-600"
      >
        수락
      </button>
      <button
        @click="reject"
        class="flex-1 py-2 text-sm font-medium border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50"
      >
        거절
      </button>
    </div>

    <div v-else-if="trade.status === 'RESERVED'" class="flex gap-2 mt-3">
      <button
        @click="confirm"
        class="flex-1 py-2 text-sm font-medium bg-blue-500 text-white rounded-lg hover:bg-blue-600"
      >
        거래 완료 확인
      </button>
      <button
        @click="cancel"
        class="py-2 px-4 text-sm font-medium border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50"
      >
        취소
      </button>
    </div>

    <div v-else-if="trade.status === 'PENDING' && isBuyer" class="mt-3">
      <button
        @click="cancel"
        class="w-full py-2 text-sm font-medium border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50"
      >
        요청 취소
      </button>
    </div>

    <!-- 거래 완료 — 리뷰 작성 -->
    <div v-else-if="trade.status === 'COMPLETED'" class="mt-3">
      <button
        @click="showReviewForm = true"
        class="w-full py-2 text-sm font-medium border border-orange-300 text-orange-600 rounded-lg hover:bg-orange-50"
      >
        ✍️ 후기 쓰기
      </button>
    </div>
  </div>

  <!-- 리뷰 폼 모달 -->
  <ReviewForm
    v-if="showReviewForm"
    :trade-id="trade.id"
    :target-nickname="isSeller ? trade.buyer.nickname : trade.seller.nickname"
    @submitted="showReviewForm = false; emit('updated')"
    @cancel="showReviewForm = false"
  />
</template>
