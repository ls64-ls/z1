import { useState, useEffect } from 'react'
import { View, Text, Button, Input } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { wechatUtil } from '../../utils/wechat'
import { checkInService } from '../../services/checkin'
import './index.scss'

function CheckInPage() {
  const router = useRouter()
  const { bookingId } = router.params
  const [manualCode, setManualCode] = useState('')
  const [location, setLocation] = useState<{ latitude: number; longitude: number } | null>(null)
  const [locationLoading, setLocationLoading] = useState(false)

  useEffect(() => {
    getLocationSilently()
  }, [])

  const getLocationSilently = async () => {
    setLocationLoading(true)
    try {
      const res = await wechatUtil.getLocation()
      setLocation({ latitude: res.latitude, longitude: res.longitude })
    } catch (err) {
      console.log('获取位置失败，允许手动签到')
    } finally {
      setLocationLoading(false)
    }
  }

  const handleScanCode = async () => {
    try {
      const res = await wechatUtil.scanCode()
      if (res.result) {
        await submitCheckIn(res.result)
      }
    } catch (err) {
      Taro.showToast({ title: '扫码失败，请重试', icon: 'none' })
    }
  }

  const handleManualCheckIn = async () => {
    if (!manualCode.trim()) {
      Taro.showToast({ title: '请输入签到码', icon: 'none' })
      return
    }
    await submitCheckIn(manualCode.trim())
  }

  const submitCheckIn = async (code: string) => {
    Taro.showLoading({ title: '签到中...' })
    try {
      const bid = bookingId ? Number(bookingId) : 0
      const res = await checkInService.checkIn(bid)
      Taro.hideLoading()
      if (res.code === 0) {
        Taro.showToast({ title: '签到成功', icon: 'success' })
        setTimeout(() => Taro.navigateBack(), 1500)
      } else {
        Taro.showToast({ title: res.message || '签到失败', icon: 'none' })
      }
    } catch (err) {
      Taro.hideLoading()
      Taro.showToast({ title: '签到失败，请重试', icon: 'none' })
    }
  }

  return (
    <View className='checkin-page'>
      <View className='checkin-content'>
        {/* 预订信息提示 */}
        {bookingId && (
          <View className='booking-ref'>
            <Text className='ref-label'>预订编号</Text>
            <Text className='ref-value'>{bookingId}</Text>
          </View>
        )}

        {/* 扫码签到 */}
        <View className='scan-section'>
          <View className='scan-icon-box' onClick={handleScanCode}>
            <View className='scan-icon-circle'>
              <Text className='scan-icon-text'>&#x1F4F7;</Text>
            </View>
            <Text className='scan-title'>扫码签到</Text>
            <Text className='scan-desc'>扫描会议室门口的二维码</Text>
          </View>
          <Button className='scan-btn' onClick={handleScanCode}>
            打开扫码
          </Button>
        </View>

        {/* 分割线 */}
        <View className='divider-section'>
          <View className='divider-line' />
          <Text className='divider-text'>或手动输入签到码</Text>
          <View className='divider-line' />
        </View>

        {/* 手动签到 */}
        <View className='manual-section'>
          <Input
            className='manual-input'
            placeholder='请输入6位签到码'
            value={manualCode}
            onInput={(e: any) => setManualCode(e.detail.value)}
            maxlength={6}
            type='number'
          />
          <Button className='manual-btn' onClick={handleManualCheckIn}>
            确认签到
          </Button>
        </View>

        {/* 位置信息 */}
        <View className='location-section'>
          <View className='location-header'>
            <Text className='location-title'>签到位置</Text>
            {locationLoading && <Text className='location-loading'>获取中...</Text>}
          </View>
          {location ? (
            <View className='location-info'>
              <Text className='location-text'>已获取当前位置</Text>
              <Text className='location-coord'>
                {location.latitude.toFixed(6)}, {location.longitude.toFixed(6)}
              </Text>
            </View>
          ) : (
            <View className='location-info'>
              <Text className='location-text no-location'>未获取到位置信息</Text>
              <Button className='retry-location' size='mini' onClick={getLocationSilently}>
                重新获取
              </Button>
            </View>
          )}
        </View>
      </View>
    </View>
  )
}

export default CheckInPage
