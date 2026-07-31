<template>
  <div class="video-study-header">
    <NuxtLink to="/video" class="back-btn" title="返回双语列表">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path
          d="M14.5 5.5 8 12l6.5 6.5"
          stroke="currentColor"
          stroke-width="1.9"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </NuxtLink>
    <h1>{{ title }}</h1>
    <button
      type="button"
      class="fav-btn"
      :class="{ 'fav-btn--on': favorited }"
      @click="$emit('favorite')"
    >
      <svg width="15" height="15" viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M12 20.5s-6.5-4.1-9-7.8C1.2 10.2 2.1 7 5 6.2c1.8-.5 3.5.3 4.5 1.7C10.5 6.5 12.2 5.7 14 6.2c2.9.8 3.8 4 2 6.5-2.5 3.7-4 7.8-4 7.8z"
          :fill="favorited ? 'currentColor' : 'none'"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linejoin="round"
        />
      </svg>
      <span>{{ favorited ? '已收藏' : '收藏' }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; favorited: boolean }>()
defineEmits<{ favorite: [] }>()
</script>

<style scoped>
.video-study-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  border: 1px solid rgba(47, 67, 51, 0.08);
  border-radius: 14px;
  background: #fff;
  padding: 0.55rem 0.85rem;
}

.back-btn {
  display: inline-flex;
  height: 2.15rem;
  width: 2.15rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  background: linear-gradient(160deg, #f4f7f2, #e8efe4);
  color: #2f4333;
  box-shadow: inset 0 0 0 1px rgba(59, 83, 62, 0.1);
  transition:
    transform 0.18s ease,
    background 0.18s ease,
    color 0.18s ease;
}

.back-btn:hover {
  transform: translateX(-1px);
  background: linear-gradient(160deg, #eaf1e6, #dce8d6);
  color: #1f2a24;
}

h1 {
  margin: 0;
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 700;
  color: #1f2a24;
}

.fav-btn {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 0.35rem;
  border: 1px solid rgba(47, 67, 51, 0.1);
  border-radius: 999px;
  background: #fff;
  padding: 0.35rem 0.75rem;
  font-size: 0.75rem;
  color: #6b7a6f;
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease,
    border-color 0.15s ease;
}

.fav-btn:hover {
  border-color: rgba(59, 83, 62, 0.22);
  background: #f5f8f3;
  color: #3b533e;
}

.fav-btn--on {
  border-color: rgba(196, 92, 92, 0.25);
  background: #fff5f5;
  color: #c45c5c;
}

@media (min-width: 768px) {
  h1 {
    font-size: 1.02rem;
  }
}
</style>
