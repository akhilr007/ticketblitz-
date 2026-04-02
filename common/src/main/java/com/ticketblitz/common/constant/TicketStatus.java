package com.ticketblitz.common.constant;

/**
 * TICKET LIFECYCLE
 *
 * GENERATING → Ticket creation in progress
 * GENERATED  → Ticket ready for delivery
 * DELIVERED  → Ticket sent to user (email/download)
 * CANCELLED  → Ticket voided (booking cancelled after fulfillment)
 *
 * @author Akhil
 */
public enum TicketStatus {
    GENERATING,
    GENERATED,
    DELIVERED,
    CANCELLED
}
