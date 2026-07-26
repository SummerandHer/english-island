<template>
  <div class="placeholder island-rise">
    <div class="placeholder-art" aria-hidden="true">
      <svg viewBox="0 0 120 80" width="120" height="80" fill="none">
        <ellipse cx="60" cy="62" rx="42" ry="10" fill="rgba(61,122,92,0.12)" />
        <path d="M28 58c8-18 18-28 32-28s24 10 32 28" stroke="var(--island-primary)" stroke-width="2" stroke-linecap="round" />
        <circle cx="60" cy="34" r="6" fill="var(--island-mint)" />
      </svg>
    </div>
    <h1>{{ title }}</h1>
    <p>{{ desc }}</p>
    <span class="badge">即将开放</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; desc: string }>()
</script>

<style scoped>
.placeholder {
  display: flex;
  min-height: 52vh;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 2rem 1rem;
}

.placeholder h1 {
  margin-top: 0.75rem;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--island-text);
}

.placeholder p {
  margin-top: 0.5rem;
  max-width: 22rem;
  color: var(--island-muted);
  font-size: 0.95rem;
  line-height: 1.6;
}

.badge {
  margin-top: 1.25rem;
  border-radius: 999px;
  background: var(--island-primary-light);
  color: var(--island-primary);
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.35rem 0.9rem;
}
</style>
