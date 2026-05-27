import api from './api'

export interface RoomVO {
  id: number
  name: string
  venueName: string
  capacity: number
  areaSqm: number
  pricePerHour: number
  amenities: { id: number; name: string; icon: string }[]
  coverImage: string
  available: boolean
  remainingSlots: number
}

export interface RoomSearchParams {
  venueId?: number
  date?: string
  startTime?: string
  endTime?: string
  capacity?: number
  amenityIds?: string
  page?: number
  size?: number
}

export const roomService = {
  search(params: RoomSearchParams) {
    return api.get<{ page: number; size: number; total: number; records: RoomVO[] }>('/rooms', params)
  },

  getDetail(id: number) {
    return api.get<any>(`/rooms/${id}`)
  },

  getAvailability(id: number, date: string) {
    return api.get<any>(`/rooms/${id}/availability`, { date })
  },

  getCalendar(id: number, year: number, month: number) {
    return api.get<any>(`/rooms/${id}/calendar`, { year, month })
  }
}
