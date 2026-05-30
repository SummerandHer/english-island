<template>
  <div class="min-h-screen bg-gray-50">
    <header class="border-b bg-white">
      <div class="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
        <div class="flex items-center gap-4">
          <NuxtLink to="/" class="text-sm text-gray-500">← 返回站点</NuxtLink>
          <span class="font-semibold text-[var(--island-primary)]">ISLAND 管理后台</span>
        </div>
        <nav class="flex gap-4 text-sm">
          <NuxtLink to="/admin/videos" class="text-gray-600 hover:text-[var(--island-primary)]">双语视频</NuxtLink>
          <NuxtLink to="/admin/series" class="text-gray-600 hover:text-[var(--island-primary)]">系列</NuxtLink>
        </nav>
      </div>
    </header>
    <main class="mx-auto max-w-6xl px-4 py-6">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
onMounted(async () => {
  const auth = useAuthStore()
  auth.hydrate()
  if (auth.token) {
    try {
      const { request } = useApi()
      const me = await request<import('~/types/api').UserProfile>('/api/v1/auth/me')
      auth.user = { ...auth.user, ...me } as import('~/types/api').UserProfile
      if (import.meta.client) {
        localStorage.setItem('island_user', JSON.stringify(auth.user))
      }
    } catch {
      /* ignore */
    }
  }
})
</script>
