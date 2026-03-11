import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import './assets/styles/global.css'

const app = createApp(App)

app.use(router)
app.use(store)

// 将store实例挂载到window对象，以便在API拦截器中使用
window.store = store

app.mount('#app')