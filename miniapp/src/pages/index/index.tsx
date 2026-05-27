import { useState, useEffect } from 'react'
import { View, Text, ScrollView, Navigator } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { roomService, RoomVO } from '../../services/rooms'
import { venueService } from '../../services/venues'
import RoomCard from '../../components/RoomCard'
import './index.scss'

function IndexPage() {
  const [venues, setVenues] = useState<{ id: number; name: string }[]>([])
  const [selectedVenueId, setSelectedVenueId] = useState<number>(1)
  const [rooms, setRooms] = useState<RoomVO[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchVenues()
  }, [])

  useEffect(() => {
    if (selectedVenueId) {
      fetchRooms()
    }
  }, [selectedVenueId])

  const fetchVenues = async () => {
    try {
      const res = await venueService.list()
      if (res.code === 0 && res.data) {
        setVenues(res.data)
        if (res.data.length > 0) {
          setSelectedVenueId(res.data[0].id)
        }
      }
    } catch (err) {
      console.error('获取门店失败', err)
    }
  }

  const fetchRooms = async () => {
    setLoading(true)
    try {
      const res = await roomService.search({ venueId: selectedVenueId, page: 1, size: 20 })
      setRooms(res.data.records || [])
    } catch (err) {
      console.error('获取会议室失败', err)
    } finally {
      setLoading(false)
    }
  }

  const handleRoomClick = (room: RoomVO) => {
    Taro.navigateTo({ url: `/pages/room-detail/index?id=${room.id}` })
  }

  return (
    <View className='index-page'>
      {/* 门店切换 */}
      <View className='venue-switcher'>
        <ScrollView scrollX className='venue-scroll'>
          {venues.map((v) => (
            <View
              key={v.id}
              className={`venue-tab ${v.id === selectedVenueId ? 'active' : ''}`}
              onClick={() => setSelectedVenueId(v.id)}
            >
              <Text>{v.name}</Text>
            </View>
          ))}
        </ScrollView>
        <View className='search-entry'>
          <Navigator url='/pages/search/index'>
            <Text className='search-icon'>&#x1F50D;</Text>
            <Text className='search-text'>搜索会议室</Text>
          </Navigator>
        </View>
      </View>

      {/* 会议室列表 */}
      <ScrollView scrollY className='room-list' refresherEnabled onRefresherRefresh={fetchRooms}>
        {loading ? (
          <View className='loading-tip'><Text>加载中...</Text></View>
        ) : rooms.length === 0 ? (
          <View className='empty-tip'><Text>暂无可用会议室</Text></View>
        ) : (
          rooms.map((room) => (
            <RoomCard key={room.id} room={room} onClick={handleRoomClick} />
          ))
        )}
      </ScrollView>
    </View>
  )
}

export default IndexPage
