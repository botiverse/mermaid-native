import DefaultTheme from 'vitepress/theme'
import MermaidGallery from './components/MermaidGallery.vue'
import './custom.css'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('MermaidGallery', MermaidGallery)
  },
}
