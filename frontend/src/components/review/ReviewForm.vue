<script setup>
import { ref } from 'vue'
import { reviewApi } from '@/api/review'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  tradeId: { type: Number, required: true },
  targetNickname: { type: String, default: '상대방' },
})
const emit = defineEmits(['submitted', 'cancel'])

const { showToast } = useToast()
const rating = ref(0)
const hoverRating = ref(0)
const content = ref('')
const isSubmitting = ref(false)

function setRating(val) {
  rating.value = val
}

async function submit() {
  if (rating.value === 0) {
    showToast('별점을 선택해주세요.', 'warning')
    return
  }
  if (content.value.trim().length < 10) {
    showToast('리뷰는 10자 이상 입력해주세요.', 'warning')
    return
  }

  isSubmitting.value = true
  try {
    await reviewApi.create({
      tradeId: props.tradeId,
      rating: rating.value,
      content: content.value.trim(),
    })
    showToast('리뷰가 등록되었습니다.', 'success')
    emit('submitted')
  } catch (e) {
    const code = e.response?.data?.code
    if (code === 'REVIEW_ALREADY_EXISTS') {
      showToast('이미 리뷰를 작성했습니다.', 'error')
    } else if (code === 'REVIEW_NOT_ALLOWED') {
      showToast('리뷰 작성 기간이 지났거나 조건이 맞지 않습니다.', 'error')
    } else {
      showToast('리뷰 등록 중 오류가 발생했습니다.', 'error')
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
      <h2 class="text-lg font-bold text-gray-900 mb-1">거래 후기 작성</h2>
      <p class="text-sm text-gray-500 mb-5">{{ targetNickname }}님과의 거래는 어떠셨나요?</p>

      <!-- 별점 -->
      <div class="flex justify-center gap-2 mb-5">
        <button
          v-for="star in 5"
          :key="star"
          @click="setRating(star)"
          @mouseenter="hoverRating = star"
          @mouseleave="hoverRating = 0"
          class="text-4xl transition-transform hover:scale-110"
        >
          <span :class="star <= (hoverRating || rating) ? 'text-yellow-400' : 'text-gray-200'">★</span>
        </button>
      </div>
      <p class="text-center text-sm text-gray-500 mb-4">
        {{ ['', '별로였어요', '그냥 그랬어요', '보통이에요', '좋았어요', '최고였어요'][hoverRating || rating] || '별점을 선택해주세요' }}
      </p>

      <!-- 내용 -->
      <textarea
        v-model="content"
        placeholder="거래 경험을 자세히 남겨주세요. (10자 이상)"
        maxlength="500"
        rows="4"
        class="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-orange-400"
      />
      <p class="text-right text-xs text-gray-400 mt-1">{{ content.length }}/500</p>

      <!-- 버튼 -->
      <div class="flex gap-3 mt-4">
        <button
          @click="emit('cancel')"
          class="flex-1 py-2.5 border border-gray-200 text-gray-600 rounded-xl text-sm font-medium hover:bg-gray-50 transition-colors"
        >
          취소
        </button>
        <button
          @click="submit"
          :disabled="isSubmitting || rating === 0"
          class="flex-1 py-2.5 bg-orange-500 text-white rounded-xl text-sm font-medium hover:bg-orange-600 disabled:opacity-40 transition-colors"
        >
          {{ isSubmitting ? '등록 중...' : '후기 등록' }}
        </button>
      </div>
    </div>
  </div>
</template>
