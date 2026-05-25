<script setup>
import { ref, onMounted } from 'vue'
import { tradeApi } from '@/api/trade'
import { useApiError } from '@/composables/useApiError'
import TradeCard from '@/components/trade/TradeCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'

const { handle } = useApiError()

const trades = ref([])
const cursor = ref(null)
const hasNext = ref(false)
const isLoading = ref(false)
const activeStatus = ref(null)

const statusOptions = [
  { value: null, label: '전체' },
  { value: 'PENDING', label: '요청 중' },
  { value: 'RESERVED', label: '수락됨' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'CANCELLED', label: '취소됨' },
]

async function fetchTrades(reset = false) {
  if (isLoading.value) return
  isLoading.value = true
  try {
    const params = { size: 10 }
    if (activeStatus.value) params.status = activeStatus.value
    if (!reset && cursor.value) params.cursor = cursor.value

    const { data } = await tradeApi.getList(params)
    if (reset) {
      trades.value = data.data.items
    } else {
      trades.value.push(...data.data.items)
    }
    cursor.value = data.data.nextCursor
    hasNext.value = data.data.hasNext
  } catch (e) {
    handle(e)
  } finally {
    isLoading.value = false
  }
}

function selectStatus(status) {
  activeStatus.value = status
  cursor.value = null
  fetchTrades(true)
}

onMounted(() => fetchTrades(true))
</script>

<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <h1 class="text-xl font-bold text-gray-900 mb-4">나의 거래</h1>

    <!-- 상태 필터 탭 -->
    <div class="flex gap-2 mb-5 overflow-x-auto pb-1">
      <button
        v-for="opt in statusOptions"
        :key="opt.value"
        @click="selectStatus(opt.value)"
        :class="[
          'px-3 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors',
          activeStatus === opt.value
            ? 'bg-orange-500 text-white'
            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
        ]"
      >
        {{ opt.label }}
      </button>
    </div>

    <!-- 거래 목록 -->
    <div v-if="trades.length === 0 && !isLoading" class="py-16 text-center text-gray-400">
      <p class="text-4xl mb-3">📦</p>
      <p>진행 중인 거래가 없습니다.</p>
    </div>

    <div v-else class="space-y-3">
      <TradeCard
        v-for="trade in trades"
        :key="trade.id"
        :trade="trade"
        @updated="fetchTrades(true)"
      />
    </div>

    <!-- 더 보기 -->
    <div v-if="hasNext" class="mt-4 text-center">
      <BaseButton
        variant="outline"
        :loading="isLoading"
        @click="fetchTrades(false)"
      >
        더 보기
      </BaseButton>
    </div>

    <div v-if="isLoading && trades.length === 0" class="py-16 text-center text-gray-400">
      <div class="inline-block w-6 h-6 border-2 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
    </div>
  </div>
</template>
