<template>
  <header class="sticky top-0 z-50 border-b border-gray-100 bg-white/95 backdrop-blur">
    <div class="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4">
      <NuxtLink to="/" class="text-lg font-bold text-[var(--island-primary)]">ISLAND</NuxtLink>
      <nav class="hidden items-center gap-5 text-sm md:flex">
        <NuxtLink v-for="item in navItems" :key="item.to" :to="item.to" class="nav-link" active-class="text-[var(--island-primary)] font-medium">
          {{ item.label }}
        </NuxtLink>
      </nav>
      <div class="ml-auto flex items-center gap-3">
        <template v-if="auth.isLoggedIn">
          <span class="hidden text-sm text-gray-600 sm:inline">{{ auth.user?.nickname }}</span>
          <NButton size="small" quaternary @click="logout">退出</NButton>
        </template>
        <NuxtLink v-else to="/login">
          <NButton size="small" type="primary">登录</NButton>
        </NuxtLink>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const router = useRouter()

const navItems = [
  { label: '首页', to: '/' },
  { label: '全平台', to: '/feed' },
  { label: '阅读', to: '/reading' },
  { label: '翻译', to: '/translation' },
  { label: '双语', to: '/video' }
]

onMounted(() => auth.hydrate())

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.nav-link {
  @apply text-gray-600 transition hover:text-[var(--island-primary)];
}
</style>
