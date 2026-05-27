import { create } from 'zustand'

interface UserState {
  token: string
  user: { id: number; nickname: string; avatarUrl: string; phone: string } | null
  setToken: (token: string) => void
  setUser: (user: any) => void
  clear: () => void
}

export const useUserStore = create<UserState>((set) => ({
  token: '',
  user: null,
  setToken: (token) => set({ token }),
  setUser: (user) => set({ user }),
  clear: () => set({ token: '', user: null })
}))
