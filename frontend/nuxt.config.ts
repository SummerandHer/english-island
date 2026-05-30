// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss', '@pinia/nuxt'],
  css: ['~/assets/css/main.css'],
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080'
    }
  },
  app: {
    head: {
      title: 'ISLAND · 四六级岛',
      meta: [{ name: 'description', content: '四六级学习平台' }]
    }
  },
  build: {
    transpile: ['naive-ui']
  }
})
