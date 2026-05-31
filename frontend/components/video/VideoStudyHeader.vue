<template>
  <div class="video-study-header island-card flex items-center gap-3 px-4 py-2.5">
    <NuxtLink
      to="/video"
      class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-gray-500 transition hover:bg-gray-50 hover:text-[var(--island-primary)]"
      title="返回双语列表"
    >
      ←
    </NuxtLink>
    <h1 class="min-w-0 flex-1 truncate text-sm font-semibold text-gray-800 md:text-base">
      {{ title }}
    </h1>
    <button
      type="button"
      class="flex shrink-0 items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs transition"
      :class="favorited ? 'bg-red-50 text-red-500' : 'text-gray-500 hover:bg-gray-50'"
      @click="$emit('favorite')"
    >
      <span>{{ favorited ? '❤' : '♡' }}</span>
      <span class="hidden sm:inline">{{ favorited ? '已收藏' : '收藏' }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; favorited: boolean }>()
defineEmits<{ favorite: [] }>()
</script>
