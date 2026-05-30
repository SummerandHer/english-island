<template>
  <div class="mx-auto flex min-h-[80vh] max-w-4xl items-center justify-center">
    <div class="island-card grid w-full overflow-hidden md:grid-cols-5">
      <div class="hidden bg-gradient-to-br from-green-50 to-white p-8 md:col-span-2 md:block">
        <h2 class="mb-6 text-xl font-bold text-[var(--island-primary)]">ISLAND</h2>
        <ul class="space-y-4 text-sm text-gray-600">
          <li v-for="item in features" :key="item.title">
            <strong class="text-gray-800">{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </li>
        </ul>
      </div>
      <div class="p-8 md:col-span-3">
        <h1 class="mb-6 text-xl font-bold">{{ isRegister ? '注册账号' : '邮箱登录' }}</h1>
        <NForm @submit.prevent="submit">
          <NFormItem label="邮箱">
            <NInput v-model:value="email" placeholder="you@example.com" />
          </NFormItem>
          <NFormItem label="密码">
            <NInput v-model:value="password" type="password" placeholder="至少 6 位" />
          </NFormItem>
          <NFormItem v-if="isRegister" label="昵称">
            <NInput v-model:value="nickname" placeholder="岛民" />
          </NFormItem>
          <NButton type="primary" attr-type="submit" block :loading="loading">
            {{ isRegister ? '注册' : '登录' }}
          </NButton>
        </NForm>
        <p class="mt-4 text-center text-sm text-gray-500">
          <button type="button" class="text-[var(--island-primary)]" @click="isRegister = !isRegister">
            {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
          </button>
        </p>
        <p class="mt-2 text-center text-xs text-gray-400">微信扫码登录 UI 二期接入，MVP 使用邮箱</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AuthData } from '~/types/api'

definePageMeta({ layout: false })

const auth = useAuthStore()
const router = useRouter()
const { request } = useApi()

const email = ref('')
const password = ref('')
const nickname = ref('')
const isRegister = ref(false)
const loading = ref(false)

const features = [
  { title: '结构化技巧', desc: '阅读翻译章节式学习' },
  { title: '双语精听', desc: '逐句同步，高效练听力' },
  { title: 'AI 翻译批改', desc: '即时反馈，提升表达' }
]

onMounted(() => {
  auth.hydrate()
  if (auth.isLoggedIn) router.replace('/')
})

async function submit() {
  loading.value = true
  try {
    const path = isRegister.value ? '/api/v1/auth/register' : '/api/v1/auth/login'
    const body = isRegister.value
      ? { email: email.value, password: password.value, nickname: nickname.value }
      : { email: email.value, password: password.value }
    const data = await request<AuthData>(path, { method: 'POST', body })
    auth.setAuth(data)
    router.push('/')
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>
