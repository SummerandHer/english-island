import naive from 'naive-ui'
import { setup } from '@css-render/vue3-ssr'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.use(naive)

  if (import.meta.server) {
    const { collect } = setup(nuxtApp.vueApp)
    useHead({
      style: () =>
        collect()
          .split('</style>')
          .filter(Boolean)
          .map((block) => {
            const id = /cssr-id="(.+?)"/.exec(block)?.[1]
            const innerHTML = (/>(.*)/s.exec(block)?.[1] ?? '').trim()
            return { key: id, innerHTML }
          })
    })
  }
})
