import http from './api'

export interface VenueItem {
  id: number
  name: string
  address: string
  status: string
  roomCount: number
}

export const venueApi = {
  list() {
    return http.get<{ code: number; data: VenueItem[] }>('/venues')
  }
}
