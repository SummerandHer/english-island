<template>
  <div class="video-reveal-nav">
    <!-- 折叠态：顶部点击展开 -->
    <button
      v-if="!expanded"
      type="button"
      class="video-reveal-nav__trigger"
      aria-label="展开导航"
      @click="open"
    >
      <span class="video-reveal-nav__trigger-icon">☰</span>
      <span>导航</span>
      <span class="video-reveal-nav__trigger-chevron">▼</span>
    </button>

    <!-- 展开态：顶栏 + 收起按钮 -->
    <div
      class="video-reveal-nav__panel"
      :class="{ 'video-reveal-nav__panel--open': expanded }"
    >
      <div class="video-reveal-nav__toolbar">
        <span class="video-reveal-nav__hint">ISLAND 导航</span>
        <button
          type="button"
          class="video-reveal-nav__collapse"
          aria-label="收起导航"
          @click="close"
        >
          <span>收起</span>
          <span class="video-reveal-nav__collapse-icon">▲</span>
        </button>
      </div>
      <LayoutAppHeader immersive />
    </div>

    <!-- 展开时点击遮罩也可收起 -->
    <div
      v-if="expanded"
      class="video-reveal-nav__backdrop"
      aria-hidden="true"
      @click="close"
    />
  </div>
</template>

<script setup lang="ts">
const expanded = ref(false)

function open() {
  expanded.value = true
}

function close() {
  expanded.value = false
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && expanded.value) {
    close()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.video-reveal-nav {
  @apply pointer-events-none fixed left-0 right-0 top-0 z-[100];
}

.video-reveal-nav__trigger {
  @apply pointer-events-auto absolute left-1/2 top-0 z-[101] flex -translate-x-1/2 items-center gap-1.5 rounded-b-xl border border-t-0 border-gray-200/90 bg-white/95 px-4 py-1.5 text-xs text-gray-600 shadow-sm backdrop-blur-sm transition;
}

.video-reveal-nav__trigger:hover {
  border-color: #18a05866;
  color: var(--island-primary);
}

.video-reveal-nav__trigger-icon {
  @apply text-sm leading-none;
}

.video-reveal-nav__trigger-chevron {
  @apply text-[10px] text-gray-400;
}

.video-reveal-nav__panel {
  @apply pointer-events-auto absolute left-0 right-0 top-0 z-[100] -translate-y-full opacity-0 transition-all duration-300 ease-out;
}

.video-reveal-nav__panel--open {
  @apply translate-y-0 opacity-100;
}

.video-reveal-nav__toolbar {
  @apply flex items-center justify-between border-b border-gray-100 bg-gray-50/90 px-4 py-1;
}

.video-reveal-nav__hint {
  @apply text-[11px] text-gray-400;
}

.video-reveal-nav__collapse {
  @apply inline-flex items-center gap-1 rounded-lg px-2.5 py-1 text-xs text-gray-600 transition;
}

.video-reveal-nav__collapse:hover {
  @apply bg-white;
  color: var(--island-primary);
}

.video-reveal-nav__collapse-icon {
  @apply text-[10px];
}

.video-reveal-nav__backdrop {
  @apply pointer-events-auto fixed inset-0 z-[99] bg-black/10;
}
</style>
