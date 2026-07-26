<template>
  <div class="app-shell">
    <div v-if="sidebarOpen" class="backdrop lg:hidden" @click="sidebarOpen = false" />
    <LayoutAppSidebar :open="sidebarOpen" @navigate="sidebarOpen = false" />
    <div class="app-main">
      <header class="topbar">
        <button class="menu lg:hidden" type="button" aria-label="打开菜单" @click="sidebarOpen = true">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M4 7h16M4 12h16M4 17h16" stroke-linecap="round" />
          </svg>
        </button>
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
      <main class="content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const sidebarOpen = ref(false)
onMounted(() => auth.hydrate())
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

@media (min-width: 768px) {
  .content { padding: 0 1.4rem 2rem; }
}

@media (min-width: 1024px) {
  .content { padding: 0 1.6rem 2rem 1.4rem; }
}
</style>
