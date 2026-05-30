<template>
  <AuthShell>
    <div class="auth-form-header">
      <h1>创建账号</h1>
      <p>使用 QQ 邮箱注册，开启四六级学习之旅</p>
    </div>

    <NForm ref="formRef" :model="form" :rules="rules" @submit.prevent="submit">
      <NFormItem path="email" label="QQ 邮箱">
        <NInput v-model:value="form.email" placeholder="123456789@qq.com" size="large" />
      </NFormItem>
      <NFormItem path="code" label="邮箱验证码">
        <div class="flex w-full gap-3">
          <NInput v-model:value="form.code" maxlength="6" placeholder="6 位验证码" size="large" class="flex-1" />
          <NButton
            size="large"
            :disabled="cooldown > 0"
            :loading="sending"
            class="shrink-0"
            @click="sendCode(form.email)"
          >
            {{ cooldown > 0 ? `${cooldown}s` : '获取验证码' }}
          </NButton>
        </div>
      </NFormItem>
      <NFormItem path="password" label="密码">
        <NInput
          v-model:value="form.password"
          type="password"
          show-password-on="click"
          placeholder="至少 6 位"
          size="large"
        />
      </NFormItem>
      <NFormItem path="nickname" label="昵称">
        <NInput v-model:value="form.nickname" placeholder="岛民（可选）" size="large" />
      </NFormItem>
      <NButton type="primary" attr-type="submit" block size="large" :loading="loading">
        注册
      </NButton>
    </NForm>

    <p class="auth-switch">
      已有账号？
      <NuxtLink to="/login">去登录</NuxtLink>
    </p>
  </AuthShell>
</template>

<script setup lang="ts">
import type { FormInst, FormRules } from 'naive-ui'
import type { AuthData } from '~/types/api'

definePageMeta({ layout: false, ssr: false })

const auth = useAuthStore()
const router = useRouter()
const message = useAppMessage()
const { request } = useApi()
const { sending, cooldown, sendCode } = useVerificationCode('register')

const formRef = ref<FormInst | null>(null)
const loading = ref(false)

const form = reactive({
  email: '',
  code: '',
  password: '',
  nickname: ''
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入 QQ 邮箱', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._%+-]+@qq\.com$/i, message: '仅支持 @qq.com 邮箱', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为 6 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

onMounted(() => {
  auth.hydrate()
  if (auth.isLoggedIn) router.replace('/')
})

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const data = await request<AuthData>('/api/v1/auth/register', {
      method: 'POST',
      body: {
        email: form.email.trim(),
        code: form.code.trim(),
        password: form.password,
        nickname: form.nickname.trim() || undefined
      }
    })
    auth.setAuth(data)
    message.success('注册成功')
    router.push('/')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '注册失败')
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
