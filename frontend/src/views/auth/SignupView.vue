<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-8">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <RouterLink to="/" class="text-2xl font-bold text-primary-600">Tradev</RouterLink>
        <p class="mt-2 text-sm text-gray-500">회원가입</p>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <form @submit.prevent="onSubmit" class="space-y-4">
          <div>
            <BaseInput
              v-model="form.email"
              id="email"
              label="이메일"
              type="email"
              placeholder="이메일 입력"
              :error="errors.email"
              required
              @input="onEmailInput"
            />
            <p v-if="emailStatus === 'available'" class="mt-1 text-xs text-green-600">사용 가능한 이메일입니다.</p>
            <p v-else-if="emailStatus === 'taken'" class="mt-1 text-xs text-red-500">이미 사용 중인 이메일입니다.</p>
          </div>

          <div>
            <BaseInput
              v-model="form.nickname"
              id="nickname"
              label="닉네임"
              placeholder="2~15자 (한글, 영문, 숫자, _)"
              :error="errors.nickname"
              required
              @input="onNicknameInput"
            />
            <p v-if="nicknameStatus === 'available'" class="mt-1 text-xs text-green-600">사용 가능한 닉네임입니다.</p>
            <p v-else-if="nicknameStatus === 'taken'" class="mt-1 text-xs text-red-500">이미 사용 중인 닉네임입니다.</p>
          </div>

          <BaseInput
            v-model="form.password"
            id="password"
            label="비밀번호"
            type="password"
            placeholder="영문+숫자 포함 8~20자"
            :error="errors.password"
            required
          />

          <!-- Password strength -->
          <div v-if="form.password" class="space-y-1">
            <div class="flex gap-1">
              <div
                v-for="i in 4"
                :key="i"
                :class="['h-1 flex-1 rounded-full transition-colors', i <= passwordStrength ? strengthColor : 'bg-gray-200']"
              />
            </div>
            <p class="text-xs" :class="strengthTextColor">{{ strengthLabel }}</p>
          </div>

          <BaseButton type="submit" class="w-full" :loading="loading">회원가입</BaseButton>
        </form>
      </div>

      <p class="text-center mt-4 text-sm text-gray-500">
        이미 계정이 있으신가요?
        <RouterLink to="/login" class="text-primary-600 font-medium hover:underline">로그인</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { useToast } from '@/composables/useToast'
import { useApiError } from '@/composables/useApiError'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseButton from '@/components/common/BaseButton.vue'

const router = useRouter()
const { success } = useToast()
const { handle } = useApiError()

const loading = ref(false)
const emailStatus = ref('')
const nicknameStatus = ref('')
const form = reactive({ email: '', nickname: '', password: '' })
const errors = reactive({ email: '', nickname: '', password: '' })

let emailTimer, nicknameTimer

function onEmailInput() {
  emailStatus.value = ''
  clearTimeout(emailTimer)
  if (!form.email || !form.email.includes('@')) return
  emailTimer = setTimeout(async () => {
    try {
      const { data } = await authApi.checkEmail(form.email)
      emailStatus.value = data.data.available ? 'available' : 'taken'
    } catch {}
  }, 500)
}

function onNicknameInput() {
  nicknameStatus.value = ''
  clearTimeout(nicknameTimer)
  if (!form.nickname || form.nickname.length < 2) return
  nicknameTimer = setTimeout(async () => {
    try {
      const { data } = await authApi.checkNickname(form.nickname)
      nicknameStatus.value = data.data.available ? 'available' : 'taken'
    } catch {}
  }, 500)
}

const passwordStrength = computed(() => {
  const p = form.password
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (/[A-Z]/.test(p)) score++
  if (/[0-9]/.test(p)) score++
  if (/[!@#$%^&*]/.test(p)) score++
  return score
})

const strengthColor = computed(() => ({
  1: 'bg-red-400', 2: 'bg-yellow-400', 3: 'bg-blue-400', 4: 'bg-green-500'
}[passwordStrength.value] || 'bg-gray-200'))

const strengthTextColor = computed(() => ({
  1: 'text-red-500', 2: 'text-yellow-600', 3: 'text-blue-600', 4: 'text-green-600'
}[passwordStrength.value] || 'text-gray-400'))

const strengthLabel = computed(() => ({
  1: '매우 약함', 2: '약함', 3: '보통', 4: '강함'
}[passwordStrength.value] || ''))

async function onSubmit() {
  loading.value = true
  try {
    await authApi.signup(form)
    success('회원가입이 완료되었습니다. 로그인해주세요.')
    router.push('/login')
  } catch (err) {
    handle(err)
  } finally {
    loading.value = false
  }
}
</script>
