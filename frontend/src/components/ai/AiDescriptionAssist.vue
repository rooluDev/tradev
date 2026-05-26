<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import api from '@/api/axios'

const props = defineProps({
  title: { type: String, default: '' },
  categoryName: { type: String, default: '' },
})
const emit = defineEmits(['apply'])

const authStore = useAuthStore()
const { warning } = useToast()

const isGenerating = ref(false)
const generatedText = ref('')
const error = ref('')

async function generate() {
  if (!props.title) {
    warning('상품명을 먼저 입력해주세요.')
    return
  }
  if (!authStore.accessToken) return

  isGenerating.value = true
  generatedText.value = ''
  error.value = ''

  try {
    const { data } = await api.get('/ai/item-description', {
      params: {
        title: props.title,
        categoryName: props.categoryName || '기타',
      },
    })
    const text = data.data?.description || ''
    generatedText.value = text
    if (text) {
      emit('apply', text)
    }
  } catch (e) {
    error.value = 'AI 생성 중 오류가 발생했습니다.'
  } finally {
    isGenerating.value = false
  }
}
</script>

<template>
  <div class="border border-gray-200 rounded-xl p-4 bg-gradient-to-br from-purple-50 to-white">
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-2">
        <span class="text-lg">✨</span>
        <span class="text-sm font-semibold text-purple-700">AI 설명 자동완성</span>
      </div>
      <button
        @click="generate"
        :disabled="isGenerating || !title"
        class="text-xs px-3 py-1.5 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-40 transition-colors"
      >
        {{ isGenerating ? '생성 중...' : '생성하기' }}
      </button>
    </div>

    <!-- 생성 중 애니메이션 -->
    <div v-if="isGenerating" class="flex gap-1 py-2">
      <span v-for="i in 3" :key="i"
        :style="{ animationDelay: `${(i - 1) * 0.2}s` }"
        class="w-2 h-2 bg-purple-400 rounded-full animate-bounce"
      />
    </div>

    <!-- 생성 결과 미리보기 -->
    <div v-if="!isGenerating && generatedText" class="mt-2">
      <p class="text-sm text-gray-500 whitespace-pre-wrap leading-relaxed">
        {{ generatedText }}
      </p>
    </div>

    <!-- 에러 -->
    <p v-if="error" class="text-xs text-red-500 mt-2">{{ error }}</p>

    <p v-if="!generatedText && !isGenerating && !error" class="text-xs text-gray-400">
      상품명과 카테고리를 입력하면 AI가 설명을 작성해드립니다. (일일 10회)
    </p>
  </div>
</template>
