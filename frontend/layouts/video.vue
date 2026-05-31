<template>
  <div class="video-layout bg-[var(--island-bg)] md:h-[100dvh] md:overflow-hidden">
    <LayoutVideoRevealNav />
    <main class="h-full w-full px-3 py-3 md:overflow-hidden md:px-5 md:py-3 lg:px-8">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
onMounted(() => useAuthStore().hydrate())
</script>
