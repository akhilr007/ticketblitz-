package com.ticketblitz.fulfillment.service;

import com.ticketblitz.common.constant.TicketStatus;
import com.ticketblitz.common.event.BookingConfirmedEvent;
import com.ticketblitz.fulfillment.entity.Ticket;
import com.ticketblitz.fulfillment.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ticket Generation Service — Core fulfillment business logic.
 *
 * PURPOSE:
 * ========
 * Generates one e-ticket per seat when a BookingConfirmedEvent is consumed.
 *
 * IDEMPOTENCY:
 * ============
 * RabbitMQ may redeliver messages (consumer crash, network partition).
 * This service checks existsByBookingId() before generating tickets.
 * If tickets already exist, the operation is a no-op — safe for replay.
 *
 * The database-level UNIQUE(booking_id, seat_id) constraint acts as a
 * second safety net even if the application-level check is bypassed.
 *
 * TICKET GENERATION:
 * ==================
 * 1. Check idempotency (tickets already exist?)
 * 2. Create Ticket entities (GENERATING status)
 * 3. Generate UUID ticket numbers
 * 4. Generate QR code payloads (JSON with ticket metadata)
 * 5. Mark tickets as GENERATED
 * 6. Batch save all tickets
 *
 * PRODUCTION ENHANCEMENTS:
 * ========================
 * - Generate actual PDF tickets (iText / OpenPDF)
 * - Upload PDFs to S3/GCS
 * - Send email with ticket attachments
 * - Generate actual QR code images (ZXing library)
 *
 * @author Akhil
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketGenerationService {

    private final TicketRepository ticketRepository;

    /**
     * Generate tickets for a confirmed booking.
     *
     * @param event The booking confirmed event from RabbitMQ
     * @return List of generated tickets, or existing tickets if already generated
     */
    @Transactional
    public List<Ticket> generateTickets(BookingConfirmedEvent event) {
        log.info("Processing ticket generation for booking: {}, seats: {}",
                event.getBookingId(), event.getTotalSeats());

        // Idempotency check — prevent duplicate generation on redelivery
        if (ticketRepository.existsByBookingId(event.getBookingId())) {
            log.info("Tickets already exist for booking: {}. Skipping generation (idempotent).",
                    event.getBookingId());
            return ticketRepository.findByBookingId(event.getBookingId());
        }

        List<Ticket> tickets = new ArrayList<>(event.getSeats().size());

        for (BookingConfirmedEvent.SeatInfo seat : event.getSeats()) {
            String ticketNumber = generateTicketNumber();
            String qrCode = generateQrPayload(event, seat, ticketNumber);

            Ticket ticket = Ticket.builder()
                    .bookingId(event.getBookingId())
                    .userId(event.getUserId())
                    .eventId(event.getEventId())
                    .eventName(event.getEventName())
                    .venueName(event.getVenueName())
                    .eventDate(event.getEventDate())
                    .seatId(seat.getSeatId())
                    .section(seat.getSection())
                    .rowLabel(seat.getRowLabel())
                    .seatNumber(seat.getSeatNumber())
                    .price(seat.getPrice())
                    .ticketNumber(ticketNumber)
                    .qrCode(qrCode)
                    .status(TicketStatus.GENERATED)
                    .generatedAt(LocalDateTime.now())
                    .build();

            tickets.add(ticket);
        }

        List<Ticket> savedTickets = ticketRepository.saveAll(tickets);

        log.info("Successfully generated {} tickets for booking: {}",
                savedTickets.size(), event.getBookingId());

        return savedTickets;
    }

    /**
     * Generate a unique ticket number (UUID v4).
     *
     * In production, consider a format like: TB-{eventId}-{seatId}-{shortUUID}
     * for human readability while maintaining uniqueness.
     */
    private String generateTicketNumber() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate QR code payload as a JSON string.
     *
     * The QR code encodes enough information for venue entry validation:
     * ticket number, event, seat location, and a validation token.
     *
     * In production, this would be a signed JWT or encrypted payload
     * to prevent ticket forgery.
     */
    private String generateQrPayload(
            BookingConfirmedEvent event,
            BookingConfirmedEvent.SeatInfo seat,
            String ticketNumber
    ) {
        return String.format(
                "{\"ticket\":\"%s\",\"event\":\"%s\",\"venue\":\"%s\",\"seat\":\"%s-%s-%d\",\"date\":\"%s\"}",
                ticketNumber,
                event.getEventName(),
                event.getVenueName(),
                seat.getSection(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                event.getEventDate()
        );
    }
}
