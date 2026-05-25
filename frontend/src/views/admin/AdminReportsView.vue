<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { useApiError } from '@/composables/useApiError'
import { useToast } from '@/composables/useToast'

const { handle } = useApiError()
const { showToast } = useToast()

const reports = ref([])
const cursor = ref(null)
const hasNext = ref(false)
const summaries = ref({})

async function fetchReports(reset = false) {
  try {
    const params = { size: 20, status: 'PENDING' }
    if (!reset && cursor.value) params.cursor = cursor.value
    const { data } = await api.get('/admin/api/reports', { params })
    if (reset) reports.value = data.data.items
    else reports.value.push(...data.data.items)
    cursor.value = data.data.nextCursor
    hasNext.value = data.data.hasNext
  } catch (e) { handle(e) }
}

async function processReport(reportId, decision) {
  const label = decision === 'ACCEPTED' ? '인정' : '기각'
  if (!window.confirm(`신고를 ${label} 처리하시겠습니까?`)) return
  try {
    await api.post(`/admin/api/reports/${reportId}/process`, null, {
      params: { decision }
    })
    showToast(`신고를 ${label} 처리했습니다.`)
    reports.value = reports.value.filter(r => r.id !== reportId)
  } catch (e) { handle(e) }
}

async function getSummary(reportId) {
  if (summaries.value[reportId]) return
  summaries.value[reportId] = '요약 중...'
  try {
    const { data } = await api.get(`/admin/api/reports/${reportId}/summary`)
    summaries.value[reportId] = data.data
  } catch (e) {
    summaries.value[reportId] = '요약 실패'
  }
}

const reasonLabel = {
  FRAUD: '사기', PROHIBITED: '금지물품', SPAM: '스팸',
  INAPPROPRIATE: '불건전', ABUSE: '욕설', OTHER: '기타',
}

onMounted(() => fetchReports(true))
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <div class="bg-white border-b px-6 py-4 flex items-center justify-between">
      <h1 class="text-lg font-bold text-gray-900">Tradev 관리자</h1>
      <nav class="flex gap-4 text-sm">
        <router-link to="/admin" class="text-gray-600 hover:text-orange-600">대시보드</router-link>
        <router-link to="/admin/users" class="text-gray-600 hover:text-orange-600">회원</router-link>
        <router-link to="/admin/reports" class="text-orange-600 font-medium">신고</router-link>
      </nav>
    </div>

    <div class="max-w-5xl mx-auto px-6 py-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">신고 처리 (대기 중)</h2>
      <div v-if="reports.length === 0" class="py-16 text-center text-gray-400">
        처리할 신고가 없습니다. 🎉
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="report in reports"
          :key="report.id"
          class="bg-white rounded-xl border border-gray-200 p-5"
        >
          <div class="flex items-start justify-between">
            <div>
              <div class="flex items-center gap-2 mb-1">
                <span class="text-xs font-medium px-2 py-0.5 bg-orange-100 text-orange-700 rounded-full">
                  {{ report.targetType }}
                </span>
                <span class="text-xs font-medium px-2 py-0.5 bg-gray-100 text-gray-600 rounded-full">
                  {{ reasonLabel[report.reason] || report.reason }}
                </span>
              </div>
              <p class="text-sm text-gray-900">신고자: {{ report.reporterNickname }}</p>
              <p class="text-sm text-gray-600 mt-1">{{ report.detail || '(상세 내용 없음)' }}</p>
            </div>
            <div class="flex gap-2">
              <button
                @click="processReport(report.id, 'ACCEPTED')"
                class="text-xs px-3 py-1.5 bg-red-500 text-white rounded-lg hover:bg-red-600"
              >인정</button>
              <button
                @click="processReport(report.id, 'REJECTED')"
                class="text-xs px-3 py-1.5 border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50"
              >기각</button>
            </div>
          </div>

          <!-- AI 요약 -->
          <div class="mt-3">
            <button
              @click="getSummary(report.id)"
              class="text-xs text-purple-600 hover:underline flex items-center gap-1"
            >
              <span>✨</span> AI 요약 보기
            </button>
            <p v-if="summaries[report.id]" class="mt-2 text-sm text-gray-700 bg-purple-50 rounded-lg p-3">
              {{ summaries[report.id] }}
            </p>
          </div>
        </div>
      </div>
      <div v-if="hasNext" class="mt-4 text-center">
        <button @click="fetchReports(false)" class="text-sm text-orange-500 hover:underline">더 보기</button>
      </div>
    </div>
  </div>
</template>
