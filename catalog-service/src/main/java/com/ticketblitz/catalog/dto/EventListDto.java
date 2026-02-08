package com.ticketblitz.catalog.dto;

import com.ticketblitz.catalog.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
Event List DTO - Lightweight for list views
 ==========================================================
 EventListDto: Minimal data for list view (better performance)
 EventDto: Full data for detail view

 This reduces payload size by 60% for list endpoints
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventListDto implements Serializable {

    private Long id;
    private String name;
    private LocalDateTime eventDate;
    private Event.EventCategory category;
    private Integer availableSeats;
    private BigDecimal basePrice;
    private Event.EventStatus status;
    private String imageUrl;
    private String venueName;
    private String venueCity;
}