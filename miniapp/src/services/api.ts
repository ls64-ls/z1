import Taro from '@tarojs/taro'

// API_BASE_URL is injected by Taro defineConstants at build time
declare const process: { env: { API_BASE_URL: string } }
const BASE_URL: string = (typeof process !== 'undefined' && process.env && process.env.API_BASE_URL)
  || 'http://127.0.0.1:8088/api/v1'

const request = async <T>(options: {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: any
}): Promise<{ code: number; message: string; data: T }> => {
  const token = Taro.getStorageSync('token')
  const res = await Taro.request({
    url: BASE_URL + options.url,
    method: options.method || 'GET',
    data: options.data,
    header: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...options.header
    }
  })
  return res.data as any
}

export const api = {
  get: <T>(url: string, data?: any) => request<T>({ url, method: 'GET', data }),
  post: <T>(url: string, data?: any) => request<T>({ url, method: 'POST', data }),
  put: <T>(url: string, data?: any) => request<T>({ url, method: 'PUT', data }),
  delete: <T>(url: string, data?: any) => request<T>({ url, method: 'DELETE', data }),
}

export default api
