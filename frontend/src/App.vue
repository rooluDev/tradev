<template>
  <div class="min-h-screen flex flex-col">
    <AppHeader />
    <main class="flex-1 pb-16 md:pb-0">
      <RouterView />
    </main>
    <BottomNavigation />
    <ToastNotification />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import AppHeader from '@/components/common/AppHeader.vue'
import BottomNavigation from '@/components/common/BottomNavigation.vue'
import ToastNotification from '@/components/common/ToastNotification.vue'
import { useAuthStore } from '@/stores/auth'
import { useCategoryStore } from '@/stores/category'
import { useSse } from '@/composables/useSse'

const authStore = useAuthStore()
const categoryStore = useCategoryStore()
const { connect: connectSse } = useSse()

onMounted(async () => {
  categoryStore.fetchCategories()
  if (authStore.isLoggedIn) {
    connectSse()
  }
})
</script>
