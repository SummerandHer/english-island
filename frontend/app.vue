<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <NMessageProvider>
      <NuxtLayout>
        <NuxtPage />
      </NuxtLayout>
    </NMessageProvider>
  </NConfigProvider>
</template>

<script setup lang="ts">
import type { GlobalThemeOverrides } from 'naive-ui'

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#18a058',
    primaryColorHover: '#36ad6a',
    primaryColorPressed: '#0c7a43',
    borderRadius: '8px',
    fontFamily: "-apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif"
  }
}
</script>
