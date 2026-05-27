import api from './api'

export const checkInService = {
  checkIn(bookingId: number) {
    return api.post<any>('/checkin', { bookingId })
  },

  checkOut(bookingId: number) {
    return api.post<any>('/checkin/checkout', { bookingId })
  }
}
