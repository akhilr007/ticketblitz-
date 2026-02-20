package com.ticketblitz.catalog.repository.specification;

import com.ticketblitz.catalog.entity.Event;
import com.ticketblitz.catalog.entity.Venue;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Event specification
 *
 * Specification patter
 * ======================
 * Encapsulates business rules as reusable, combinable specifications.
 * Allows building complex queries dynamically without string concatenation
 *
 * BENEFITS:
 * ==========
 * 1. Type-safe query building (compile time checking)
 * 2. Reusable specifications (DRY Principle)
 * 3. Composable (AND/OR combinations)
 * 4. Testable in isolation
 * 5. No SQL injection risk
 *
 * USAGE EXAMPLE:
 * Specification<Event> spec = Specification.
 * where(hasCategory(CONCERT))
 * .and(hasStatus(ACTIVE))
 * .and(isUpcoming())
 * .and(inCity("NEW YORK"))
 *
 * Page<Event> events = eventRepsoitory.findAll(spec, pageable);
 *
 * PERFORMANCE:
 * ============
 * 1. Generates efficient SQL with proper JOINS
 * 2. Index friendly predicates
 * 3. No N+1 query problems
 *
 * @Author Akhil
 */
public class EventSpecification {
    /**
     * Filter by category
     */
    public static Specification<Event> hasCategory(Event.EventCategory category) {
        return (root, query, cb) -> {
            if (category == null) {
                return cb.conjunction(); // always true(no filters)
            }
            return cb.equal(root.get("category"), category);
        };
    }

    /**
     * Filter by status
     */
    public static Specification<Event> hasStatus(Event.EventStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    /**
     * Filter by upcoming events (after current time)
     */
    public static Specification<Event> isUpcoming() {
        return (root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("eventDate"), LocalDateTime.now());
    }

    /**
     * Filter by date range
     */
    public static Specification<Event> isBetweenDates(
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return (root, query, cb) -> {
            if (startDate == null || endDate == null) {
                return cb.conjunction();
            }
            return cb.between(root.get("eventDate"), startDate, endDate);
        };
    }

    /**
     * Filter by venue id
     */
    public static Specification<Event> hasVenueId(Long venueId) {
        return (root, query, cb) -> {
            if (venueId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("venue").get("id"), venueId);
        };
    }

    /**
     * Filter by city (through venue JOIN)
     */
    public static Specification<Event> inCity(String city) {
        return (root, query, cb) -> {
            if (city == null || city.isBlank()) {
                return cb.conjunction();
            }
            Join<Event, Venue> venueJoin = root.join("venue", JoinType.INNER);
            return cb.equal(
                    cb.lower(venueJoin.get("city")),
                    city.toLowerCase()
            );
        };
    }

    /**
     * Search by name (case-insensitive LIKE)
     */
    public static Specification<Event> nameLike(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            return cb.like(
                    cb.lower(root.get("name")),
                    pattern
            );
        };
    }

    /**
     * Filter by minimum available seats
     */
    public static Specification<Event> hasMinimumSeats(Integer minSeats) {
        return (root, query, cb) -> {
            if (minSeats == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(
                    root.get("availableSeats"),
                    minSeats
            );
        };
    }

    /**
     * Filter by price range
     */
    public static Specification<Event> priceRange(
            Double minPrice,
            Double maxPrice) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (minPrice != null) {
                predicate = cb.and(
                        predicate,
                        cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice)
                );
            }

            if (maxPrice != null) {
                predicate = cb.and(
                        predicate,
                        cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice)
                );
            }

            return predicate;
        };
    }

    /**
     * Combine multiple filters (Helper method)
     *
     * USAGE:
     * Specification<Event> spec = EventSpecification.buildSpecification(
     *     category, status, city, searchTerm, minSeats
     * );
     */
    public static Specification<Event> buildSpecification(
            Event.EventCategory category,
            Event.EventStatus status,
            String city,
            String searchTerm,
            Integer minSeats,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return Specification
                .where(hasCategory(category))
                .and(hasStatus(status))
                .and(inCity(city))
                .and(nameLike(searchTerm))
                .and(hasMinimumSeats(minSeats))
                .and(isBetweenDates(startDate, endDate));
    }

}