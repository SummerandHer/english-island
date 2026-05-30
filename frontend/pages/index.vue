<template>
  <div>
    <section class="island-card mb-6 p-8 text-center">
      <h1 class="mb-2 text-3xl font-bold text-gray-800">四六级岛 ISLAND</h1>
      <p class="text-gray-500">阅读 · 翻译 · 双语视频 · 学习社区</p>
    </section>
    <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <NuxtLink v-for="m in modules" :key="m.to" :to="m.to" class="island-card block p-6 transition hover:shadow-md">
        <div class="mb-2 text-2xl">{{ m.icon }}</div>
        <h3 class="font-semibold">{{ m.title }}</h3>
        <p class="mt-1 text-sm text-gray-500">{{ m.desc }}</p>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
const modules = [
  { to: '/reading', icon: '📖', title: '阅读技巧', desc: '章节式方法论' },
  { to: '/translation', icon: '✍️', title: '翻译技巧', desc: '练习 + AI 批改' },
  { to: '/video', icon: '🎬', title: '双语视频', desc: '逐句精听学习' },
  { to: '/feed', icon: '🏝️', title: '全平台', desc: '学习心得交流' }
]
</script>
