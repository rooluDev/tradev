<template>
  <div>
    <slot />
    <div ref="sentinel" class="h-4" />
    <div v-if="loading" class="flex justify-center py-4">
      <svg class="animate-spin w-6 h-6 text-primary-500" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
    <div v-if="!hasNext && !loading && showEndMessage" class="text-center py-6 text-sm text-gray-400">
      모든 상품을 불러왔습니다.
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  loading: Boolean,
  hasNext: Boolean,
  showEndMessage: { type: Boolean, default: true },
})
const emit = defineEmits(['load-more'])

const sentinel = ref(null)
let observer = null

onMounted(() => {
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && props.hasNext && !props.loading) {
        emit('load-more')
      }
    },
    { threshold: 0.8 }
  )
  if (sentinel.value) observer.observe(sentinel.value)
})

onUnmounted(() => observer?.disconnect())
</script>
