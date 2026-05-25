<template>
  <div class="max-w-screen-sm mx-auto px-4 py-6">
    <h1 class="text-lg font-bold text-gray-900 mb-6">{{ isEdit ? '상품 수정' : '상품 등록' }}</h1>

    <form @submit.prevent="onSubmit" class="space-y-5">
      <!-- Images -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">이미지 <span class="text-red-500">*</span></label>
        <div class="flex gap-2 flex-wrap">
          <label
            v-for="(img, i) in previewImages"
            :key="i"
            class="relative w-20 h-20 rounded-lg overflow-hidden cursor-pointer border-2 border-primary-300"
          >
            <img :src="img" class="w-full h-full object-cover" />
            <button
              type="button"
              class="absolute top-0.5 right-0.5 w-5 h-5 bg-black/60 text-white rounded-full flex items-center justify-center text-xs"
              @click.prevent="removeImage(i)"
            >✕</button>
          </label>
          <label
            v-if="previewImages.length < 10"
            class="w-20 h-20 border-2 border-dashed border-gray-300 rounded-lg flex flex-col items-center justify-center cursor-pointer hover:border-primary-400 transition-colors text-gray-400 text-xs"
          >
            <svg class="w-6 h-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            <span>추가</span>
            <input type="file" accept="image/*" multiple class="hidden" @change="onFilePick" />
          </label>
        </div>
        <p v-if="errors.images" class="mt-1 text-xs text-red-500">{{ errors.images }}</p>
        <p class="mt-1 text-xs text-gray-400">최대 10장, 첫 번째 이미지가 대표 이미지</p>
      </div>

      <!-- Category -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">카테고리 <span class="text-red-500">*</span></label>
        <select v-model="form.categoryId" class="input-field">
          <option value="">카테고리 선택</option>
          <template v-for="cat in categoryStore.categories" :key="cat.id">
            <option disabled>── {{ cat.name }}</option>
            <option v-for="sub in cat.children" :key="sub.id" :value="sub.id">{{ sub.name }}</option>
          </template>
        </select>
        <p v-if="errors.categoryId" class="mt-1 text-xs text-red-500">{{ errors.categoryId }}</p>
      </div>

      <BaseInput v-model="form.title" label="제목" placeholder="상품 제목 입력" :error="errors.title" required />

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">설명 <span class="text-red-500">*</span></label>
        <textarea
          v-model="form.description"
          rows="5"
          placeholder="상품 설명을 입력해주세요."
          class="input-field resize-none"
          :class="{ 'input-error': errors.description }"
        />
        <p v-if="errors.description" class="mt-1 text-xs text-red-500">{{ errors.description }}</p>
      </div>

      <!-- AI 설명 자동완성 -->
      <AiDescriptionAssist
        :title="form.title"
        :category-name="categoryStore.findById(form.categoryId)?.name || ''"
        @apply="(text) => form.description = text"
      />

      <BaseInput v-model.number="form.price" label="가격" type="number" placeholder="0" hint="0원은 무료나눔으로 표시됩니다." :error="errors.price" required />

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">거래 방식</label>
        <div class="flex gap-2">
          <label v-for="opt in tradeTypeOptions" :key="opt.value" class="flex-1">
            <input v-model="form.tradeType" type="radio" :value="opt.value" class="sr-only" />
            <div
              :class="['text-center border rounded-lg py-2 text-sm cursor-pointer transition-colors', form.tradeType === opt.value ? 'border-primary-500 bg-primary-50 text-primary-700 font-medium' : 'border-gray-200 text-gray-600 hover:border-gray-300']"
            >{{ opt.label }}</div>
          </label>
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">상품 상태</label>
        <div class="flex gap-2 flex-wrap">
          <label v-for="opt in conditionOptions" :key="opt.value">
            <input v-model="form.itemCondition" type="radio" :value="opt.value" class="sr-only" />
            <div
              :class="['border rounded-lg px-3 py-1.5 text-sm cursor-pointer transition-colors', form.itemCondition === opt.value ? 'border-primary-500 bg-primary-50 text-primary-700 font-medium' : 'border-gray-200 text-gray-600 hover:border-gray-300']"
            >{{ opt.label }}</div>
          </label>
        </div>
      </div>

      <BaseInput v-model="form.location" label="거래 장소" placeholder="예: 강남역 2번 출구" />

      <BaseButton type="submit" class="w-full" :loading="submitting">
        {{ isEdit ? '수정 완료' : '등록하기' }}
      </BaseButton>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { itemApi } from '@/api/item'
