<template>
  <div class="video-layout md:h-[100dvh] md:overflow-hidden">
    <main class="h-full w-full px-3 py-3 md:overflow-hidden md:px-5 md:py-3 lg:px-7">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
onMounted(() => useAuthStore().hydrate())
</script>

<style scoped>
.video-layout {
  background:
    radial-gradient(ellipse 80% 36% at 30% -6%, rgba(209, 224, 201, 0.35), transparent 55%),
    linear-gradient(180deg, #f4f6f2 0%, #fafbf9 40%, #ffffff 100%);
  min-height: 100dvh;
}
</style>
