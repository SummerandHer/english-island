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
    primaryColor: '#4f6b52',
    primaryColorHover: '#5d7c60',
    primaryColorPressed: '#3b533e',
    borderRadius: '12px',
    fontFamily: "'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif"
  },
  Button: {
    borderRadiusMedium: '999px',
    borderRadiusLarge: '999px'
  }
}
</script>
