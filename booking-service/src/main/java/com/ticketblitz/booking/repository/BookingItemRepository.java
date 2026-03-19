package com.ticketblitz.booking.repository;

import com.ticketblitz.booking.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
    // Managed via Booking cascade operations
}