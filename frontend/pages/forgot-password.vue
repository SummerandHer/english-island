<template>
  <AuthShell>
    <div class="auth-form-header">
      <h1>重置密码</h1>
      <p>通过 QQ 邮箱验证码设置新密码</p>
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
      <NFormItem path="newPassword" label="新密码">
        <NInput
          v-model:value="form.newPassword"
          type="password"
          show-password-on="click"
          placeholder="至少 6 位"
          size="large"
        />
      </NFormItem>
      <NFormItem path="confirmPassword" label="确认密码">
        <NInput
          v-model:value="form.confirmPassword"
          type="password"
          show-password-on="click"
          placeholder="再次输入新密码"
          size="large"
        />
      </NFormItem>
      <NButton type="primary" attr-type="submit" block size="large" :loading="loading">
        确认重置
      </NButton>
    </NForm>

    <p class="auth-switch">
      想起密码了？
      <NuxtLink to="/login">返回登录</NuxtLink>
    </p>
  </AuthShell>
</template>

<script setup lang="ts">
import type { FormInst, FormRules } from 'naive-ui'

definePageMeta({ layout: false, ssr: false })

const router = useRouter()
const message = useAppMessage()
const { request } = useApi()
const { sending, cooldown, sendCode } = useVerificationCode('reset_password')

const formRef = ref<FormInst | null>(null)
const loading = ref(false)

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
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
  newPassword: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value: string) => value === form.newPassword,
      message: '两次密码不一致',
      trigger: 'blur'
    }
  ]
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await request<void>('/api/v1/auth/reset-password', {
      method: 'POST',
      body: {
        email: form.email.trim(),
        code: form.code.trim(),
        newPassword: form.newPassword
      }
    })
    message.success('密码重置成功，请登录')
    router.push('/login')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '重置失败')
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
