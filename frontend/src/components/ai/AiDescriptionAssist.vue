<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  title: { type: String, default: '' },
  categoryName: { type: String, default: '' },
})
const emit = defineEmits(['apply'])

const authStore = useAuthStore()
const { showToast } = useToast()

const isGenerating = ref(false)
const generatedText = ref('')
const error = ref('')

async function generate() {
  if (!props.title) {
    showToast('상품명을 먼저 입력해주세요.', 'warning')
    return
  }
  if (!authStore.accessToken) return

  isGenerating.value = true
  generatedText.value = ''
  error.value = ''

  try {
    const params = new URLSearchParams({
      title: props.title,
      categoryName: props.categoryName || '기타',
    })
    const url = `/api/ai/item-description?${params}&token=${encodeURIComponent(authStore.accessToken)}`

    // Fetch Streams API로 SSE 수신
    const response = await fetch(url)
    if (!response.ok) {
      const data = await response.json()
      if (data.code === 'AI_002') {
        error.value = '오늘 AI 사용 한도(10회)를 초과했습니다.'
      } else {
        error.value = 'AI 생성 중 오류가 발생했습니다.'
      }
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value)
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (line.startsWith('data: ')) {
          generatedText.value += line.slice(6)
        }
      }
    }
  } catch (e) {
    error.value = 'AI 서버에 연결할 수 없습니다.'
    console.error('[AI]', e)
  } finally {
    isGenerating.value = false
  }
}

function apply() {
  if (generatedText.value) {
    emit('apply', generatedText.value)
    generatedText.value = ''
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
    <div v-if="isGenerating && !generatedText" class="flex gap-1 py-2">
      <span v-for="i in 3" :key="i"
        :style="{ animationDelay: `${(i - 1) * 0.2}s` }"
        class="w-2 h-2 bg-purple-400 rounded-full animate-bounce"
      />
    </div>

    <!-- 생성 결과 -->
    <div v-if="generatedText || isGenerating" class="mt-2">
      <p class="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed min-h-[60px]">
        {{ generatedText }}<span v-if="isGenerating" class="inline-block w-0.5 h-4 bg-purple-500 animate-pulse ml-0.5 align-middle" />
      </p>
      <button
        v-if="!isGenerating && generatedText"
        @click="apply"
        class="mt-3 w-full py-2 text-sm font-medium border border-purple-400 text-purple-700 rounded-lg hover:bg-purple-50 transition-colors"
      >
        이 내용 사용하기
      </button>
    </div>

    <!-- 에러 -->
    <p v-if="error" class="text-xs text-red-500 mt-2">{{ error }}</p>

    <p v-if="!generatedText && !isGenerating && !error" class="text-xs text-gray-400">
      상품명과 카테고리를 입력하면 AI가 설명을 작성해드립니다. (일일 10회)
    </p>
  </div>
</template>
