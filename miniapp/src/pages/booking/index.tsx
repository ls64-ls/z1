import { useState } from 'react'
import { View, Text, Input, Textarea, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useBookingStore } from '../../store/bookingStore'
import { bookingService } from '../../services/bookings'
import './index.scss'

function BookingPage() {
  const { selectedRoom, selectedDate, selectedStartTime, selectedEndTime, clear } = useBookingStore()
  const [title, setTitle] = useState('')
  const [attendeeCount, setAttendeeCount] = useState('')
  const [remark, setRemark] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (!selectedRoom) {
    return (
      <View className='booking-page'>
        <View className='empty-container'>
          <Text className='empty-text'>未选择会议室，请返回重新操作</Text>
          <Button className='back-btn' onClick={() => Taro.navigateBack()}>返回</Button>
        </View>
      </View>
    )
  }

  const startHour = selectedStartTime ? parseInt(selectedStartTime.split(':')[0]) : 0
  const endHour = selectedEndTime ? parseInt(selectedEndTime.split(':')[0]) : 0
  const hours = Math.max(endHour - startHour, 1)
  const totalPrice = hours * (selectedRoom.pricePerHour || 0)

  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const dateObj = new Date(selectedDate)
  const weekdayStr = weekdays[dateObj.getDay()]

  const handleSubmit = async () => {
    if (!title.trim()) {
      Taro.showToast({ title: '请输入会议主题', icon: 'none' })
      return
    }

    setSubmitting(true)
    try {
      // Pre-check before creating
      const preRes = await bookingService.preCheck(
        selectedRoom.id, selectedDate, selectedStartTime, selectedEndTime
      )
      if (preRes.code === 0 && preRes.data && !preRes.data.available) {
        const suggestion = preRes.data.suggestion
        if (suggestion) {
          Taro.showModal({
            title: '时段冲突',
            content: `已被预订。建议: ${suggestion.availableStart} - ${suggestion.availableEnd}`,
            showCancel: false
          })
        } else {
          Taro.showToast({ title: '该时段已被预订', icon: 'none' })
        }
        setSubmitting(false)
        return
      }

      const res = await bookingService.create({
        roomId: selectedRoom.id,
        bookingDate: selectedDate,
        startTime: selectedStartTime,
        endTime: selectedEndTime,
        title: title.trim(),
        attendeeCount: attendeeCount ? Number(attendeeCount) : undefined,
        remark: remark.trim() || undefined
      })
      if (res.code === 0) {
        clear()
        Taro.redirectTo({ url: `/pages/booking-success/index?id=${res.data.id}` })
      } else {
        Taro.showToast({ title: res.message || '预订失败', icon: 'none' })
      }
    } catch (err: any) {
      Taro.showToast({ title: err.message || '网络错误', icon: 'none' })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <View className='booking-page'>
      {/* 房间信息摘要 */}
      <View className='booking-header'>
        <Text className='header-room-name'>{selectedRoom.name}</Text>
        <Text className='header-venue'>{selectedRoom.venueName}</Text>
      </View>

      {/* 时间信息 */}
      <View className='info-card'>
        <View className='info-row'>
          <Text className='info-label'>预订日期</Text>
          <Text className='info-value'>{selectedDate} {weekdayStr}</Text>
        </View>
        <View className='info-row'>
          <Text className='info-label'>使用时间</Text>
          <Text className='info-value'>{selectedStartTime} - {selectedEndTime}（{hours}小时）</Text>
        </View>
        <View className='info-row'>
          <Text className='info-label'>容纳人数</Text>
          <Text className='info-value'>{selectedRoom.capacity} 人</Text>
        </View>
      </View>

      {/* 预订表单 */}
      <View className='form-card'>
        <View className='form-item'>
          <Text className='form-label'>
            会议主题 <Text className='required'>*</Text>
          </Text>
          <Input
            className='form-input'
            placeholder='请输入会议主题'
            value={title}
            onInput={(e: any) => setTitle(e.detail.value)}
            maxlength={50}
          />
        </View>

        <View className='form-item'>
          <Text className='form-label'>参会人数</Text>
          <Input
            className='form-input'
            type='number'
            placeholder={`不超过${selectedRoom.capacity}人`}
            value={attendeeCount}
            onInput={(e: any) => setAttendeeCount(e.detail.value)}
          />
        </View>

        <View className='form-item'>
          <Text className='form-label'>备注说明</Text>
          <Textarea
            className='form-textarea'
            placeholder='如有特殊需求请备注说明'
            value={remark}
            onInput={(e: any) => setRemark(e.detail.value)}
            maxlength={200}
            autoHeight
          />
        </View>
      </View>

      {/* 费用明细 */}
      <View className='price-card'>
        <Text className='price-title'>费用明细</Text>
        <View className='price-row'>
          <Text className='price-label'>{selectedRoom.name} x {hours}小时</Text>
          <Text className='price-value'>¥{totalPrice}</Text>
        </View>
        <View className='price-divider' />
        <View className='price-row total-row'>
          <Text className='price-label'>合计</Text>
          <Text className='total-price'>¥{totalPrice}</Text>
        </View>
      </View>

      {/* 提交按钮 */}
      <View className='submit-section'>
        <Button
          className='submit-btn'
          onClick={handleSubmit}
          loading={submitting}
          disabled={submitting}
        >
          {submitting ? '提交中...' : `确认预订 ¥${totalPrice}`}
        </Button>
      </View>
    </View>
  )
}

export default BookingPage