import { useCategoryStore } from '@/stores/category'
import { useImageUpload } from '@/composables/useImageUpload'
import { useToast } from '@/composables/useToast'
import { useApiError } from '@/composables/useApiError'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import AiDescriptionAssist from '@/components/ai/AiDescriptionAssist.vue'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()
const { upload, uploading } = useImageUpload()
const { success } = useToast()
const { handle } = useApiError()

const isEdit = computed(() => !!route.params.itemId)
const submitting = ref(false)
const previewImages = ref([])
const pickedFiles = ref([])
const existingS3Keys = ref([])
const form = reactive({
  categoryId: '',
  title: '',
  description: '',
  price: 0,
  tradeType: 'ALL',
  itemCondition: 'GOOD',
  location: '',
})
const errors = reactive({})

const tradeTypeOptions = [
  { label: '모두', value: 'ALL' },
  { label: '직거래', value: 'DIRECT' },
  { label: '택배', value: 'DELIVERY' },
]
const conditionOptions = [
  { label: '새상품', value: 'NEW' },
  { label: '거의새것', value: 'LIKE_NEW' },
  { label: '상태좋음', value: 'GOOD' },
  { label: '보통', value: 'FAIR' },
  { label: '상태나쁨', value: 'POOR' },
]

let formDirty = false

onMounted(async () => {
  await categoryStore.fetchCategories()
  if (isEdit.value) {
    const { data } = await itemApi.getItem(route.params.itemId)
    const item = data.data
    form.categoryId = item.category.id
    form.title = item.title
    form.description = item.description
    form.price = item.price
    form.tradeType = item.tradeType
    form.itemCondition = item.itemCondition
    form.location = item.location || ''
    previewImages.value = item.images.map((i) => i.imageUrl)
    existingS3Keys.value = item.images.map((i) => i.s3Key)
  }
})

onBeforeRouteLeave((to, from, next) => {
  if (formDirty && !confirm('작성 중인 내용이 있습니다. 나가시겠습니까?')) {
    next(false)
  } else {
    next()
  }
})

function onFilePick(e) {
  const files = Array.from(e.target.files)
  pickedFiles.value = [...pickedFiles.value, ...files]
  files.forEach((f) => {
    const reader = new FileReader()
    reader.onload = (ev) => previewImages.value.push(ev.target.result)
    reader.readAsDataURL(f)
  })
  formDirty = true
}

function removeImage(idx) {
  previewImages.value.splice(idx, 1)
  if (idx < existingS3Keys.value.length) {
    existingS3Keys.value.splice(idx, 1)
  } else {
    pickedFiles.value.splice(idx - existingS3Keys.value.length, 1)
  }
}

async function onSubmit() {
  errors.images = ''
  if (previewImages.value.length === 0) {
    errors.images = '이미지를 최소 1장 등록해주세요.'
    return
  }

  submitting.value = true
  try {
    let newS3Keys = []
    if (pickedFiles.value.length > 0) {
      newS3Keys = await upload(pickedFiles.value)
      if (!newS3Keys) return
    }

    const imageS3Keys = [...existingS3Keys.value, ...newS3Keys]
    const payload = { ...form, imageS3Keys }

    if (isEdit.value) {
      await itemApi.updateItem(route.params.itemId, payload)
      success('상품이 수정되었습니다.')
      router.push(`/items/${route.params.itemId}`)
    } else {
      const { data } = await itemApi.createItem(payload)
      success('상품이 등록되었습니다.')
      formDirty = false
      router.push(`/items/${data.data.id}`)
    }
  } catch (err) {
    handle(err)
  } finally {
    submitting.value = false
  }
}
</script>
