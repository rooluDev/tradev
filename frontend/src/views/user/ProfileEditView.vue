<template>
  <div class="max-w-screen-sm mx-auto px-4 py-6">
    <h1 class="text-lg font-bold text-gray-900 mb-6">프로필 수정</h1>

    <form @submit.prevent="onSubmit" class="space-y-5">
      <!-- Profile image -->
      <div class="flex flex-col items-center gap-3">
        <div class="relative w-20 h-20">
          <div class="w-full h-full rounded-full overflow-hidden bg-primary-100 flex items-center justify-center">
            <img v-if="previewImage" :src="previewImage" class="w-full h-full object-cover" />
            <span v-else class="text-3xl font-bold text-primary-600">{{ authStore.user?.nickname?.[0] }}</span>
          </div>
          <label class="absolute bottom-0 right-0 w-6 h-6 bg-gray-800 rounded-full flex items-center justify-center cursor-pointer">
            <svg class="w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
            </svg>
            <input type="file" accept="image/*" class="hidden" @change="onImagePick" />
          </label>
        </div>
      </div>

      <BaseInput v-model="form.nickname" label="닉네임" placeholder="2~15자" :error="errors.nickname" />
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">소개</label>
        <textarea v-model="form.bio" rows="3" placeholder="자기소개를 입력하세요." class="input-field resize-none" maxlength="200" />
        <p class="mt-1 text-xs text-gray-400 text-right">{{ form.bio.length }}/200</p>
      </div>

      <BaseButton type="submit" class="w-full" :loading="submitting">저장</BaseButton>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useImageUpload } from '@/composables/useImageUpload'
import { useToast } from '@/composables/useToast'
import { useApiError } from '@/composables/useApiError'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseButton from '@/components/common/BaseButton.vue'

const router = useRouter()
const authStore = useAuthStore()
const { upload } = useImageUpload()
const { success } = useToast()
const { handle } = useApiError()

const submitting = ref(false)
const previewImage = ref(authStore.user?.profileImageUrl || '')
const pickedFile = ref(null)
const form = reactive({ nickname: authStore.user?.nickname || '', bio: authStore.user?.bio || '' })
const errors = reactive({ nickname: '' })

function onImagePick(e) {
  const file = e.target.files[0]
  if (!file) return
  pickedFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => (previewImage.value = ev.target.result)
  reader.readAsDataURL(file)
}

async function onSubmit() {
  submitting.value = true
  try {
    let profileImageS3Key = undefined
    if (pickedFile.value) {
      const keys = await upload([pickedFile.value])
      if (!keys) return
      profileImageS3Key = keys[0]
    }

    const { data } = await userApi.updateProfile({ ...form, profileImageS3Key })
    authStore.setUser(data.data)
    success('프로필이 수정되었습니다.')
    router.push(`/users/${authStore.user.id}`)
  } catch (err) {
    handle(err)
  } finally {
    submitting.value = false
  }
}
</script>
