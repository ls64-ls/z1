export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/search/index',
    'pages/room-detail/index',
    'pages/booking/index',
    'pages/booking-success/index',
    'pages/my-bookings/index',
    'pages/booking-detail/index',
    'pages/check-in/index',
    'pages/profile/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#1677FF',
    navigationBarTitleText: '会议室预约',
    navigationBarTextStyle: 'white'
  },
  tabBar: {
    color: '#999999',
    selectedColor: '#1677FF',
    backgroundColor: '#FFFFFF',
    borderStyle: 'black',
    list: [
      { pagePath: 'pages/index/index', text: '首页', iconPath: 'assets/icons/home.png', selectedIconPath: 'assets/icons/home-active.png' },
      { pagePath: 'pages/my-bookings/index', text: '我的预订', iconPath: 'assets/icons/booking.png', selectedIconPath: 'assets/icons/booking-active.png' },
      { pagePath: 'pages/profile/index', text: '我的', iconPath: 'assets/icons/profile.png', selectedIconPath: 'assets/icons/profile-active.png' }
    ]
  }
})
