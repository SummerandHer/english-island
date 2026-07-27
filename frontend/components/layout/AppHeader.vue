<template>
  <header class="app-header" :class="{ 'app-header--immersive': immersive }">
    <div class="app-header-inner" :class="{ 'app-header-inner--wide': immersive }">
      <div class="flex items-center gap-3 md:gap-8">
        <NButton class="md:hidden" quaternary size="small" @click="drawerOpen = true">☰</NButton>
        <NuxtLink to="/" class="app-logo">
          <span class="app-logo-mark">ISLAND</span>
          <span class="app-logo-sub hidden sm:inline">四六级岛</span>
        </NuxtLink>
        <nav class="hidden items-center gap-1 md:flex">
          <NuxtLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            active-class="nav-link-active"
          >
            {{ item.label }}
          </NuxtLink>
        </nav>
      </div>

      <div class="flex items-center gap-2 md:gap-3">
        <template v-if="auth.isLoggedIn">
          <NuxtLink
            to="/vocabulary?tab=notebook"
            class="hidden text-sm text-gray-600 hover:text-[var(--island-primary)] sm:inline"
          >
            生词本
          </NuxtLink>
          <NuxtLink
            v-if="auth.isAdmin"
            to="/admin/videos"
            class="hidden text-sm text-[var(--island-primary)] sm:inline"
          >
            管理后台
          </NuxtLink>
          <NuxtLink to="/me" class="user-chip hidden sm:flex">
            <span class="user-avatar">{{ avatarLetter }}</span>
            <span class="user-name">{{ auth.user?.nickname }}</span>
            <span v-if="auth.isVip" class="vip-badge">VIP</span>
          </NuxtLink>
          <NButton size="small" quaternary @click="logout">退出</NButton>
        </template>
        <template v-else>
          <NButton size="small" quaternary @click="goLogin">登录</NButton>
          <NButton size="small" type="primary" @click="goRegister">注册</NButton>
        </template>
      </div>
    </div>

    <NDrawer v-model:show="drawerOpen" placement="left" :width="280">
      <NDrawerContent title="导航" closable>
        <nav class="flex flex-col gap-1">
          <NuxtLink
            v-for="item in allNavItems"
            :key="item.to"
            :to="item.to"
            class="rounded-lg px-3 py-2.5 text-sm text-gray-700 hover:bg-green-50"
            @click="drawerOpen = false"
          >
            {{ item.label }}
          </NuxtLink>
        </nav>
      </NDrawerContent>
    </NDrawer>
  </header>
</template>

<script setup lang="ts">
defineProps<{ immersive?: boolean }>()

const auth = useAuthStore()
const router = useRouter()
const drawerOpen = ref(false)

const navItems = [
  { label: '首页', to: '/' },
  { label: '日报岛', to: '/daily' },
  { label: '双语岛', to: '/video' },
  { label: '仿真题岛', to: '/islands/exam' },
  { label: '我的', to: '/me' }
]

const allNavItems = [...navItems]

const avatarLetter = computed(() => {
  const name = auth.user?.nickname || '?'
  return name.charAt(0).toUpperCase()
})

onMounted(() => auth.hydrate())

function goLogin() {
  router.push('/login')
}

function goRegister() {
  router.push('/register')
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  @apply sticky top-0 z-50 border-b border-gray-100/80 bg-white/90 backdrop-blur-md;
}

.app-header--immersive {
  @apply sticky top-0 border-b border-gray-100 bg-white;
}

.app-header-inner {
  @apply mx-auto flex h-14 max-w-6xl items-center justify-between px-4;
}

.app-header-inner--wide {
  @apply max-w-none px-6;
}

.app-logo {
  @apply flex items-center gap-2;
}

.app-logo-mark {
  @apply text-lg font-bold tracking-tight text-[var(--island-primary)];
}

.app-logo-sub {
  @apply text-xs font-normal text-gray-400;
}

.nav-link {
  @apply rounded-lg px-3 py-1.5 text-sm text-gray-600 transition hover:bg-gray-50 hover:text-[var(--island-primary)];
}

.nav-link-active {
  @apply bg-[#e8f7ef] font-medium text-[var(--island-primary)];
}

.user-chip {
  @apply items-center gap-2 rounded-full border border-gray-100 bg-gray-50/80 px-3 py-1;
}

.user-avatar {
  @apply flex h-6 w-6 items-center justify-center rounded-full bg-[var(--island-primary)] text-xs font-medium text-white;
}

.user-name {
  @apply max-w-[8rem] truncate text-sm text-gray-700;
}

.vip-badge {
  @apply rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-semibold text-amber-700;
}
</style>
