package com.example.booking.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.booking.entity.BlockedTimeSlot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlockedTimeSlotMapper extends BaseMapper<BlockedTimeSlot> {
}
