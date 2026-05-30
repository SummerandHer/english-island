<template>
  <div>
    <h1 class="mb-4 text-xl font-bold">全平台</h1>
    <div class="mb-6 island-card p-4" v-if="auth.isLoggedIn">
      <NInput v-model:value="content" type="textarea" placeholder="分享学习心得..." :rows="3" />
      <div class="mt-3 flex flex-wrap gap-2">
        <input ref="fileInput" type="file" accept="image/*" multiple class="hidden" @change="onFiles" />
        <NButton size="small" @click="fileInput?.click()">添加图片</NButton>
        <NButton size="small" type="primary" :loading="posting" @click="submitPost">发布</NButton>
      </div>
      <div v-if="previewUrls.length" class="mt-3 flex flex-wrap gap-2">
        <img v-for="(u, i) in previewUrls" :key="i" :src="u" class="h-20 w-20 rounded-lg object-cover" />
      </div>
    </div>
    <div v-else class="mb-4 text-sm text-gray-500">
      <NuxtLink to="/login" class="text-[var(--island-primary)]">登录</NuxtLink> 后发帖
    </div>
    <div class="space-y-4">
      <article v-for="post in posts" :key="post.id" class="island-card p-4">
        <div class="mb-2 flex items-center gap-2">
          <div class="flex h-8 w-8 items-center justify-center rounded-full bg-green-100 text-sm">🏝️</div>
          <span class="font-medium">{{ post.authorNickname }}</span>
        </div>
        <p class="whitespace-pre-wrap text-gray-700">{{ post.content }}</p>
        <div v-if="post.images?.length" class="mt-3 grid grid-cols-3 gap-2">
          <img v-for="(img, i) in post.images" :key="i" :src="apiBase + img" class="rounded-lg object-cover" />
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PageResult, PostItem } from '~/types/api'

const auth = useAuthStore()
const { request, apiBase } = useApi()

const posts = ref<PostItem[]>([])
const content = ref('')
const posting = ref(false)
const fileInput = ref<HTMLInputElement>()
const pendingFiles = ref<File[]>([])
const previewUrls = ref<string[]>([])

onMounted(async () => {
  auth.hydrate()
  await loadFeed()
})

async function loadFeed() {
  const data = await request<PageResult<PostItem>>('/api/v1/posts?page=1&size=20')
  posts.value = data.items
}

function onFiles(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files) return
  pendingFiles.value = Array.from(files)
  previewUrls.value = pendingFiles.value.map(f => URL.createObjectURL(f))
}

async function submitPost() {
  if (!content.value.trim()) return
  posting.value = true
  try {
    const fileIds: number[] = []
    for (const file of pendingFiles.value) {
      const form = new FormData()
      form.append('file', file)
      const uploaded = await request<{ fileId: number; url: string }>('/api/v1/posts/upload', {
        method: 'POST',
        body: form
      })
      fileIds.push(uploaded.fileId)
    }
    await request('/api/v1/posts', {
      method: 'POST',
      body: { content: content.value, fileIds }
    })
    content.value = ''
    pendingFiles.value = []
    previewUrls.value = []
    await loadFeed()
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '发布失败')
  } finally {
    posting.value = false
  }
}
</script>
