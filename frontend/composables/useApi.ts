import type { ApiResponse } from '~/types/api'
import type { FetchError } from 'ofetch'

function extractErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    const fetchError = error as FetchError<ApiResponse<unknown>>
    if (fetchError.data?.message) return fetchError.data.message
    return error.message
  }
  return '请求失败'
}

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
    try {
      const res = await $fetch<ApiResponse<T>>(`${config.public.apiBase}${path}`, {
        ...options,
        headers
      })
      if (res.code !== 0) {
        throw new Error(res.message || '请求失败')
      }
      return res.data as T
    } catch (error: unknown) {
      throw new Error(extractErrorMessage(error))
    }
  }

  async function uploadForm<T>(path: string, formData: FormData, timeoutMs = 1_200_000): Promise<T> {
    const headers: Record<string, string> = {}
    if (auth.token) {
      headers.Authorization = `Bearer ${auth.token}`
    }
    try {
      const res = await $fetch<ApiResponse<T>>(`${config.public.apiBase}${path}`, {
        method: 'POST',
        body: formData,
        headers,
        timeout: timeoutMs
      })
      if (res.code !== 0) {
        throw new Error(res.message || '请求失败')
      }
      return res.data as T
    } catch (error: unknown) {
      throw new Error(extractErrorMessage(error))
    }
  }

  return { request, uploadForm, apiBase: config.public.apiBase }
}
