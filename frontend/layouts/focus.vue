<template>
  <div class="focus-shell">
    <slot />
  </div>
</template>

<style scoped>
.focus-shell {
  min-height: 100vh;
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%, rgba(194, 214, 190, 0.45), transparent),
    var(--island-bg);
  color: var(--island-text);
}
</style>
