<script setup>
import { ref, computed, onMounted } from 'vue'
import { slotApi } from '@/api/reservation'
import { useAuthStore } from '@/stores/auth'
import { useApiError } from '@/composables/useApiError'
import { useToast } from '@/composables/useToast'
import BaseButton from '@/components/common/BaseButton.vue'

const authStore = useAuthStore()
const { handle } = useApiError()
const { showToast } = useToast()

const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)

const slots = ref([])
const isLoading = ref(false)
const isAdding = ref(false)

// 새 슬롯 입력
const newDate = ref('')
const newStartTime = ref('09:00')
const newEndTime = ref('10:00')

const monthLabel = computed(() =>
  `${currentYear.value}년 ${currentMonth.value}월`
)

async function fetchSlots() {
  isLoading.value = true
  try {
    const { data } = await slotApi.getByMonth(
      authStore.user.id,
      currentYear.value,
      currentMonth.value
    )
    slots.value = data.data
  } catch (e) {
    handle(e)
  } finally {
    isLoading.value = false
  }
}

function prevMonth() {
  if (currentMonth.value === 1) {
    currentYear.value--
    currentMonth.value = 12
  } else {
    currentMonth.value--
  }
  fetchSlots()
}

function nextMonth() {
  if (currentMonth.value === 12) {
    currentYear.value++
    currentMonth.value = 1
  } else {
    currentMonth.value++
  }
  fetchSlots()
}

async function addSlot() {
  if (!newDate.value || !newStartTime.value || !newEndTime.value) return
  isAdding.value = true
  try {
    await slotApi.create([{
      startedAt: `${newDate.value}T${newStartTime.value}:00`,
      endedAt: `${newDate.value}T${newEndTime.value}:00`,
    }])
    showToast('슬롯이 추가되었습니다.')
    newDate.value = ''
    fetchSlots()
  } catch (e) {
    handle(e)
  } finally {
    isAdding.value = false
  }
}

async function deleteSlot(slotId) {
  if (!window.confirm('이 슬롯을 삭제하시겠습니까?')) return
  try {
    await slotApi.delete(slotId)
    slots.value = slots.value.filter((s) => s.id !== slotId)
    showToast('슬롯이 삭제되었습니다.')
  } catch (e) {
    handle(e)
  }
}

const statusColor = {
  AVAILABLE: 'bg-green-100 text-green-700',
  LOCKED: 'bg-yellow-100 text-yellow-700',
  RESERVED: 'bg-blue-100 text-blue-700',
}

const statusLabel = { AVAILABLE: '예약 가능', LOCKED: '예약 중', RESERVED: '예약 완료' }

function formatDateTime(dt) {
  const d = new Date(dt)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(fetchSlots)
</script>

<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <h1 class="text-xl font-bold text-gray-900 mb-6">예약 슬롯 관리</h1>

    <!-- 월 네비게이션 -->
    <div class="flex items-center justify-between mb-4">
      <button @click="prevMonth" class="p-2 rounded-lg hover:bg-gray-100">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>
      <span class="font-semibold text-gray-900">{{ monthLabel }}</span>
      <button @click="nextMonth" class="p-2 rounded-lg hover:bg-gray-100">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
        </svg>
      </button>
    </div>

    <!-- 슬롯 추가 폼 -->
    <div class="bg-gray-50 rounded-xl p-4 mb-4">
      <p class="text-sm font-medium text-gray-700 mb-3">새 슬롯 추가</p>
      <div class="grid grid-cols-3 gap-2">
        <input
          v-model="newDate"
          type="date"
          class="col-span-3 sm:col-span-1 border border-gray-300 rounded-lg px-3 py-2 text-sm"
        />
        <input
          v-model="newStartTime"
          type="time"
          class="border border-gray-300 rounded-lg px-3 py-2 text-sm"
        />
        <input
          v-model="newEndTime"
          type="time"
          class="border border-gray-300 rounded-lg px-3 py-2 text-sm"
        />
      </div>
      <BaseButton
        class="mt-3 w-full"
        :loading="isAdding"
        :disabled="!newDate"
        @click="addSlot"
      >
        슬롯 추가
      </BaseButton>
    </div>

    <!-- 슬롯 목록 -->
    <div v-if="isLoading" class="py-10 text-center text-gray-400">
      <div class="inline-block w-5 h-5 border-2 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
    </div>

    <div v-else-if="slots.length === 0" class="py-10 text-center text-gray-400">
      <p class="text-3xl mb-2">📅</p>
      <p class="text-sm">이 달에 등록된 슬롯이 없습니다.</p>
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="slot in slots"
        :key="slot.id"
        class="flex items-center justify-between bg-white rounded-lg border border-gray-200 px-4 py-3"
      >
        <div>
          <p class="text-sm font-medium text-gray-900">
            {{ formatDateTime(slot.startedAt) }} ~ {{ formatDateTime(slot.endedAt) }}
          </p>
        </div>
        <div class="flex items-center gap-3">
          <span :class="['px-2 py-0.5 rounded-full text-xs font-medium', statusColor[slot.status]]">
            {{ statusLabel[slot.status] }}
          </span>
          <button
            v-if="slot.status === 'AVAILABLE'"
            @click="deleteSlot(slot.id)"
            class="text-gray-400 hover:text-red-500 transition-colors"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
