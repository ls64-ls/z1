import Taro from '@tarojs/taro'

export const wechatUtil = {
  async login() {
    return Taro.login()
  },

  async getUserProfile() {
    return Taro.getUserProfile({ desc: '用于完善个人资料' })
  },

  async requestPayment(params: { timeStamp: string; nonceStr: string; package: string; signType: string; paySign: string }) {
    return Taro.requestPayment(params)
  },

  async scanCode() {
    return Taro.scanCode({ onlyFromCamera: true })
  },

  async getLocation() {
    return Taro.getLocation({ type: 'gcj02' })
  }
}
