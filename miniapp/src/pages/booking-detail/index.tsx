import { useState, useEffect } from 'react'
import { View, Text, Button, ScrollView } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { bookingService } from '../../services/bookings'
import { BookingStatusMap, BookingStatusColor } from '../../constants/enums'
import './index.scss'

interface BookingDetail {
  id: number
  roomName: string
  venueName: string
  roomAddress: string
  bookingDate: string
  startTime: string
  endTime: string
  status: string
  title: string
  attendeeCount: number
  remark: string
  totalPrice: number
  createTime: string
}

function BookingDetailPage() {
  const router = useRouter()
  const { id } = router.params
  const [booking, setBooking] = useState<BookingDetail | null>(null)
  const [showCancelModal, setShowCancelModal] = useState(false)
  const [cancelReason, setCancelReason] = useState('')

  useEffect(() => {
    if (id) fetchDetail()
  }, [id])

  const fetchDetail = async () => {
    try {
      const res = await bookingService.getDetail(Number(id))
      setBooking(res.data)
    } catch (err) {
      console.error('获取详情失败', err)
      Taro.showToast({ title: '加载失败', icon: 'none' })
    }
  }

  const handleCancel = async () => {
    if (!booking) return
    try {
      await bookingService.cancel(booking.id, cancelReason || undefined)
      Taro.showToast({ title: '取消成功', icon: 'success' })
      setShowCancelModal(false)
      fetchDetail()
    } catch (err) {
      Taro.showToast({ title: '取消失败', icon: 'none' })
    }
  }

  const handleCheckIn = () => {
    Taro.navigateTo({ url: `/pages/check-in/index?bookingId=${id}` })
  }

  if (!booking) {
    return (
      <View className='detail-page'>
        <View className='loading-container'><Text>加载中...</Text></View>
      </View>
    )
  }

  const statusColor = BookingStatusColor[booking.status] || '#8C8C8C'
  const statusLabel = BookingStatusMap[booking.status] || booking.status
  const canCancel = ['PENDING', 'CONFIRMED'].includes(booking.status)
  const canCheckIn = booking.status === 'CONFIRMED'

  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const dateObj = new Date(booking.bookingDate)
  const weekdayStr = weekdays[dateObj.getDay()]

  return (
    <View className='detail-page'>
      <ScrollView scrollY className='detail-scroll'>
        {/* 状态头部 */}
        <View className='status-header' style={{ backgroundColor: `${statusColor}12` }}>
          <View className='status-info'>
            <Text className='status-label' style={{ color: statusColor }}>{statusLabel}</Text>
            <Text className='booking-id'>预订编号：{booking.id}</Text>
          </View>
        </View>

        {/* 基本信息 */}
        <View className='info-card'>
          <Text className='card-title'>基本信息</Text>
          <View className='info-row'>
            <Text className='info-label'>会议室</Text>
            <Text className='info-value'>{booking.roomName}</Text>
          </View>
          <View className='info-row'>
            <Text className='info-label'>地点</Text>
            <Text className='info-value'>{booking.roomAddress || booking.venueName}</Text>
          </View>
          <View className='info-row'>
            <Text className='info-label'>日期</Text>
            <Text className='info-value'>{booking.bookingDate} {weekdayStr}</Text>
          </View>
          <View className='info-row'>
            <Text className='info-label'>时间</Text>
            <Text className='info-value'>{booking.startTime} - {booking.endTime}</Text>
          </View>
          <View className='info-row'>
            <Text className='info-label'>会议主题</Text>
            <Text className='info-value'>{booking.title}</Text>
          </View>
          {booking.attendeeCount > 0 && (
            <View className='info-row'>
              <Text className='info-label'>参会人数</Text>
              <Text className='info-value'>{booking.attendeeCount} 人</Text>
            </View>
          )}
          {booking.remark && (
            <View className='info-row'>
              <Text className='info-label'>备注</Text>
              <Text className='info-value remark-text'>{booking.remark}</Text>
            </View>
          )}
        </View>

        {/* 费用信息 */}
        <View className='info-card'>
          <Text className='card-title'>费用信息</Text>
          <View className='info-row'>
            <Text className='info-label'>合计金额</Text>
            <Text className='info-value price-highlight'>¥{booking.totalPrice}</Text>
          </View>
          <View className='info-row'>
            <Text className='info-label'>创建时间</Text>
            <Text className='info-value'>{booking.createTime}</Text>
          </View>
        </View>
      </ScrollView>

      {/* 底部操作 */}
      <View className='bottom-actions'>
        {canCheckIn && (
          <Button className='action-btn checkin-btn' onClick={handleCheckIn}>
            签到
          </Button>
        )}
        {canCancel && (
          <Button className='action-btn cancel-btn' onClick={() => setShowCancelModal(true)}>
            取消预订
          </Button>
        )}
      </View>

      {/* 取消确认弹窗 */}
      {showCancelModal && (
        <View className='modal-overlay' onClick={() => setShowCancelModal(false)}>
          <View className='modal-content' onClick={(e: any) => e.stopPropagation()}>
            <Text className='modal-title'>确认取消预订？</Text>
            <Text className='modal-desc'>取消后不可恢复，请确认操作</Text>
            <View className='modal-buttons'>
              <Button className='modal-btn cancel' onClick={() => setShowCancelModal(false)}>我再想想</Button>
              <Button className='modal-btn confirm' onClick={handleCancel}>确认取消</Button>
            </View>
          </View>
        </View>
      )}
    </View>
  )
}

export default BookingDetailPage
