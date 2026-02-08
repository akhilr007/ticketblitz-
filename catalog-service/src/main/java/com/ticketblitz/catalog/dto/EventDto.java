package com.ticketblitz.catalog.dto;

import com.ticketblitz.catalog.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventDto implements Serializable {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime eventDate;
    private LocalDateTime eventEndDate;
    private Event.EventCategory category;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal basePrice;
    private Event.EventStatus status;
    private String imageUrl;
    private VenueDto venue;

    // computed fields (not in entity)
    private Integer bookedSeats;
    private Double occupancyRate;
    private Boolean isAlmostSoldOut;
}