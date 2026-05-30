export default defineNuxtRouteMiddleware(() => {
  const auth = useAuthStore()
  auth.hydrate()
  if (!auth.isLoggedIn) {
    return navigateTo('/login')
  }
  if (!auth.isAdmin) {
    return navigateTo('/')
  }
})
