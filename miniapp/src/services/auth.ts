import Taro from '@tarojs/taro'
import api from './api'

export interface UserInfo {
  id: number
  nickname: string
  avatarUrl: string
  phone: string
}

export const authService = {
  async login(): Promise<string> {
    try {
      const systemInfo = Taro.getSystemInfoSync()
      // In WeChat DevTools simulator, use dev-login to bypass real WeChat auth
      if (systemInfo.platform === 'devtools') {
        const res = await api.post<{ token: string; userInfo: UserInfo }>('/auth/dev-login', {
          openid: 'dev_simulator_user'
        })
        if (res.code === 0) {
          Taro.setStorageSync('token', res.data.token)
          Taro.setStorageSync('user', res.data.userInfo)
          return res.data.token
        }
        throw new Error(res.message || '开发登录失败')
      }

      const { code } = await Taro.login()
      const res = await api.post<{ token: string; userInfo: UserInfo }>('/auth/login', { code })
      if (res.code === 0) {
        Taro.setStorageSync('token', res.data.token)
        Taro.setStorageSync('user', res.data.userInfo)
        return res.data.token
      }
      throw new Error(res.message || '登录失败')
    } catch (err: any) {
      throw new Error(err.message || '登录失败')
    }
  },

  async getProfile(): Promise<UserInfo> {
    const res = await api.get<UserInfo>('/auth/profile')
    return res.data
  },

  async updateProfile(data: Partial<UserInfo>): Promise<void> {
    await api.put('/auth/profile', data)
  }
}
