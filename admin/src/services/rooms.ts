import http from './api'

export interface RoomItem {
  id: number
  name: string
  venueId: number
  venueName: string
  capacity: number
  areaSqm: number
  floor: number
  pricePerHour: number
  pricePerHalfDay: number
  pricePerDay: number
  status: string
  description: string
  amenities: { id: number; name: string }[]
  images: string[]
  available: boolean
}

export interface RoomSearchParams {
  venueId?: number
  capacity?: number
  amenityIds?: number[]
  page?: number
  size?: number
}

export const roomApi = {
  list(params: RoomSearchParams) {
    return http.get<{ code: number; data: { page: number; size: number; total: number; records: RoomItem[] } }>('/rooms/search', params)
  },

  getById(id: number) {
    return http.get<{ code: number; data: RoomItem }>(`/rooms/${id}`)
  },

  create(data: any) {
    return http.post<any>('/admin/rooms', data)
  },

  update(id: number, data: any) {
    return http.put<any>(`/admin/rooms/${id}`, data)
  },

  delete(id: number) {
    return http.delete<any>(`/admin/rooms/${id}`)
  },

  toggleStatus(id: number, status: string) {
    return http.put<any>(`/admin/rooms/${id}/status`, { status })
  }
}
