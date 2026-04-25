import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { router } from './router/index'
import { useAuthStore } from './stores/auth'
import App from './App.vue'
import './assets/main.css'

const pinia = createPinia()
const app = createApp(App)
app.use(pinia)

const auth = useAuthStore()
auth.initFromStorage()

app.use(router)
app.mount('#app')
