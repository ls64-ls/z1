package com.example.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomVO {
    private Long id;
    private String name;
    private Long venueId;
    private String venueName;
    private Integer capacity;
    private BigDecimal areaSqm;
    private Integer floor;
    private String description;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerHalfDay;
    private BigDecimal pricePerDay;
    private String status;
    private boolean available;
    private int remainingSlots;
    private List<AmenityVO> amenities;
    private List<String> images;
    private String coverImage;
}
