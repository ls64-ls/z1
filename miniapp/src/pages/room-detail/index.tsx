import { useState, useEffect, useMemo } from 'react'
import { View, Text, Image, ScrollView, Button } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { roomService } from '../../services/rooms'
import { bookingService } from '../../services/bookings'
import { useBookingStore } from '../../store/bookingStore'
import Calendar from '../../components/Calendar'
import TimeSlotPicker from '../../components/TimeSlotPicker'
import './index.scss'

interface RoomDetail {
  id: number
  name: string
  venueName: string
  capacity: number
  areaSqm: number
  pricePerHour: number
  amenities: { id: number; name: string; icon: string }[]
  images: string[]
  description: string
  floor: number
}

interface TimeSlot {
  start: string
  end: string
  available: boolean
}

function RoomDetailPage() {
  const router = useRouter()
  const { id } = router.params
  const [room, setRoom] = useState<RoomDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [selectedDate, setSelectedDate] = useState('')
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<TimeSlot | null>(null)
  const [prechecking, setPrechecking] = useState(false)
  const { setSelectedRoom, setSelectedDate: setStoreDate, setSelectedTime } = useBookingStore()

  const today = useMemo(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }, [])

  useEffect(() => {
    if (id) fetchRoomDetail()
  }, [id])

  useEffect(() => {
    if (selectedDate && id) fetchTimeSlots()
  }, [selectedDate])

  const fetchRoomDetail = async () => {
    setLoading(true)
    try {
      const res = await roomService.getDetail(Number(id))
      if (res.code === 0 && res.data) {
        setRoom(res.data)
      }
    } catch (err) {
      console.error('Failed to load room detail', err)
      Taro.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }

  const fetchTimeSlots = async () => {
    try {
      const res = await roomService.getAvailability(Number(id), selectedDate)
      if (res.code === 0 && res.data) {
        setTimeSlots(res.data.slots || [])
      }
    } catch (err) {
      // Fallback: generate default slots
      setTimeSlots(generateDefaultSlots())
    }
  }

  const generateDefaultSlots = (): TimeSlot[] => {
    const slots: TimeSlot[] = []
    for (let h = 8; h < 20; h++) {
      const start = `${String(h).padStart(2, '0')}:00`
      const end = `${String(h + 1).padStart(2, '0')}:00`
      slots.push({ start, end, available: Math.random() > 0.3 })
    }
    return slots
  }

  const handleDateChange = (date: string) => {
    setSelectedDate(date)
    setSelectedSlot(null)
  }

  const handleSlotSelect = (start: string, end: string) => {
    const slot = timeSlots.find(s => s.start === start && s.end === end)
    if (slot) setSelectedSlot(slot)
  }

  const handleBooking = async () => {
    if (!selectedSlot) {
      Taro.showToast({ title: '请选择时间段', icon: 'none' })
      return
    }

    // Pre-check before navigating
    setPrechecking(true)
    try {
      const res = await bookingService.preCheck(
        Number(id), selectedDate, selectedSlot.start, selectedSlot.end
      )
      if (res.code === 0) {
        const result = res.data
        if (!result.available) {
          const suggestion = result.suggestion
          if (suggestion) {
            Taro.showModal({
              title: '时段冲突',
              content: `该时段已被预订。建议选择: ${suggestion.availableStart} - ${suggestion.availableEnd}`,
              showCancel: false
            })
          } else {
            Taro.showToast({ title: '该时段已被预订', icon: 'none' })
          }
          return
        }
      }
    } catch (err) {
      console.error('Pre-check failed', err)
    } finally {
      setPrechecking(false)
    }

    setSelectedRoom(room)
    setStoreDate(selectedDate || today)
    setSelectedTime(selectedSlot.start, selectedSlot.end)
    Taro.navigateTo({ url: '/pages/booking/index' })
  }

  if (loading) {
    return (
      <View className='detail-page'>
        <View className='loading-container'><Text>加载中...</Text></View>
      </View>
    )
  }

  if (!room) {
    return (
      <View className='detail-page'>
        <View className='error-container'>
          <Text className='error-text'>会议室不存在</Text>
          <Button className='back-btn' onClick={() => Taro.navigateBack()}>返回</Button>
        </View>
      </View>
    )
  }

  const effectiveDate = selectedDate || today

  return (
    <View className='detail-page'>
      <ScrollView scrollY className='detail-scroll'>
        <View className='image-gallery'>
          <View className='main-image'>
            {room.images && room.images.length > 0 ? (
              <Image src={room.images[0]} mode='aspectFill' className='room-image' />
            ) : (
              <View className='room-image-placeholder'>
                <Text className='placeholder-text'>{room.name.charAt(0)}</Text>
              </View>
            )}
          </View>
        </View>

        <View className='room-basic-info'>
          <Text className='room-title'>{room.name}</Text>
          <Text className='room-venue'>{room.venueName}{room.floor ? ` · ${room.floor}F` : ''}</Text>
          <View className='room-tags'>
            <View className='info-tag'><Text className='tag-text'>容纳 {room.capacity} 人</Text></View>
            {room.areaSqm && <View className='info-tag'><Text className='tag-text'>{room.areaSqm} m²</Text></View>}
            <View className='info-tag price-tag'><Text className='tag-text'>¥{room.pricePerHour}/时</Text></View>
          </View>
        </View>

        {room.amenities && room.amenities.length > 0 && (
          <View className='amenities-section'>
            <Text className='section-title'>配套设施</Text>
            <View className='amenities-grid'>
              {room.amenities.map((a) => (
                <View key={a.id} className='amenity-item'>
                  <Text className='amenity-name'>{a.name}</Text>
                </View>
              ))}
            </View>
          </View>
        )}

        <Calendar
          value={effectiveDate}
          onChange={handleDateChange}
          minDate={today}
        />

        <TimeSlotPicker
          slots={timeSlots}
          selectedStart={selectedSlot?.start}
          selectedEnd={selectedSlot?.end}
          onSelect={handleSlotSelect}
        />

        {room.description && (
          <View className='desc-section'>
            <Text className='section-title'>房间描述</Text>
            <Text className='desc-text'>{room.description}</Text>
          </View>
        )}
        <View className='bottom-spacer' />
      </ScrollView>

      <View className='bottom-bar'>
        <View className='price-summary'>
          <Text className='total-price'>
            <Text className='price-number'>¥{room.pricePerHour}</Text>
          </Text>
          <Text className='price-unit'>/小时</Text>
        </View>
        <Button
          className='booking-btn'
          onClick={handleBooking}
          loading={prechecking}
          disabled={!selectedSlot || prechecking}
        >
          {prechecking ? '检测中...' : selectedSlot ? `预订 ${selectedSlot.start}-${selectedSlot.end}` : '请选择时段'}
        </Button>
      </View>
    </View>
  )
}

export default RoomDetailPage
