<template>
  <AuthShell>
    <div class="auth-form-header">
      <h1>欢迎回来</h1>
      <p>使用 QQ 邮箱登录你的四六级岛账号</p>
    </div>

    <NForm ref="formRef" :model="form" :rules="rules" @submit.prevent="submit">
      <NFormItem path="email" label="QQ 邮箱">
        <NInput v-model:value="form.email" placeholder="123456789@qq.com" size="large" />
      </NFormItem>
      <NFormItem path="password" label="密码">
        <NInput
          v-model:value="form.password"
          type="password"
          show-password-on="click"
          placeholder="请输入密码"
          size="large"
        />
      </NFormItem>
      <div class="mb-4 flex justify-end">
        <NuxtLink to="/forgot-password" class="text-sm text-[var(--island-primary)] hover:underline">
          忘记密码？
        </NuxtLink>
      </div>
      <NButton type="primary" attr-type="submit" block size="large" :loading="loading">
        登录
      </NButton>
    </NForm>

    <p class="auth-switch">
      还没有账号？
      <NuxtLink to="/register">立即注册</NuxtLink>
    </p>
  </AuthShell>
</template>

<script setup lang="ts">
import type { FormInst, FormRules } from 'naive-ui'
import type { AuthData } from '~/types/api'

definePageMeta({ layout: false, ssr: false })

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const message = useAppMessage()
const { request } = useApi()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)

const form = reactive({
  email: '',
  password: ''
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入 QQ 邮箱', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._%+-]+@qq\.com$/i, message: '仅支持 @qq.com 邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

function postLoginPath() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }
  return '/'
}

onMounted(() => {
  auth.hydrate()
  if (auth.isLoggedIn) router.replace(postLoginPath())
})

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const data = await request<AuthData>('/api/v1/auth/login', {
      method: 'POST',
      body: { email: form.email.trim(), password: form.password }
    })
    auth.setAuth(data)
    message.success('登录成功')
    router.push(postLoginPath())
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-form-header {
  @apply mb-8;
}

.auth-form-header h1 {
  @apply text-2xl font-bold text-gray-800;
}

.auth-form-header p {
  @apply mt-2 text-sm text-gray-500;
}

.auth-switch {
  @apply mt-6 text-center text-sm text-gray-500;
}

.auth-switch a {
  @apply font-medium text-[var(--island-primary)] hover:underline;
}
</style>
