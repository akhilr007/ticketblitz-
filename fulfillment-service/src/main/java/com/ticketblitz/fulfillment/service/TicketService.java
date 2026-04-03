package com.ticketblitz.fulfillment.service;

import com.ticketblitz.common.exception.ResourceNotFoundException;
import com.ticketblitz.fulfillment.dto.TicketDto;
import com.ticketblitz.fulfillment.entity.Ticket;
import com.ticketblitz.fulfillment.mapper.TicketMapper;
import com.ticketblitz.fulfillment.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ticket Query Service — read-only operations for the REST API.
 *
 * Separated from TicketGenerationService following CQRS:
 * - TicketGenerationService = write side (event-driven)
 * - TicketService = read side (REST API queries)
 *
 * @author Akhil
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    /**
     * Get all tickets for a booking.
     */
    @Transactional(readOnly = true)
    public List<TicketDto> getTicketsByBookingId(Long bookingId) {
        log.info("Fetching tickets for booking: {}", bookingId);

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        if (tickets.isEmpty()) {
            throw new ResourceNotFoundException("Tickets", bookingId);
        }

        return ticketMapper.toDtoList(tickets);
    }

    /**
     * Get a single ticket by its ticket number (UUID).
     */
    @Transactional(readOnly = true)
    public TicketDto getTicketByTicketNumber(String ticketNumber) {
        log.info("Fetching ticket: {}", ticketNumber);

        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketNumber));

        return ticketMapper.toDto(ticket);
    }

    /**
     * Get all tickets for a user (paginated).
     */
    @Transactional(readOnly = true)
    public Page<TicketDto> getUserTickets(String userId, Pageable pageable) {
        log.info("Fetching tickets for user: {}", userId);

        return ticketRepository.findByUserId(userId, pageable)
                .map(ticketMapper::toDto);
    }
}
