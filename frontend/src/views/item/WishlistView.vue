<template>
  <div class="max-w-screen-md mx-auto px-4 py-4">
    <h1 class="text-lg font-bold text-gray-900 mb-4">관심 목록</h1>

    <div v-if="loading && items.length === 0" class="grid grid-cols-2 sm:grid-cols-3 gap-3">
      <div v-for="i in 6" :key="i" class="rounded-xl overflow-hidden">
        <div class="skeleton aspect-square" />
        <div class="p-3 space-y-2">
          <div class="skeleton h-4 w-3/4" />
          <div class="skeleton h-4 w-1/2" />
        </div>
      </div>
    </div>

    <div v-else-if="items.length === 0" class="text-center py-20">
      <p class="text-gray-400 text-sm">관심 목록이 비어 있습니다.</p>
    </div>

    <InfiniteScrollList v-else :loading="loading" :has-next="hasNext" @load-more="loadMore">
      <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
        <ItemCard v-for="item in items" :key="item.id" :item="item" />
      </div>
    </InfiniteScrollList>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '@/api/user'
import ItemCard from '@/components/item/ItemCard.vue'
import InfiniteScrollList from '@/components/common/InfiniteScrollList.vue'

const items = ref([])
const loading = ref(false)
const hasNext = ref(false)
const cursor = ref(null)

async function fetchWishlist(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    const { data } = await userApi.getMyWishlist({ cursor: cursor.value, pageSize: 20 })
    const result = data.data
    items.value = reset ? result.items : [...items.value, ...result.items]
    cursor.value = result.nextCursor
    hasNext.value = result.hasNext
  } finally {
    loading.value = false
  }
}

function loadMore() {
  fetchWishlist(false)
}

onMounted(() => fetchWishlist(true))
</script>
