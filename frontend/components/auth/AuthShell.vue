<template>
  <div class="auth-page">
    <div class="auth-shell island-card">
      <aside class="auth-brand">
        <div class="auth-brand-inner">
          <NuxtLink to="/" class="auth-logo">ISLAND</NuxtLink>
          <p class="auth-tagline">四六级岛 · 大学生备考平台</p>
          <ul class="auth-features">
            <li v-for="item in features" :key="item.title">
              <span class="auth-feature-icon">{{ item.icon }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </div>
            </li>
          </ul>
          <div class="auth-wechat">
            <div class="auth-wechat-qr">
              <span class="text-3xl opacity-40">⌁</span>
            </div>
            <p class="text-sm font-medium text-gray-700">微信扫码登录</p>
            <p class="text-xs text-gray-400">即将上线，敬请期待</p>
          </div>
        </div>
      </aside>

      <section class="auth-form-panel">
        <slot />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
const features = [
  { icon: '📖', title: '结构化技巧', desc: '阅读翻译章节式学习' },
  { icon: '🎬', title: '双语岛', desc: '逐句同步，高效练听力' },
  { icon: '✍️', title: 'AI 翻译批改', desc: '即时反馈，提升表达' }
]
</script>

<style scoped>
.auth-page {
  @apply flex min-h-screen items-center justify-center bg-gradient-to-br from-[#eef9f3] via-[#f7f8fa] to-[#eef5ff] px-4 py-10;
}

.auth-shell {
  @apply grid w-full max-w-5xl overflow-hidden shadow-lg md:grid-cols-5;
}

.auth-brand {
  @apply hidden bg-gradient-to-br from-[#e8f7ef] via-white to-[#f0faf4] md:col-span-2 md:block;
}

.auth-brand-inner {
  @apply flex h-full flex-col p-8 lg:p-10;
}

.auth-logo {
  @apply text-2xl font-bold tracking-tight text-[var(--island-primary)];
}

.auth-tagline {
  @apply mt-2 text-sm text-gray-500;
}

.auth-features {
  @apply mt-8 flex-1 space-y-5;
}

.auth-features li {
  @apply flex gap-3 text-sm text-gray-600;
}

.auth-feature-icon {
  @apply flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white text-base shadow-sm;
}

.auth-features strong {
  @apply block text-gray-800;
}

.auth-wechat {
  @apply mt-6 rounded-xl border border-dashed border-gray-200 bg-white/70 p-4 text-center;
}

.auth-wechat-qr {
  @apply mx-auto mb-3 flex h-24 w-24 items-center justify-center rounded-lg bg-gray-50;
}

.auth-form-panel {
  @apply p-8 md:col-span-3 lg:p-10;
}
</style>
