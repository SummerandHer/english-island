export default defineNuxtRouteMiddleware((to) => {
  // Admin 页为客户端鉴权：SSR 时无 localStorage，勿在此误判未登录
  if (import.meta.server) {
    return
  }

  const auth = useAuthStore()
  auth.hydrate()

  if (!auth.isLoggedIn) {
    return navigateTo({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  }

  if (!auth.isAdmin) {
    // 已登录但非管理员：回首页（不是登录页）
    return navigateTo('/')
  }
})
