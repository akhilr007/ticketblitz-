package com.ticketblitz.catalog.repository;

import com.ticketblitz.catalog.entity.Venue;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Venue Repository
 *
 * CACHING NOTE:
 * =============
 * Venues are relatively static data (rarely change)
 * Perfect candidate for aggressive caching (1 hour TTL)
 */
@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    /**
     * Find venues by city
     */
    @Query("SELECT v FROM Venue v WHERE v.city = :city")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Venue> findByCity(@Param("city") String city);

    /**
     * Find venues by country
     */
    @Query("SELECT v FROM Venue v WHERE v.country = :country")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Page<Venue> findByCountry(
            @Param("country") String country,
            Pageable pageable
    );

    /**
     * Search venues by name (case-insensitive)
     */
    @Query("SELECT v FROM Venue v " +
            "WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Venue> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Find all venues (cached, read-only)
     */
    @Query("SELECT v FROM Venue v ORDER BY v.name")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Venue> findAllOrderedByName();
}