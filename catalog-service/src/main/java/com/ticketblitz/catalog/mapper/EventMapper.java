package com.ticketblitz.catalog.mapper;

import com.ticketblitz.catalog.dto.EventDto;
import com.ticketblitz.catalog.dto.EventListDto;
import com.ticketblitz.catalog.entity.Event;
import org.mapstruct.*;

import java.util.List;

/**
 * Event Mapper - MAPSTRUCT PATTERN (Staff Engineer Level)
 *
 * WHY MAPSTRUCT:
 * ==============
 * 1. Zero runtime overhead (compile-time code generation)
 * 2. Type-safe (compile-time checking)
 * 3. No reflection (faster than libraries like ModelMapper)
 * 4. Easy to customize
 * 5. Handles nested objects automatically
 *
 * ALTERNATIVES REJECTED:
 * ======================
 * - Manual mapping: Too much boilerplate
 * - ModelMapper: Runtime reflection (slower)
 * - BeanUtils: Not type-safe
 *
 * PERFORMANCE:
 * ============
 * MapStruct generates code like:
 *   EventDto dto = new EventDto();
 *   dto.setId(event.getId());
 *   dto.setName(event.getName());
 *   // ... etc
 *
 * This is as fast as hand-written code!
 *
 * CUSTOM MAPPINGS:
 * ================
 * @Mapping - Custom field mapping
 * @AfterMapping - Post-processing logic
 *
 * @author Staff Engineer
 */
@Mapper(
        componentModel = "spring",
        uses = {VenueMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EventMapper {
    /**
     * Entity → Full DTO (for detail views)
     *
     * MapStruct automatically maps:
     * - Same field names
     * - Nested objects (venue → venueDto via VenueMapper)
     */
    EventDto toDto(Event event);

    /**
     * Entity -> List Dto (for list views)
     * Custom mappings:
     * - venue.name -> venueName (flatten nested object)
     * - venue.city -> venueCity
     */
    @Mapping(source="venue.name", target="venueName")
    @Mapping(source="venue.city", target="venueCity")
    EventListDto toListDto(Event event);

    /**
     * Batch conversion - Entity list -> DTO list
     */
    List<EventDto> toDtoList(List<Event> events);

    /**
     * Batch conversion - Entity list → List DTO list
     */
    List<EventListDto> toListDtoList(List<Event> events);

    /**
     * Post processing: calculate computed fields
     * - MapStruct handles mapping
     * - @AfterMapping handles business logic
     */
    @AfterMapping
    default void calculateComputedFields(
            @MappingTarget EventDto dto,
            Event event
    ) {
        // calculate booked seats
        int bookedSeats = event.getTotalSeats() - event.getAvailableSeats();
        dto.setBookedSeats(bookedSeats);

        // calculate occupancy rate
        double occupancyRate = (double) bookedSeats / event.getTotalSeats() * 100;
        dto.setOccupancyRate(occupancyRate);

        // check if almost sold out (>90%)
        dto.setIsAlmostSoldOut(occupancyRate > 90.0);
    }

}