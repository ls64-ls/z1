import { View, Text, Button, Image } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { useBookingStore } from '../../store/bookingStore'
import './index.scss'

function BookingSuccessPage() {
  const router = useRouter()
  const { id } = router.params
  const { selectedRoom, selectedDate, selectedStartTime, selectedEndTime } = useBookingStore()

  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const dateObj = new Date(selectedDate)
  const weekdayStr = weekdays[dateObj.getDay()]

  const handleViewDetail = () => {
    Taro.redirectTo({ url: `/pages/booking-detail/index?id=${id}` })
  }

  const handleGoHome = () => {
    Taro.switchTab({ url: '/pages/index/index' })
  }

  return (
    <View className='success-page'>
      <View className='success-content'>
        {/* 成功图标 */}
        <View className='success-icon'>
          <View className='icon-circle'>
            <Text className='check-mark'>&#x2713;</Text>
          </View>
        </View>

        <Text className='success-title'>预订成功</Text>
        <Text className='success-subtitle'>您已成功预订会议室</Text>

        {/* 预订摘要 */}
        <View className='booking-summary'>
          <View className='summary-row'>
            <Text className='summary-label'>会议室</Text>
            <Text className='summary-value'>{selectedRoom?.name || '-'}</Text>
          </View>
          <View className='summary-row'>
            <Text className='summary-label'>日期</Text>
            <Text className='summary-value'>{selectedDate} {weekdayStr}</Text>
          </View>
          <View className='summary-row'>
            <Text className='summary-label'>时间</Text>
            <Text className='summary-value'>{selectedStartTime} - {selectedEndTime}</Text>
          </View>
          <View className='summary-row'>
            <Text className='summary-label'>地点</Text>
            <Text className='summary-value'>{selectedRoom?.venueName || '-'}</Text>
          </View>
          <View className='summary-row'>
            <Text className='summary-label'>预订编号</Text>
            <Text className='summary-value booking-id'>{id || '-'}</Text>
          </View>
        </View>

        {/* 操作按钮 */}
        <View className='action-buttons'>
          <Button className='action-btn primary-btn' onClick={handleViewDetail}>
            查看详情
          </Button>
          <Button className='action-btn secondary-btn' onClick={handleGoHome}>
            返回首页
          </Button>
        </View>
      </View>
    </View>
  )
}

export default BookingSuccessPage
