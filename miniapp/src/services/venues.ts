import api from './api'

export interface VenueVO {
  id: number
  name: string
  address: string
  contactPerson: string
  phone: string
  description: string
  status: string
}

export const venueService = {
  list() {
    return api.get<VenueVO[]>('/venues')
  }
}
