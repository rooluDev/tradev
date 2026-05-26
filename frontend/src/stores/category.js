import { defineStore } from 'pinia'
import { ref } from 'vue'
import { itemApi } from '@/api/item'

export const useCategoryStore = defineStore('category', () => {
  const categories = ref([])
  const loaded = ref(false)

  async function fetchCategories() {
    if (loaded.value) return categories.value
    const { data } = await itemApi.getCategories()
    categories.value = data.data
    loaded.value = true
    return categories.value
  }

  function flatList() {
    const result = []
    for (const parent of categories.value) {
      result.push(parent)
      for (const child of parent.children || []) {
        result.push(child)
      }
    }
    return result
  }

  function findById(id) {
    if (!id) return null
    return flatList().find((cat) => cat.id === Number(id)) ?? null
  }

  return { categories, loaded, fetchCategories, flatList, findById }
})
