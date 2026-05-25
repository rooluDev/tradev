<template>
  <div class="max-w-screen-lg mx-auto px-4 py-4">
    <!-- Mobile search -->
    <div class="md:hidden mb-4">
      <div class="relative">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="상품 검색"
          class="w-full border border-gray-200 rounded-full px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          @keyup.enter="applySearch"
        />
      </div>
    </div>

    <div class="flex gap-6">
      <!-- Filter sidebar (desktop) -->
      <aside class="hidden md:block w-48 flex-shrink-0">
        <div class="bg-white rounded-xl border border-gray-100 p-4 sticky top-20">
          <h3 class="text-sm font-semibold text-gray-900 mb-3">카테고리</h3>
          <div class="space-y-1">
            <button
              :class="['w-full text-left text-sm px-2 py-1.5 rounded-lg transition-colors', !filters.categoryId ? 'bg-primary-50 text-primary-700 font-medium' : 'text-gray-600 hover:bg-gray-50']"
              @click="setCategory(null)"
            >전체</button>
            <template v-for="cat in categoryStore.categories" :key="cat.id">
              <button
                :class="['w-full text-left text-sm px-2 py-1.5 rounded-lg transition-colors', filters.categoryId === cat.id ? 'bg-primary-50 text-primary-700 font-medium' : 'text-gray-600 hover:bg-gray-50']"
                @click="setCategory(cat.id)"
              >{{ cat.name }}</button>
            </template>
          </div>

          <div class="mt-4 border-t border-gray-100 pt-4">
            <h3 class="text-sm font-semibold text-gray-900 mb-3">가격</h3>
            <div class="flex gap-2 items-center">
              <input v-model.number="filters.minPrice" type="number" placeholder="최소" class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-primary-500" />
              <span class="text-gray-400 text-xs">~</span>
              <input v-model.number="filters.maxPrice" type="number" placeholder="최대" class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-primary-500" />
            </div>
          </div>

          <BaseButton size="sm" class="w-full mt-4" @click="applySearch">필터 적용</BaseButton>
          <BaseButton variant="ghost" size="sm" class="w-full mt-2" @click="resetFilters">초기화</BaseButton>
        </div>
      </aside>

      <!-- Item grid -->
      <div class="flex-1 min-w-0">
        <!-- Skeleton -->
        <div v-if="initialLoading" class="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <div v-for="i in 6" :key="i" class="rounded-xl overflow-hidden">
            <div class="skeleton aspect-square" />
            <div class="p-3 space-y-2">
              <div class="skeleton h-4 w-3/4" />
              <div class="skeleton h-4 w-1/2" />
            </div>
          </div>
        </div>

        <template v-else>
          <div v-if="items.length === 0" class="text-center py-20">
            <p class="text-gray-400 text-sm">상품이 없습니다.</p>
          </div>

          <InfiniteScrollList :loading="loading" :has-next="hasNext" @load-more="loadMore">
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
              <ItemCard v-for="item in items" :key="item.id" :item="item" />
            </div>
          </InfiniteScrollList>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { itemApi } from '@/api/item'
import { useCategoryStore } from '@/stores/category'
import ItemCard from '@/components/item/ItemCard.vue'
import InfiniteScrollList from '@/components/common/InfiniteScrollList.vue'
import BaseButton from '@/components/common/BaseButton.vue'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()

const items = ref([])
const loading = ref(false)
const initialLoading = ref(true)
const hasNext = ref(false)
const cursor = ref(null)
const searchQuery = ref(route.query.keyword || '')

const filters = reactive({
  keyword: route.query.keyword || '',
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : null,
  minPrice: null,
  maxPrice: null,
})

async function fetchItems(reset = false) {
  if (loading.value) return
  if (reset) {
    items.value = []
    cursor.value = null
    hasNext.value = false
  }

  loading.value = true
  try {
    const params = {
      keyword: filters.keyword || undefined,
      categoryId: filters.categoryId || undefined,
      minPrice: filters.minPrice || undefined,
      maxPrice: filters.maxPrice || undefined,
      cursor: cursor.value || undefined,
      pageSize: 20,
    }
    const { data } = await itemApi.getItems(params)
    const result = data.data
    items.value = reset ? result.items : [...items.value, ...result.items]
    cursor.value = result.nextCursor
    hasNext.value = result.hasNext
  } finally {
    loading.value = false
    initialLoading.value = false
  }
}

function setCategory(id) {
  filters.categoryId = id
  applySearch()
}

function applySearch() {
  filters.keyword = searchQuery.value
  fetchItems(true)
}

function resetFilters() {
  searchQuery.value = ''
  filters.keyword = ''
  filters.categoryId = null
  filters.minPrice = null
  filters.maxPrice = null
  fetchItems(true)
}

function loadMore() {
  fetchItems(false)
}

watch(() => route.query.keyword, (kw) => {
  if (kw !== undefined) {
    searchQuery.value = kw
    filters.keyword = kw
    fetchItems(true)
  }
})

onMounted(() => fetchItems(true))
</script>
