package com.example.booking.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.booking.entity.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;

@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    @Select("""
        SELECT COUNT(*)
        FROM booking
        WHERE room_id = #{roomId}
          AND booking_date = #{bookingDate}
          AND status NOT IN ('CANCELLED', 'EXPIRED')
          AND time_slot && tsrange(
              (CAST(#{bookingDate} AS date) + CAST(#{startTime} AS time))::timestamp,
              (CAST(#{bookingDate} AS date) + CAST(#{endTime} AS time))::timestamp,
              '[)'
          )
        """)
    int countConflicts(@Param("roomId") Long roomId,
                       @Param("bookingDate") LocalDate bookingDate,
                       @Param("startTime") LocalTime startTime,
                       @Param("endTime") LocalTime endTime);
}
