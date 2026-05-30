import type { ApiResponse } from '~/types/api'

export function useApi() {
  const config = useRuntimeConfig()
  const auth = useAuthStore()

  async function request<T>(path: string, options: Parameters<typeof $fetch>[1] = {}): Promise<T> {
    const headers: Record<string, string> = {
      ...(options.headers as Record<string, string> | undefined)
    }
    if (auth.token) {
      headers.Authorization = `Bearer ${auth.token}`
    }
    const res = await $fetch<ApiResponse<T>>(`${config.public.apiBase}${path}`, {
      ...options,
      headers
    })
    if (res.code !== 0) {
      throw new Error(res.message || '请求失败')
    }
    return res.data as T
  }

  return { request, apiBase: config.public.apiBase }
}
