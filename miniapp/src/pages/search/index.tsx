import { useState, useEffect } from 'react'
import { View, Text, Input, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { roomService, RoomVO } from '../../services/rooms'
import RoomCard from '../../components/RoomCard'
import './index.scss'

function SearchPage() {
  const [keyword, setKeyword] = useState('')
  const [rooms, setRooms] = useState<RoomVO[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(true)
  const [total, setTotal] = useState(0)

  useEffect(() => {
    searchRooms()
  }, [])

  const searchRooms = async (pageNum: number = 1) => {
    setLoading(true)
    try {
      const params: any = {
        page: pageNum,
        size: 10
      }
      if (keyword.trim()) params.keyword = keyword.trim()
      const res = await roomService.search(params)
      const records = res.data.records || []
      if (pageNum === 1) {
        setRooms(records)
      } else {
        setRooms(prev => [...prev, ...records])
      }
      setTotal(res.data.total || 0)
      setHasMore(records.length >= 10)
    } catch (err) {
      console.error('搜索失败', err)
      Taro.showToast({ title: '搜索失败，请重试', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = () => {
    setPage(1)
    searchRooms(1)
  }

  const handleRoomClick = (room: RoomVO) => {
    Taro.navigateTo({ url: `/pages/room-detail/index?id=${room.id}` })
  }

  const handleRefresh = () => {
    setPage(1)
    searchRooms(1)
  }

  const handleLoadMore = () => {
    if (!hasMore || loading) return
    const nextPage = page + 1
    setPage(nextPage)
    searchRooms(nextPage)
  }

  return (
    <View className='search-page'>
      <View className='search-header'>
        <View className='search-input-box'>
          <Text className='search-icon'>&#x1F50D;</Text>
          <Input
            className='search-input'
            placeholder='搜索会议室名称'
            value={keyword}
            onInput={(e: any) => setKeyword(e.detail.value)}
            onConfirm={handleSearch}
            confirmType='search'
          />
        </View>
        <View className='search-btn' onClick={handleSearch}>
          <Text className='search-btn-text'>搜索</Text>
        </View>
      </View>

      <ScrollView
        scrollY
        className='search-results'
        refresherEnabled
        onRefresherRefresh={handleRefresh}
        onScrollToLower={handleLoadMore}
      >
        {loading && page === 1 ? (
          <View className='loading-tip'><Text>搜索中...</Text></View>
        ) : rooms.length === 0 ? (
          <View className='empty-tip'>
            <Text className='empty-icon'>&#x1F3E2;</Text>
            <Text>暂无符合条件的会议室</Text>
          </View>
        ) : (
          <>
            <View className='result-header'>
              <Text className='result-count'>共 {total} 个会议室</Text>
            </View>
            {rooms.map((room) => (
              <RoomCard key={room.id} room={room} onClick={handleRoomClick} />
            ))}
            {loading && page > 1 && (
              <View className='loading-more'><Text>加载更多...</Text></View>
            )}
            {!hasMore && rooms.length > 0 && (
              <View className='no-more'><Text>已加载全部</Text></View>
            )}
          </>
        )}
      </ScrollView>
    </View>
  )
}

export default SearchPage
