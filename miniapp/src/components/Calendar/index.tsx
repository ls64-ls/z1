import React, { useState, useEffect, useMemo } from 'react'
import { View, Text } from '@tarojs/components'
import './index.scss'

interface CalendarProps {
  value?: string
  onChange?: (date: string) => void
  availableDates?: string[]
  minDate?: string
  maxDate?: string
}

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

function Calendar({ value, onChange, availableDates = [], minDate, maxDate }: CalendarProps) {
  const today = useMemo(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }, [])

  const [currentDate, setCurrentDate] = useState(value || today)
  const [displayYear, setDisplayYear] = useState(new Date().getFullYear())
  const [displayMonth, setDisplayMonth] = useState(new Date().getMonth() + 1)

  useEffect(() => {
    if (value) {
      setCurrentDate(value)
      const [y, m] = value.split('-').map(Number)
      setDisplayYear(y)
      setDisplayMonth(m)
    }
  }, [value])

  const daysInMonth = useMemo(() => {
    return new Date(displayYear, displayMonth, 0).getDate()
  }, [displayYear, displayMonth])

  const firstDayOfWeek = useMemo(() => {
    return new Date(displayYear, displayMonth - 1, 1).getDay()
  }, [displayYear, displayMonth])

  const calendarDays = useMemo(() => {
    const days: (number | null)[] = []
    for (let i = 0; i < firstDayOfWeek; i++) {
      days.push(null)
    }
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i)
    }
    return days
  }, [daysInMonth, firstDayOfWeek])

  const isAvailable = (day: number): boolean => {
    const dateStr = `${displayYear}-${String(displayMonth).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    if (availableDates.length > 0) {
      return availableDates.includes(dateStr)
    }
    if (minDate && dateStr < minDate) return false
    if (maxDate && dateStr > maxDate) return false
    if (dateStr < today) return false
    return true
  }

  const isSelected = (day: number): boolean => {
    const dateStr = `${displayYear}-${String(displayMonth).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    return dateStr === currentDate
  }

  const isToday = (day: number): boolean => {
    const dateStr = `${displayYear}-${String(displayMonth).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    return dateStr === today
  }

  const handleDayClick = (day: number) => {
    if (!isAvailable(day)) return
    const dateStr = `${displayYear}-${String(displayMonth).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    setCurrentDate(dateStr)
    onChange?.(dateStr)
  }

  const prevMonth = () => {
    if (displayMonth === 1) {
      setDisplayYear(displayYear - 1)
      setDisplayMonth(12)
    } else {
      setDisplayMonth(displayMonth - 1)
    }
  }

  const nextMonth = () => {
    if (displayMonth === 12) {
      setDisplayYear(displayYear + 1)
      setDisplayMonth(1)
    } else {
      setDisplayMonth(displayMonth + 1)
    }
  }

  return (
    <View className='calendar'>
      <View className='calendar-header'>
        <View className='nav-btn' onClick={prevMonth}>
          <Text className='nav-arrow'>&lt;</Text>
        </View>
        <Text className='month-label'>{displayYear}年{displayMonth}月</Text>
        <View className='nav-btn' onClick={nextMonth}>
          <Text className='nav-arrow'>&gt;</Text>
        </View>
      </View>

      <View className='weekday-row'>
        {WEEKDAYS.map((wd) => (
          <Text key={wd} className='weekday-cell'>{wd}</Text>
        ))}
      </View>

      <View className='days-grid'>
        {calendarDays.map((day, idx) => (
          <View
            key={idx}
            className={`day-cell ${day === null ? 'empty' : ''} ${day !== null && isSelected(day) ? 'selected' : ''} ${day !== null && !isAvailable(day) ? 'disabled' : ''}`}
            onClick={() => day !== null && handleDayClick(day)}
          >
            {day !== null && (
              <React.Fragment>
                <Text className='day-num'>{day}</Text>
                {isToday(day) && <View className='today-dot' />}
              </React.Fragment>
            )}
          </View>
        ))}
      </View>
    </View>
  )
}

export default Calendar
