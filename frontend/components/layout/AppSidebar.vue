<template>
  <aside class="sidebar" :class="{ 'sidebar--open': open }">
    <div class="brand">
      <img class="brand-logo" src="/home/logo.png" width="44" height="44" alt="" />
      <div>
        <p class="brand-en">ISLAND</p>
        <p class="brand-zh">四六级岛</p>
        <p class="brand-sub">CET ISLAND</p>
      </div>
    </div>

    <nav class="nav">
      <NuxtLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="nav-item"
        :class="{ active: isActive(item.to) }"
        @click="$emit('navigate')"
      >
        <span class="nav-ico" aria-hidden="true" v-html="item.icon" />
        <span>{{ item.label }}</span>
      </NuxtLink>
    </nav>

    <div class="foot">
      <div class="palm" aria-hidden="true">
        <svg viewBox="0 0 80 48" width="80" height="48" fill="none">
          <ellipse cx="40" cy="40" rx="22" ry="5" fill="rgba(209,224,201,0.18)" />
          <path d="M40 34c-1-8-8-14-8-20 4 3 7 8 8 14 1-6 4-11 8-14 0 6-7 12-8 20Z" fill="rgba(209,224,201,0.35)" />
          <path d="M40 34v8" stroke="rgba(209,224,201,0.45)" stroke-width="2" stroke-linecap="round" />
        </svg>
      </div>
      <p class="quote-zh">每一次努力<br />都是登岛的脚印</p>
      <p class="quote-en">Every effort is a footprint to the island.</p>
      <div class="user">
        <img class="user-av" src="/home/avatar.png" width="36" height="36" alt="" />
        <div class="user-meta">
          <p class="user-name">{{ displayName }}</p>
          <p class="user-lv">Lv.12</p>
          <div class="xp"><i style="width: 29%" /></div>
          <p class="xp-num">260/900</p>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
defineProps<{ open?: boolean }>()
defineEmits<{ navigate: [] }>()

const auth = useAuthStore()
const route = useRoute()
auth.hydrate()

const icon = {
  home: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1v-9.5Z"/></svg>`,
  aa: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M5 17 10 5h1l5 12M6.8 13h7.4"/><path d="M16 17c1.2-3 2.4-4.5 4-4.5"/></svg>`,
  ear: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 12v-1a8 8 0 0 1 16 0v1"/><path d="M6 12v3a3 3 0 0 0 3 3h1"/><path d="M18 12v2a2 2 0 0 1-2 2h-1"/><circle cx="8.5" cy="18.5" r="1.5"/><circle cx="15.5" cy="16.5" r="1.5"/></svg>`,
  book: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M3 6.5C5.5 5 8.5 5 12 6.5S18.5 8 21 6.5V18c-2.5 1.5-5.5 1.5-9 0s-6.5-1.5-9 0V6.5Z"/></svg>`,
  pen: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M13.5 5.5 18.5 10.5"/><path d="M4 20l4.2-1.1L19 8.1a2.1 2.1 0 0 0 0-3L17 3a2.1 2.1 0 0 0-3 0L5.1 11.8 4 16v4Z"/></svg>`,
  bi: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.5 2.8 3.8 5.8 3.8 9s-1.3 6.2-3.8 9c-2.5-2.8-3.8-5.8-3.8-9S9.5 5.8 12 3Z"/></svg>`,
  tr: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 6h10M9 6v12"/><path d="M14 14h6M17 11l3 3-3 3"/></svg>`,
  exam: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M8 3h8a2 2 0 0 1 2 2v14l-6-2.5L6 19V5a2 2 0 0 1 2-2Z"/><path d="M9 8h6M9 12h4"/></svg>`,
  chart: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 19h16"/><path d="M7 16V9M12 16V5M17 16v-6"/></svg>`,
  me: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="8" r="3.5"/><path d="M5 19.5c1.8-3.2 4.2-4.5 7-4.5s5.2 1.3 7 4.5"/></svg>`,
  daily: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="4" y="5" width="16" height="15" rx="2"/><path d="M8 3v4M16 3v4M4 11h16"/><path d="M8 15h3M13 15h3" stroke-linecap="round"/></svg>`,
  admin: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M12 3 4.5 7v5c0 4.5 3.2 7.6 7.5 9 4.3-1.4 7.5-4.5 7.5-9V7L12 3Z"/><path d="M9.5 12.2 11.2 14l3.5-3.8" stroke-linecap="round" stroke-linejoin="round"/></svg>`
}

