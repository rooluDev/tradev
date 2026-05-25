<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'
import { useApiError } from '@/composables/useApiError'

const router = useRouter()
const { handle } = useApiError()
const stats = ref(null)

async function fetchDashboard() {
  try {
    const { data } = await api.get('/admin/api/dashboard')
    stats.value = data.data
  } catch (e) { handle(e) }
}

onMounted(fetchDashboard)
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 관리자 헤더 -->
    <div class="bg-white border-b px-6 py-4 flex items-center justify-between">
      <h1 class="text-lg font-bold text-gray-900">Tradev 관리자</h1>
      <nav class="flex gap-4 text-sm">
        <router-link to="/admin" class="text-orange-600 font-medium">대시보드</router-link>
        <router-link to="/admin/users" class="text-gray-600 hover:text-orange-600">회원</router-link>
        <router-link to="/admin/reports" class="text-gray-600 hover:text-orange-600">신고</router-link>
      </nav>
    </div>

    <div class="max-w-5xl mx-auto px-6 py-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-6">대시보드</h2>

      <div v-if="!stats" class="text-center py-20 text-gray-400">
        <div class="inline-block w-6 h-6 border-2 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
      </div>

      <div v-else class="grid grid-cols-2 md:grid-cols-3 gap-4">
        <div class="bg-white rounded-xl p-5 border border-gray-200">
          <p class="text-xs text-gray-500 mb-1">오늘 신규 가입</p>
          <p class="text-3xl font-bold text-gray-900">{{ stats.todaySignups }}</p>
        </div>
        <div class="bg-white rounded-xl p-5 border border-gray-200">
          <p class="text-xs text-gray-500 mb-1">오늘 거래 요청</p>
          <p class="text-3xl font-bold text-gray-900">{{ stats.todayTrades }}</p>
        </div>
        <div class="bg-white rounded-xl p-5 border border-red-200 bg-red-50">
          <p class="text-xs text-red-600 mb-1">처리 대기 신고</p>
          <p class="text-3xl font-bold text-red-600">{{ stats.pendingReports }}</p>
        </div>
        <div class="bg-white rounded-xl p-5 border border-gray-200">
          <p class="text-xs text-gray-500 mb-1">전체 상품</p>
          <p class="text-3xl font-bold text-gray-900">{{ stats.totalItems.toLocaleString() }}</p>
        </div>
        <div class="bg-white rounded-xl p-5 border border-gray-200">
          <p class="text-xs text-gray-500 mb-1">전체 회원</p>
          <p class="text-3xl font-bold text-gray-900">{{ stats.totalUsers.toLocaleString() }}</p>
        </div>
        <div class="bg-white rounded-xl p-5 border border-gray-200">
          <p class="text-xs text-gray-500 mb-1">전체 거래</p>
          <p class="text-3xl font-bold text-gray-900">{{ stats.totalTrades.toLocaleString() }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
