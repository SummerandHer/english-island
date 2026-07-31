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
  routeRules: {
    '/sim-exam': { redirect: { to: '/islands/exam', statusCode: 301 } },
    '/sim-exam/short': { redirect: { to: '/islands/exam/short', statusCode: 301 } },
    '/sim-exam/long': { redirect: { to: '/islands/exam/long', statusCode: 301 } },
    '/sim-exam/practice/**': { redirect: { to: '/islands/exam/practice/**', statusCode: 301 } }
  },
  app: {
    head: {
      title: 'ISLAND · 四六级岛',
      meta: [{ name: 'description', content: '四六级学习平台' }],
      link: [
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        {
          rel: 'stylesheet',
          href: 'https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;600;700&family=Quicksand:wght@500;600;700&display=swap'
        }
      ]
    }
  },
  build: {
    transpile: ['naive-ui']
  }
})
