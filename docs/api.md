# 🔌 TicketBlitz API Overview

*For fully interactive documentation, boot the project and visit [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).*

## 1. Catalog Service Endpoints (Public)
> **Note:** Routed through API Gateway at `/api/v1/events`

### Get Active Events
`GET /api/v1/events`
Returns a paginated list of active events ready for booking.

### Get Event Seat Layout
`GET /api/v1/events/{eventId}/seats`
Returns seating layout and dynamic pricing availability for a specific event.

## 2. Booking Service Endpoints (Protected)
> **Note:** All requests must include `Authorization: Bearer <JWT_TOKEN>`

### Create Booking
`POST /api/v1/bookings`
Initiates a distributed seat lock and books the specified seats.

**Payload:**
```json
{
  "eventId": 100,
  "seatIds": [54, 55],
  "idempotencyKey": "4f1883be-2c83-4a11-80fc-566b579101d2"
}
```
**Response (200 OK):**
```json
{
  "bookingId": "B-912384",
  "status": "PENDING",
  "amount": 150.00,
  "expiresAt": "2026-04-04T12:00:00Z"
}
```

## 3. Fulfillment Service Endpoints (Protected)
### Download Ticket PDF
`GET /api/v1/tickets/{ticketId}/download`
Returns the PDF binary stream for a confirmed ticket.
