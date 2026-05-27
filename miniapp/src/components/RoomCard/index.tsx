import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import './index.scss'

interface RoomCardProps {
  room: {
    id: number
    name: string
    venueName: string
    capacity: number
    areaSqm: number
    floor: number
    pricePerHour: number
    status: string
    available: boolean
    remainingSlots: number
    amenities?: { id: number; name: string }[]
    coverImage?: string
  }
  onClick?: (room: any) => void
  showBookBtn?: boolean
}

function RoomCard({ room, onClick, showBookBtn = true }: RoomCardProps) {
  const handleClick = () => {
    if (onClick) {
      onClick(room)
    } else {
      Taro.navigateTo({ url: `/pages/room-detail/index?id=${room.id}` })
    }
  }

  return (
    <View className='room-card' onClick={handleClick}>
      <View className='room-cover'>
        {room.coverImage ? (
          <View className='room-image' style={{ backgroundImage: `url(${room.coverImage})` }} />
        ) : (
          <View className='room-image-placeholder'>
            <Text className='placeholder-text'>{room.name.charAt(0)}</Text>
          </View>
        )}
        <View className={`room-status-tag ${room.available ? 'available' : 'full'}`}>
          <Text>{room.available ? '可预订' : '已满'}</Text>
        </View>
      </View>

      <View className='room-info'>
        <Text className='room-name'>{room.name}</Text>
        <Text className='room-venue'>{room.venueName}</Text>

        <View className='room-meta'>
          <Text className='meta-item'>容纳 {room.capacity} 人</Text>
          {room.areaSqm && <Text className='meta-item'>{room.areaSqm} m²</Text>}
          {room.floor && <Text className='meta-item'>{room.floor}F</Text>}
        </View>

        {room.amenities && room.amenities.length > 0 && (
          <View className='room-amenities'>
            {room.amenities.slice(0, 4).map((a) => (
              <Text key={a.id} className='amenity-tag'>{a.name}</Text>
            ))}
            {room.amenities.length > 4 && (
              <Text className='amenity-tag'>+{room.amenities.length - 4}</Text>
            )}
          </View>
        )}

        <View className='room-footer'>
          <View className='room-price'>
            <Text className='price-value'>¥{room.pricePerHour}</Text>
            <Text className='price-unit'>/小时</Text>
          </View>
          {showBookBtn && room.available && (
            <View className='book-btn' hoverClass='book-btn-active'>
              <Text className='book-btn-text'>立即预订</Text>
            </View>
          )}
        </View>
      </View>
    </View>
  )
}

export default RoomCard
