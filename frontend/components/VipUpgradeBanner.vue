<template>
  <div class="rounded-lg border border-amber-200 bg-amber-50 p-4">
    <p class="mb-1 font-medium text-amber-900">{{ title }}</p>
    <p class="mb-3 text-sm text-amber-800">{{ message }}</p>
    <div class="flex flex-wrap gap-2">
      <NButton v-if="backTo" size="small" @click="navigateTo(backTo)">返回</NButton>
      <NButton size="small" quaternary @click="emit('dismiss')">知道了</NButton>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    message?: string
    backTo?: string
  }>(),
  {
    title: 'VIP 专属内容',
    message: '升级 VIP 后可使用高级技巧、更多模拟题与专属视频。支付功能即将上线，敬请期待。',
    backTo: '/'
  }
)

const emit = defineEmits<{ dismiss: [] }>()
</script>
