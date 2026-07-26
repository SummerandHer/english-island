<template>
  <div>
    <h1 class="mb-2 text-xl font-bold">四六级备考导航</h1>
    <p class="mb-6 text-sm text-gray-600">
      本站练习题为<strong>自编模拟题</strong>，非历年真题。真题与报名请以教育部考试中心发布为准。
    </p>

    <section class="island-card mb-6 p-6">
      <h2 class="mb-3 font-semibold text-gray-800">官方入口</h2>
      <ul class="space-y-3 text-sm">
        <li v-for="link in officialLinks" :key="link.url">
          <a
            :href="link.url"
            target="_blank"
            rel="noopener noreferrer"
            class="text-[var(--island-primary)] hover:underline"
          >
            {{ link.title }}
          </a>
          <p class="mt-0.5 text-gray-500">{{ link.desc }}</p>
        </li>
      </ul>
    </section>

    <section class="island-card mb-6 p-6">
      <h2 class="mb-3 font-semibold text-gray-800">站内学习建议</h2>
      <ol class="list-decimal space-y-2 pl-5 text-sm text-gray-700">
        <li>阅读/翻译：先学技巧章节，再做同章模拟练习。</li>
        <li>词汇：登录后完成「每日 20 词」复习队列。</li>
        <li>双语视频：句轴精听，配合阅读/翻译主题巩固。</li>
      </ol>
      <div class="mt-4 flex flex-wrap gap-3">
        <NuxtLink to="/reading" class="text-sm text-[var(--island-primary)] hover:underline">阅读技巧 →</NuxtLink>
        <NuxtLink to="/translation" class="text-sm text-[var(--island-primary)] hover:underline">翻译练习 →</NuxtLink>
        <NuxtLink to="/vocabulary" class="text-sm text-[var(--island-primary)] hover:underline">每日词汇 →</NuxtLink>
      </div>
    </section>

    <section class="rounded-lg border border-gray-100 bg-gray-50 p-4 text-xs text-gray-500">
      ISLAND 不托管历年真题 PDF/听力文件，不批量爬取第三方题库。模拟题标注见各练习页「模拟题」标签。
    </section>
  </div>
</template>

<script setup lang="ts">
const officialLinks = [
  {
    title: '中国教育考试网 · 四六级考试（CET）',
    url: 'https://cet.neea.edu.cn/',
    desc: '报名、考试公告、成绩查询等权威信息。'
  },
  {
    title: '全国大学英语四、六级考试大纲',
    url: 'https://cet.neea.edu.cn/',
    desc: '考试范围与题型说明（请从官网导航进入大纲/政策栏目）。'
  }
]
</script>
