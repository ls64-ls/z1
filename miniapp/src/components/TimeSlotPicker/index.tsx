import React from 'react'
import { View, Text } from '@tarojs/components'
import './index.scss'

interface TimeSlot {
  start: string
  end: string
  available: boolean
}

interface TimeSlotPickerProps {
  slots: TimeSlot[]
  selectedStart?: string
  selectedEnd?: string
  onSelect?: (start: string, end: string) => void
}

function TimeSlotPicker({ slots, selectedStart, selectedEnd, onSelect }: TimeSlotPickerProps) {
  const handleSelect = (slot: TimeSlot) => {
    if (!slot.available) return
    onSelect?.(slot.start, slot.end)
  }

  const isSelected = (slot: TimeSlot): boolean => {
    return selectedStart === slot.start && selectedEnd === slot.end
  }

  if (slots.length === 0) {
    return (
      <View className='timeslot-picker'>
        <View className='empty-slots'>
          <Text className='empty-text'>暂无可用时段</Text>
        </View>
      </View>
    )
  }

  return (
    <View className='timeslot-picker'>
      <Text className='picker-title'>选择时段</Text>
      <View className='slots-grid'>
        {slots.map((slot, idx) => (
          <View
            key={idx}
            className={`slot-chip ${isSelected(slot) ? 'selected' : ''} ${!slot.available ? 'disabled' : ''}`}
            onClick={() => handleSelect(slot)}
          >
            <Text className='slot-time'>{slot.start} - {slot.end}</Text>
            <Text className='slot-status'>
              {slot.available ? '空闲' : '已约'}
            </Text>
          </View>
        ))}
      </View>
    </View>
  )
}

export default TimeSlotPicker
