<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { reviewApi } from '@/api/review'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  review: { type: Object, required: true },
  /** 해당 유저 프로필 페이지의 owner인지 (답글 작성 버튼 노출) */
  isOwner: { type: Boolean, default: false },
})
const emit = defineEmits(['replied'])

const authStore = useAuthStore()
const { showToast } = useToast()

const showReplyForm = ref(false)
const replyContent = ref('')
const isSubmitting = ref(false)

function formatDate(dateStr) {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric',
  })
}

async function submitReply() {
  if (replyContent.value.trim().length < 5) {
    showToast('답글은 5자 이상 입력해주세요.', 'warning')
    return
  }
  isSubmitting.value = true
  try {
    await reviewApi.reply(props.review.id, { content: replyContent.value.trim() })
    showToast('답글이 등록되었습니다.', 'success')
    showReplyForm.value = false
    replyContent.value = ''
    emit('replied')
  } catch {
    showToast('답글 등록 중 오류가 발생했습니다.', 'error')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-100 p-4">
    <!-- 리뷰어 정보 + 별점 -->
    <div class="flex items-start justify-between mb-2">
      <div class="flex items-center gap-2">
        <div class="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center text-sm font-bold text-orange-600">
          {{ review.reviewer?.nickname?.charAt(0) || '?' }}
        </div>
        <div>
          <p class="text-sm font-medium text-gray-900">{{ review.reviewer?.nickname }}</p>
          <p class="text-xs text-gray-400">{{ formatDate(review.createdAt) }}</p>
        </div>
      </div>
      <div class="flex gap-0.5">
        <span
          v-for="star in 5"
          :key="star"
          :class="star <= review.rating ? 'text-yellow-400' : 'text-gray-200'"
          class="text-base"
        >★</span>
      </div>
    </div>

    <!-- 리뷰 내용 -->
    <p class="text-sm text-gray-700 leading-relaxed">{{ review.content }}</p>

    <!-- 답글 -->
    <div v-if="review.reply" class="mt-3 bg-gray-50 rounded-lg p-3">
      <p class="text-xs font-semibold text-gray-500 mb-1">판매자 답글</p>
      <p class="text-sm text-gray-700">{{ review.reply }}</p>
    </div>

    <!-- 답글 작성 (소유자만, 아직 답글 없는 경우) -->
    <div v-if="isOwner && !review.reply" class="mt-3">
      <button
        v-if="!showReplyForm"
        @click="showReplyForm = true"
        class="text-xs text-orange-500 hover:text-orange-600 font-medium"
      >
        답글 달기
      </button>
      <div v-else>
        <textarea
          v-model="replyContent"
          placeholder="리뷰에 답글을 남겨보세요."
          maxlength="300"
          rows="2"
          class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-orange-400"
        />
        <div class="flex gap-2 mt-1.5">
          <button
            @click="showReplyForm = false; replyContent = ''"
            class="text-xs text-gray-400 hover:text-gray-600"
          >취소</button>
          <button
            @click="submitReply"
            :disabled="isSubmitting"
            class="text-xs text-orange-500 font-medium hover:text-orange-600 disabled:opacity-40"
          >
            {{ isSubmitting ? '등록 중...' : '등록' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
