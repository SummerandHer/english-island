<template>
  <div class="mb-4 flex flex-wrap items-center gap-2">
    <span class="text-sm text-gray-600">词书</span>
    <NButton
      v-for="level in levels"
      :key="level.value"
      :type="modelValue === level.value ? 'primary' : 'default'"
      size="small"
      :loading="saving"
      @click="select(level.value)"
    >
      {{ level.label }}
    </NButton>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  modelValue: string
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  change: [value: string]
}>()

const levels = [
  { value: 'cet4', label: '四级词书' },
  { value: 'cet6', label: '六级词书' }
]

function select(value: string) {
  if (value === props.modelValue) return
  emit('update:modelValue', value)
  emit('change', value)
}
</script>
