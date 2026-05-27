import { useState, useEffect } from 'react'
import { View, Text, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { bookingService } from '../../services/bookings'
import { BookingStatusMap, BookingStatusColor } from '../../constants/enums'
import './index.scss'

interface BookingItem {
  id: number
  roomName: string
  venueName: string
  bookingDate: string
  startTime: string
  endTime: string
  status: string
  title: string
}

const TABS = [
  { key: '', label: '全部' },
  { key: 'CONFIRMED', label: '待使用' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]

function MyBookingsPage() {
  const [activeTab, setActiveTab] = useState('')
  const [bookings, setBookings] = useState<BookingItem[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchBookings()
  }, [activeTab])

  const fetchBookings = async () => {
    setLoading(true)
    try {
      const res = await bookingService.getMyBookings({
        status: activeTab || undefined,
        page: 1,
        size: 50
      })
      setBookings(res.data.records || [])
    } catch (err) {
      console.error('获取预订列表失败', err)
    } finally {
      setLoading(false)
    }
  }

  const handleBookingClick = (booking: BookingItem) => {
    Taro.navigateTo({ url: `/pages/booking-detail/index?id=${booking.id}` })
  }

  const statusLabel = (status: string) => BookingStatusMap[status] || status
  const statusColor = (status: string) => BookingStatusColor[status] || '#8C8C8C'

  return (
    <View className='my-bookings-page'>
      {/* 分段控制器 */}
      <View className='segment-control'>
        <ScrollView scrollX className='segment-scroll'>
          {TABS.map((tab) => (
            <View
              key={tab.key}
              className={`segment-item ${activeTab === tab.key ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              <Text>{tab.label}</Text>
            </View>
          ))}
        </ScrollView>
      </View>

      {/* 预订列表 */}
      <ScrollView
        scrollY
        className='booking-list'
        refresherEnabled
        onRefresherRefresh={fetchBookings}
      >
        {loading ? (
          <View className='list-tip'><Text>加载中...</Text></View>
        ) : bookings.length === 0 ? (
          <View className='list-tip empty-tip'>
            <Text className='empty-icon'>&#x1F4CB;</Text>
            <Text className='empty-text'>暂无预订记录</Text>
            <Text className='empty-hint'>快去首页预订会议室吧</Text>
          </View>
        ) : (
          bookings.map((booking) => (
            <View
              key={booking.id}
              className='booking-card'
              onClick={() => handleBookingClick(booking)}
            >
              <View className='booking-card-header'>
                <Text className='booking-room-name'>{booking.roomName}</Text>
                <View
                  className='status-badge'
                  style={{ backgroundColor: `${statusColor(booking.status)}18`, color: statusColor(booking.status) }}
                >
                  <Text className='status-text'>{statusLabel(booking.status)}</Text>
                </View>
              </View>
              <View className='booking-card-body'>
                <Text className='booking-title'>{booking.title}</Text>
                <View className='booking-meta'>
                  <Text className='meta-item'>{booking.bookingDate}</Text>
                  <Text className='meta-item'>{booking.startTime} - {booking.endTime}</Text>
                </View>
                <Text className='booking-venue'>{booking.venueName}</Text>
              </View>
            </View>
          ))
        )}
      </ScrollView>
    </View>
  )
}

export default MyBookingsPage
