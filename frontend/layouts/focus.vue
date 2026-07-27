<template>
  <div class="focus-shell">
    <slot />
  </div>
</template>

<!-- 日报沉浸模式已改用页面内 Teleport 全屏层；本 layout 保留备用 -->
<style scoped>
.focus-shell {
  min-height: 100vh;
  background: var(--island-bg);
  color: var(--island-text);
}
</style>
