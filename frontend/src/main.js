import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/stores/auth'
import './style.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

// 새로고침 시 세션 복원: router guard 실행 전에 accessToken 복구
const authStore = useAuthStore()
await authStore.restoreSession()

app.use(router)
app.mount('#app')
