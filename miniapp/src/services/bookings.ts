import api from './api'

export interface CreateBookingParams {
  roomId: number
  bookingDate: string
  startTime: string
  endTime: string
  title: string
  attendeeCount?: number
  remark?: string
  recurring?: {
    type: string
    daysOfWeek: number[]
    endDate: string
  }
}

export const bookingService = {
  preCheck(roomId: number, date: string, startTime: string, endTime: string) {
    return api.post<any>('/bookings/pre-check', { roomId, bookingDate: date, startTime, endTime })
  },

  create(params: CreateBookingParams) {
    return api.post<any>('/bookings', params)
  },

  cancel(id: number, reason?: string) {
    return api.post<any>(`/bookings/${id}/cancel`, { reason })
  },

  getMyBookings(params: { status?: string; page?: number; size?: number }) {
    return api.get<any>('/bookings', params)
  },

  getDetail(id: number) {
    return api.get<any>(`/bookings/${id}`)
  }
}
