import { createApp } from 'vue'
import '@phosphor-icons/web/regular'
import './style.css'
// Markdown 代码块自定义样式
import './assets/styles/markdown-code-block.css'
// Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/styles/element-plus-custom.css'
import App from './App.vue'

const app = createApp(App)
app.use(ElementPlus)
app.mount('#app')