const navItems = computed(() => {
  const items = [
    { label: '首页', to: '/', icon: icon.home },
    { label: '日报岛', to: '/daily', icon: icon.daily },
    { label: '单词岛', to: '/islands/vocab', icon: icon.aa },
    { label: '听力岛', to: '/islands/listening', icon: icon.ear },
    { label: '阅读岛', to: '/islands/reading', icon: icon.book },
    { label: '写作岛', to: '/islands/writing', icon: icon.pen },
    { label: '双语岛', to: '/video', icon: icon.bi },
    { label: '翻译岛', to: '/islands/translation', icon: icon.tr },
    { label: '仿真题岛', to: '/islands/exam', icon: icon.exam },
    { label: '学习记录', to: '/islands/records', icon: icon.chart },
    { label: '我的岛站', to: '/me', icon: icon.me }
  ]
  if (auth.isAdmin) {
    items.push({ label: '管理后台', to: '/admin/daily', icon: icon.admin })
  }
  return items
})

function isActive(to: string) {
  if (to === '/') return route.path === '/'
  return route.path === to || route.path.startsWith(to + '/')
}

const displayName = computed(() => auth.user?.nickname || 'Islander')
</script>

<style scoped>
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 60;
  display: flex;
  width: var(--island-sidebar-w);
  flex-direction: column;
  background: var(--island-forest);
  color: #f3f6f1;
  padding: 1.1rem 0.75rem 0.9rem;
  transform: translateX(0);
  transition: transform 0.28s ease;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.2rem 0.45rem 1rem;
}

.brand-logo {
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 0 0 2px rgba(209, 224, 201, 0.35);
}

.brand-en {
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  line-height: 1;
}

.brand-zh {
  margin-top: 0.15rem;
  font-size: 0.82rem;
  font-weight: 500;
}

.brand-sub {
  margin-top: 0.1rem;
  font-size: 0.62rem;
  letter-spacing: 0.08em;
  opacity: 0.45;
}

.nav {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 0.15rem;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  border-radius: 14px;
  padding: 0.62rem 0.8rem;
  font-size: 0.9rem;
  color: rgba(243, 246, 241, 0.78);
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-item:hover {
  background: rgba(209, 224, 201, 0.1);
  color: #fff;
}

.nav-item.active {
  background: var(--island-sage);
  color: var(--island-forest);
  font-weight: 600;
}

.nav-ico {
  display: inline-flex;
  width: 18px;
}

.foot {
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  text-align: center;
}

.palm {
  display: flex;
  justify-content: center;
  opacity: 0.9;
}

.quote-zh {
  margin-top: 0.15rem;
  font-size: 0.78rem;
  line-height: 1.45;
  opacity: 0.78;
}

.quote-en {
  margin-top: 0.25rem;
  font-size: 0.62rem;
  opacity: 0.4;
  font-style: italic;
}

.user {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-top: 0.85rem;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.12);
  padding: 0.55rem 0.65rem;
  text-align: left;
}

.user-av {
  border-radius: 50%;
  object-fit: cover;
}

.user-name {
  font-size: 0.85rem;
  font-weight: 600;
}

.user-lv {
  font-size: 0.68rem;
  opacity: 0.55;
}

.xp {
  margin-top: 0.3rem;
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
}

.xp i {
  display: block;
  height: 100%;
  background: var(--island-sage);
}

.xp-num {
  margin-top: 0.15rem;
  font-size: 0.6rem;
  opacity: 0.45;
  text-align: right;
}

@media (max-width: 1023px) {
  .sidebar { transform: translateX(-105%); }
  .sidebar--open { transform: translateX(0); }
}
</style>
