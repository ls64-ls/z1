import http from './api'

export interface BookingItem {
  id: number
  bookingNo: string
  roomId: number
  roomName: string
  venueName: string
  userId: number
  bookingDate: string
  startTime: string
  endTime: string
  title: string
  attendeeCount: number
  status: string
  totalAmount: number
  createdAt: string
}

export const bookingApi = {
  list(params: { venueId?: number; roomId?: number; status?: string; date?: string; page?: number; size?: number }) {
    return http.get<{ code: number; data: { page: number; size: number; total: number; records: BookingItem[] } }>('/admin/bookings', params)
  },

  getById(id: number) {
    return http.get<{ code: number; data: BookingItem }>(`/bookings/${id}`)
  },

  cancel(id: number, reason?: string) {
    return http.post<any>(`/admin/bookings/${id}/cancel`, { reason })
  },

  updateStatus(id: number, status: string) {
    return http.put<any>(`/admin/bookings/${id}/status`, { status })
  }
}
