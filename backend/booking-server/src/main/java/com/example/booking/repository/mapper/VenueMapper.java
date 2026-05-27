package com.example.booking.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.booking.entity.Venue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VenueMapper extends BaseMapper<Venue> {
}
