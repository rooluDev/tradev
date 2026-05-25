<script setup>
import { ref } from 'vue'
import { reportApi } from '@/api/report'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  targetType: {
    type: String,
    required: true,
    validator: (v) => ['ITEM', 'USER', 'REVIEW'].includes(v),
  },
  targetId: { type: Number, required: true },
})
const emit = defineEmits(['close'])

const { showToast } = useToast()

const REASONS = [
  { value: 'FRAUD',       label: '사기 의심' },
  { value: 'PROHIBITED',  label: '금지 물품' },
  { value: 'SPAM',        label: '스팸/도배' },
  { value: 'INAPPROPRIATE', label: '부적절한 내용' },
  { value: 'ABUSE',       label: '욕설/비방' },
  { value: 'OTHER',       label: '기타' },
]

const reason = ref('')
const detail = ref('')
const isSubmitting = ref(false)

async function submit() {
  if (!reason.value) {
    showToast('신고 사유를 선택해주세요.', 'warning')
    return
  }
  isSubmitting.value = true
  try {
    await reportApi.create({
      targetType: props.targetType,
      targetId: props.targetId,
      reason: reason.value,
      detail: detail.value.trim() || null,
    })
    showToast('신고가 접수되었습니다. 검토 후 조치하겠습니다.', 'success')
    emit('close')
  } catch (e) {
    const code = e.response?.data?.code
    if (code === 'REPORT_DUPLICATE') {
      showToast('이미 신고한 항목입니다.', 'error')
    } else if (code === 'REPORT_SELF') {
      showToast('자기 자신을 신고할 수 없습니다.', 'error')
    } else {
      showToast('신고 접수 중 오류가 발생했습니다.', 'error')
    }
  } finally {
    isSubmitting.value = false
  }
}

const TARGET_LABEL = { ITEM: '상품', USER: '사용자', REVIEW: '리뷰' }
</script>

<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" @click.self="emit('close')">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6">
      <h2 class="text-base font-bold text-gray-900 mb-1">{{ TARGET_LABEL[targetType] }} 신고</h2>
      <p class="text-sm text-gray-400 mb-4">신고 사유를 선택해주세요.</p>

      <!-- 사유 선택 -->
      <div class="space-y-2 mb-4">
        <label
          v-for="r in REASONS"
          :key="r.value"
          class="flex items-center gap-3 p-3 border rounded-xl cursor-pointer transition-colors"
          :class="reason === r.value
            ? 'border-orange-400 bg-orange-50'
            : 'border-gray-100 hover:border-gray-300'"
        >
          <input type="radio" v-model="reason" :value="r.value" class="accent-orange-500" />
          <span class="text-sm text-gray-700">{{ r.label }}</span>
        </label>
      </div>

      <!-- 상세 내용 (선택) -->
      <textarea
        v-model="detail"
        placeholder="추가 설명이 있으면 입력해주세요. (선택)"
        maxlength="300"
        rows="3"
        class="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-orange-400 mb-4"
      />

      <!-- 버튼 -->
      <div class="flex gap-3">
        <button
          @click="emit('close')"
          class="flex-1 py-2.5 border border-gray-200 text-gray-600 rounded-xl text-sm font-medium hover:bg-gray-50 transition-colors"
        >
          취소
        </button>
        <button
          @click="submit"
          :disabled="isSubmitting || !reason"
          class="flex-1 py-2.5 bg-red-500 text-white rounded-xl text-sm font-medium hover:bg-red-600 disabled:opacity-40 transition-colors"
        >
          {{ isSubmitting ? '접수 중...' : '신고하기' }}
        </button>
      </div>
    </div>
  </div>
</template>
