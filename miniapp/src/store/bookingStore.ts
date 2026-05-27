import { create } from 'zustand'

interface BookingState {
  selectedRoom: any | null
  selectedDate: string
  selectedStartTime: string
  selectedEndTime: string
  setSelectedRoom: (room: any) => void
  setSelectedDate: (date: string) => void
  setSelectedTime: (start: string, end: string) => void
  clear: () => void
}

export const useBookingStore = create<BookingState>((set) => ({
  selectedRoom: null,
  selectedDate: '',
  selectedStartTime: '',
  selectedEndTime: '',
  setSelectedRoom: (room) => set({ selectedRoom: room }),
  setSelectedDate: (date) => set({ selectedDate: date }),
  setSelectedTime: (start, end) => set({ selectedStartTime: start, selectedEndTime: end }),
  clear: () => set({ selectedRoom: null, selectedDate: '', selectedStartTime: '', selectedEndTime: '' })
}))
