package com.example.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.booking.dto.response.VenueVO;
import com.example.booking.entity.Venue;
import com.example.booking.repository.mapper.RoomMapper;
import com.example.booking.repository.mapper.VenueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueMapper venueMapper;
    private final RoomMapper roomMapper;

    public List<VenueVO> listActive() {
        List<Venue> venues = venueMapper.selectList(
                new LambdaQueryWrapper<Venue>().eq(Venue::getStatus, "ACTIVE"));
        return venues.stream().map(this::toVenueVO).collect(Collectors.toList());
    }

    private VenueVO toVenueVO(Venue venue) {
        Long roomCount = roomMapper.selectCount(
                new LambdaQueryWrapper<com.example.booking.entity.Room>()
                        .eq(com.example.booking.entity.Room::getVenueId, venue.getId())
                        .eq(com.example.booking.entity.Room::getStatus, "AVAILABLE"));

        return VenueVO.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .latitude(venue.getLatitude())
                .longitude(venue.getLongitude())
                .status(venue.getStatus())
                .roomCount(roomCount.intValue())
                .build();
    }
}
