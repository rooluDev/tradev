<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { useApiError } from '@/composables/useApiError'
import { useToast } from '@/composables/useToast'

const { handle } = useApiError()
const { showToast } = useToast()

const users = ref([])
const cursor = ref(null)
const hasNext = ref(false)
const isLoading = ref(false)

async function fetchUsers(reset = false) {
  isLoading.value = true
  try {
    const params = { size: 20 }
    if (!reset && cursor.value) params.cursor = cursor.value
    const { data } = await api.get('/admin/api/users', { params })
    if (reset) users.value = data.data.items
    else users.value.push(...data.data.items)
    cursor.value = data.data.nextCursor
    hasNext.value = data.data.hasNext
  } catch (e) { handle(e) }
  finally { isLoading.value = false }
}

async function suspend(userId) {
  const days = window.prompt('정지 일수 (기본 7일):', '7')
  if (!days) return
  try {
    await api.patch(`/admin/api/users/${userId}/suspend?days=${days}`)
    showToast(`${days}일 정지 처리되었습니다.`)
    fetchUsers(true)
  } catch (e) { handle(e) }
}

async function activate(userId) {
  if (!window.confirm('정지를 해제하시겠습니까?')) return
  try {
    await api.patch(`/admin/api/users/${userId}/activate`)
    showToast('정지가 해제되었습니다.')
    fetchUsers(true)
  } catch (e) { handle(e) }
}

const statusBadge = {
  ACTIVE: 'bg-green-100 text-green-700',
  SUSPENDED: 'bg-red-100 text-red-700',
  WITHDRAWN: 'bg-gray-100 text-gray-500',
}

onMounted(() => fetchUsers(true))
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <div class="bg-white border-b px-6 py-4 flex items-center justify-between">
      <h1 class="text-lg font-bold text-gray-900">Tradev 관리자</h1>
      <nav class="flex gap-4 text-sm">
        <router-link to="/admin" class="text-gray-600 hover:text-orange-600">대시보드</router-link>
        <router-link to="/admin/users" class="text-orange-600 font-medium">회원</router-link>
        <router-link to="/admin/reports" class="text-gray-600 hover:text-orange-600">신고</router-link>
      </nav>
    </div>

    <div class="max-w-5xl mx-auto px-6 py-8">
      <h2 class="text-xl font-semibold text-gray-800 mb-4">회원 관리</h2>
      <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b">
            <tr>
              <th class="text-left px-4 py-3 text-gray-500 font-medium">ID</th>
              <th class="text-left px-4 py-3 text-gray-500 font-medium">닉네임</th>
              <th class="text-left px-4 py-3 text-gray-500 font-medium">이메일</th>
              <th class="text-left px-4 py-3 text-gray-500 font-medium">상태</th>
              <th class="text-left px-4 py-3 text-gray-500 font-medium">신뢰점수</th>
              <th class="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="user in users" :key="user.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 text-gray-400">{{ user.id }}</td>
              <td class="px-4 py-3 font-medium text-gray-900">{{ user.nickname }}</td>
              <td class="px-4 py-3 text-gray-600">{{ user.email }}</td>
              <td class="px-4 py-3">
                <span :class="['px-2 py-0.5 rounded-full text-xs font-medium', statusBadge[user.status]]">
                  {{ user.status }}
                </span>
              </td>
              <td class="px-4 py-3 text-gray-600">{{ user.trustScore }}</td>
              <td class="px-4 py-3 text-right">
                <button
                  v-if="user.status === 'ACTIVE'"
                  @click="suspend(user.id)"
                  class="text-xs text-red-500 hover:underline mr-3"
                >정지</button>
                <button
                  v-else-if="user.status === 'SUSPENDED'"
                  @click="activate(user.id)"
                  class="text-xs text-blue-500 hover:underline"
                >해제</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="hasNext" class="p-4 text-center border-t">
          <button @click="fetchUsers(false)" class="text-sm text-orange-500 hover:underline">더 보기</button>
        </div>
      </div>
    </div>
  </div>
</template>
