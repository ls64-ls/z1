package com.example.booking.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class RoomSearchRequest {
    private Long venueId;
    private String date;
    private String startTime;
    private String endTime;
    private Integer capacity;
    private List<Long> amenityIds;
    private Integer page = 1;
    private Integer size = 10;
}
