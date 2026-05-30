<template>
  <div class="min-h-screen">
    <LayoutAppHeader />
    <main class="mx-auto max-w-6xl px-4 py-6">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
onMounted(() => useAuthStore().hydrate())
</script>
