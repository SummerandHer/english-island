<template>
  <div
    class="app-shell"
    :class="{
      'app-shell--reader': hideChromeSidebar && route.path !== '/video',
      'app-shell--video': route.path === '/video'
    }"
  >
    <div
      v-if="!hideChromeSidebar && sidebarOpen"
      class="backdrop lg:hidden"
      @click="sidebarOpen = false"
    />
    <LayoutAppSidebar
      v-if="!hideChromeSidebar"
      :open="sidebarOpen"
      @navigate="sidebarOpen = false"
    />
    <div class="app-main" :class="{ 'app-main--full': hideChromeSidebar }">
      <header class="topbar">
        <button
          v-if="!hideChromeSidebar"
          class="menu lg:hidden"
          type="button"
          aria-label="打开菜单"
          @click="sidebarOpen = true"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M4 7h16M4 12h16M4 17h16" stroke-linecap="round" />
          </svg>
        </button>
        <NuxtLink
          v-if="hideChromeSidebar"
          to="/"
          class="home-link"
        >
          ← 首页
        </NuxtLink>
        <div class="grow" />
        <label class="search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" stroke-linecap="round" />
          </svg>
          <input type="search" placeholder="搜索" disabled />
        </label>
        <button class="icon-btn" type="button" aria-label="通知">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M6 16V10a6 6 0 1 1 12 0v6l1.2 1.8H4.8L6 16Z" />
            <path d="M10 19a2 2 0 0 0 4 0" />
          </svg>
          <i class="dot" />
        </button>
        <template v-if="auth.isLoggedIn">
          <NuxtLink to="/me" class="av-link">
            <img class="av" src="/home/avatar.png" width="34" height="34" alt="" />
          </NuxtLink>
          <button class="text-btn" type="button" @click="logout">退出</button>
        </template>
        <template v-else>
          <button class="text-btn" type="button" @click="navigateTo('/login')">登录</button>
          <button class="cta" type="button" @click="navigateTo('/register')">注册</button>
        </template>
      </header>
      <main class="content" :class="{ 'content--reader': hideChromeSidebar }">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const route = useRoute()
const sidebarOpen = ref(false)

/** 日报正文 / 仿真题练习 / 双语列表：隐藏全局侧栏，给模块自有侧栏腾出宽度 */
const hideChromeSidebar = computed(() =>
  /^\/daily\/[^/]+$/.test(route.path)
  || /^\/islands\/exam\/practice\/[^/]+$/.test(route.path)
  || route.path === '/video'
)

onMounted(() => auth.hydrate())

watch(
  () => route.path,
  () => {
    sidebarOpen.value = false
  }
)

function logout() {
  auth.logout()
  navigateTo('/login')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--island-bg);
}

.app-shell--reader {
  background: #f4f5f2;
}

.app-shell--video {
  background:
    radial-gradient(ellipse 90% 42% at 50% -8%, rgba(209, 224, 201, 0.42), transparent 58%),
    linear-gradient(180deg, #f5f7f3 0%, #fafbf9 32%, #ffffff 72%);
}

.app-shell--video .content {
  padding-left: 1.1rem;
  padding-right: 1.1rem;
}

@media (min-width: 768px) {
  .app-shell--video .content {
    padding-left: 1.6rem;
    padding-right: 1.6rem;
  }
}

@media (min-width: 1024px) {
  .app-shell--video .content {
    padding-left: 2rem;
    padding-right: 2rem;
  }
}

.app-shell--video .home-link {
  font-family: var(--font-display);
  font-weight: 600;
  letter-spacing: 0.02em;
}

.backdrop {
  position: fixed;
  inset: 0;
  z-index: 55;
  background: rgba(30, 45, 35, 0.35);
}

.app-main {
  min-height: 100vh;
}

@media (min-width: 1024px) {
  .app-main { padding-left: var(--island-sidebar-w); }
  .app-main--full { padding-left: 0; }
}

.topbar {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  height: 3.4rem;
  padding: 0 1rem;
}

@media (min-width: 1024px) {
  .topbar { padding: 0 1.6rem 0 1.4rem; }
}

.menu,
.icon-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: var(--island-text);
  box-shadow: 0 2px 8px rgba(45, 71, 57, 0.06);
  cursor: pointer;
}

.dot {
  position: absolute;
  top: 0.35rem;
  right: 0.4rem;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e25b5b;
}

.grow { flex: 1; }

.home-link {
  color: var(--island-muted);
  font-size: 0.85rem;
  text-decoration: none;
}

.home-link:hover {
  color: var(--island-text);
}

.search {
  display: none;
  align-items: center;
  gap: 0.4rem;
  height: 2.2rem;
  min-width: 9.5rem;
  border-radius: 999px;
  background: #fff;
  padding: 0 0.85rem;
  color: var(--island-muted);
  box-shadow: 0 2px 8px rgba(45, 71, 57, 0.06);
}

.search input {
  width: 4.5rem;
  border: none;
  background: transparent;
  color: var(--island-text);
  font-size: 0.85rem;
  outline: none;
}

@media (min-width: 640px) {
  .search { display: inline-flex; }
}

.av {
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(45, 71, 57, 0.08);
}

.text-btn {
  border: none;
  background: transparent;
  color: var(--island-muted);
  font-size: 0.85rem;
  cursor: pointer;
}

.cta {
  border: none;
  border-radius: 999px;
  background: var(--island-forest);
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  padding: 0.4rem 0.9rem;
  cursor: pointer;
}

.content {
  padding: 0 1rem 1.5rem;
}

.content--reader {
  padding-bottom: 2rem;
}

@media (min-width: 768px) {
  .content { padding: 0 1.4rem 2rem; }
}

@media (min-width: 1024px) {
  .content { padding: 0 1.6rem 2rem 1.4rem; }
  .content--reader { padding: 0 1.75rem 2.5rem; }
}
</style>
