package com.ticketblitz.booking.mapper;

import com.ticketblitz.booking.dto.BookingDto;
import com.ticketblitz.booking.dto.BookingItemDto;
import com.ticketblitz.booking.dto.BookingListDto;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingItem;
import org.mapstruct.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BookingMapper {

    /**
     * Entity -> Full dto with computed fields
     * @return BookingDto
     */
    BookingDto toDto(Booking booking);

    /**
     * Entity -> list dto (lightweight)
     * @return BookingListDto
     */
    BookingListDto toListDto(Booking booking);

    /**
     * Batch conversion
     */
    List<BookingDto> toDtoList(List<Booking> bookings);
    List<BookingListDto> toListDtoList(List<Booking> bookings);

    /**
     * BookingItem conversion
     */
    BookingItemDto toItemDto(BookingItem item);
    List<BookingItemDto> toItemDtoList(List<BookingItem> items);

    /*
        calculate computed fields after mapping
     */
    @AfterMapping
    default void calculateComputedFields(
            @MappingTarget BookingDto dto,
            Booking booking
    ) {
        // calculate seconds until expiry
        if (booking.getExpiresAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = booking.getExpiresAt();

            if (now.isBefore(expiresAt)) {
                long seconds = Duration.between(now, expiresAt).getSeconds();
                dto.setSecondsUntilExpiry(seconds);
            }
            else {
                dto.setSecondsUntilExpiry(0L);
            }
        }

        dto.setIsExpired(booking.isExpired());
    }
}