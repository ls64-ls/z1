export const BookingStatusMap: Record<string, string> = {
  PENDING: '待支付',
  CONFIRMED: '已确认',
  CHECKED_IN: '已签到',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  EXPIRED: '已过期'
}

export const BookingStatusColor: Record<string, string> = {
  PENDING: '#FAAD14',
  CONFIRMED: '#1677FF',
  CHECKED_IN: '#52C41A',
  COMPLETED: '#8C8C8C',
  CANCELLED: '#FF4D4F',
  EXPIRED: '#8C8C8C'
}
