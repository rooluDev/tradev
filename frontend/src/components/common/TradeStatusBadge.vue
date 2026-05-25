<template>
  <span :class="['inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium', badgeClass]">
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
})

const config = {
  SALE: { label: '판매중', class: 'bg-green-100 text-green-700' },
  RESERVED: { label: '예약중', class: 'bg-yellow-100 text-yellow-700' },
  COMPLETED: { label: '판매완료', class: 'bg-gray-100 text-gray-500' },
  PENDING: { label: '요청중', class: 'bg-blue-100 text-blue-700' },
  CANCELLED: { label: '취소', class: 'bg-red-100 text-red-500' },
  REJECTED: { label: '거절', class: 'bg-red-100 text-red-500' },
}

const badgeClass = computed(() => config[props.status]?.class || 'bg-gray-100 text-gray-500')
const label = computed(() => config[props.status]?.label || props.status)
</script>
