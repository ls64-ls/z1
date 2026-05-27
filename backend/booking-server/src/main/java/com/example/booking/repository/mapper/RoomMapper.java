package com.example.booking.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.booking.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoomMapper extends BaseMapper<Room> {

    @Select("SELECT * FROM room WHERE id = #{id} FOR UPDATE")
    Room selectByIdForUpdate(@Param("id") Long id);

    @Select("""
        <script>
        SELECT DISTINCT r.* FROM room r
        LEFT JOIN room_amenity ra ON r.id = ra.room_id
        WHERE r.status = 'AVAILABLE'
        <if test='venueId != null'>AND r.venue_id = #{venueId}</if>
        <if test='capacity != null'>AND r.capacity >= #{capacity}</if>
        <if test='amenityIds != null and amenityIds.size() > 0'>
            AND ra.amenity_id IN
            <foreach collection='amenityIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
        </if>
        ORDER BY r.id
        </script>
        """)
    List<Room> searchRooms(@Param("venueId") Long venueId,
                           @Param("capacity") Integer capacity,
                           @Param("amenityIds") List<Long> amenityIds);
}
