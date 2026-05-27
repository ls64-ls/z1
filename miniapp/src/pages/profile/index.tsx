import { useEffect, useState } from 'react'
import { View, Text, Image, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useUserStore } from '../../store/userStore'
import { authService, UserInfo } from '../../services/auth'
import './index.scss'

function ProfilePage() {
  const { user, setUser, clear } = useUserStore()
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!user) {
      loadProfile()
    }
  }, [])

  const loadProfile = async () => {
    setLoading(true)
    try {
      const profile = await authService.getProfile()
      setUser(profile)
    } catch (err) {
      console.error('获取用户信息失败', err)
    } finally {
      setLoading(false)
    }
  }

  const handleGetUserProfile = async () => {
    try {
      const res = await Taro.getUserProfile({ desc: '用于完善个人资料' })
      if (res.userInfo) {
        setUser({
          ...user,
          nickname: res.userInfo.nickName,
          avatarUrl: res.userInfo.avatarUrl
        })
      }
    } catch (err) {
      console.log('用户拒绝授权')
    }
  }

  const handleLogin = async () => {
    try {
      await authService.login()
      Taro.showToast({ title: '登录成功', icon: 'success' })
      await loadProfile()
    } catch (err) {
      Taro.showToast({ title: '登录失败', icon: 'none' })
    }
  }

  const menuItems = [
    {
      icon: '\u{1F4C5}',
      title: '我的预订',
      desc: '查看全部预订记录',
      url: '/pages/my-bookings/index'
    },
    {
      icon: '⚙️',
      title: '设置',
      desc: '账号安全与偏好设置',
      url: ''
    },
    {
      icon: 'ℹ️',
      title: '关于',
      desc: '版本信息与帮助',
      url: ''
    }
  ]

  const handleMenuClick = (item: typeof menuItems[0]) => {
    if (item.url) {
      if (item.url.startsWith('/pages/my-bookings')) {
        Taro.switchTab({ url: item.url })
      } else {
        Taro.navigateTo({ url: item.url })
      }
    } else {
      Taro.showToast({ title: `${item.title}功能开发中`, icon: 'none' })
    }
  }

  return (
    <View className='profile-page'>
      {/* 用户信息头部 */}
      <View className='profile-header'>
        {user ? (
          <View className='user-info'>
            <View className='avatar-box' onClick={handleGetUserProfile}>
              {user.avatarUrl ? (
                <Image src={user.avatarUrl} className='avatar-img' />
              ) : (
                <View className='avatar-placeholder'>
                  <Text className='avatar-text'>{user.nickname?.charAt(0) || 'U'}</Text>
                </View>
              )}
            </View>
            <View className='user-detail'>
              <Text className='user-name'>{user.nickname || '微信用户'}</Text>
              <Text className='user-phone'>{user.phone || '未绑定手机号'}</Text>
            </View>
          </View>
        ) : (
          <View className='login-prompt'>
            <View className='avatar-placeholder'>
              <Text className='avatar-text'>&#x1F464;</Text>
            </View>
            <Text className='login-text'>点击登录，享受更多服务</Text>
            <Button className='login-btn' onClick={handleLogin} loading={loading}>
              {loading ? '加载中...' : '微信一键登录'}
            </Button>
          </View>
        )}
      </View>

      {/* 统计信息 */}
      {user && (
        <View className='stats-row'>
          <View className='stat-item'>
            <Text className='stat-num'>0</Text>
            <Text className='stat-label'>全部预订</Text>
          </View>
          <View className='stat-item'>
            <Text className='stat-num'>0</Text>
            <Text className='stat-label'>待使用</Text>
          </View>
          <View className='stat-item'>
            <Text className='stat-num'>0</Text>
            <Text className='stat-label'>已完成</Text>
          </View>
        </View>
      )}

      {/* 菜单列表 */}
      <View className='menu-list'>
        {menuItems.map((item, index) => (
          <View key={index} className='menu-item' onClick={() => handleMenuClick(item)}>
            <Text className='menu-icon'>{item.icon}</Text>
            <View className='menu-content'>
              <Text className='menu-title'>{item.title}</Text>
              <Text className='menu-desc'>{item.desc}</Text>
            </View>
            <Text className='menu-arrow'>&#x203A;</Text>
          </View>
        ))}
      </View>

      {/* 退出登录 */}
      {user && (
        <View className='logout-section'>
          <Button className='logout-btn' onClick={() => { clear(); Taro.showToast({ title: '已退出', icon: 'none' }) }}>
            退出登录
          </Button>
        </View>
      )}
    </View>
  )
}

export default ProfilePage
