import { defineStore } from 'pinia'
import type { AuthData, UserProfile } from '~/types/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '' as string,
    user: null as UserProfile | null
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isVip: (s) => !!s.user?.vip
  },
  actions: {
    hydrate() {
      if (import.meta.client) {
        const token = localStorage.getItem('island_token')
        const user = localStorage.getItem('island_user')
        if (token) this.token = token
        if (user) this.user = JSON.parse(user)
      }
    },
    setAuth(data: AuthData) {
      this.token = data.token
      this.user = data.user
      if (import.meta.client) {
        localStorage.setItem('island_token', data.token)
        localStorage.setItem('island_user', JSON.stringify(data.user))
      }
    },
    logout() {
      this.token = ''
      this.user = null
      if (import.meta.client) {
        localStorage.removeItem('island_token')
        localStorage.removeItem('island_user')
      }
    }
  }
})
