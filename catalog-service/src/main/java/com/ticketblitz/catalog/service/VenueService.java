package com.ticketblitz.catalog.service;

import com.ticketblitz.catalog.config.CacheConfig;
import com.ticketblitz.catalog.dto.VenueDto;
import com.ticketblitz.catalog.entity.Venue;
import com.ticketblitz.catalog.mapper.VenueMapper;
import com.ticketblitz.catalog.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Venue Service
 *
 * CACHING STRATEGY:
 * =================
 * Venues are STATIC data (rarely change)
 * Aggressive caching: 1 hour TTL
 * Perfect candidate for preloading (cache warming)
 *
 * @author Akhil
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueService {
    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    /**
     * Get venue by ID
     *
     * CACHING: 1 hour TTL (venues rarely change)
     */
    @Cacheable(
            value = CacheConfig.VENUE_CACHE,
            key = "#venueId",
            unless = "#result == null"
    )
    public VenueDto getVenueById(Long venueId) {
        log.debug("Fetching venue with ID: {}", venueId);

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException(
                        "Venue not found with ID: " + venueId
                ));

        return venueMapper.toDto(venue);
    }

    /**
     * Get all venues
     *
     * This endpoint returns ALL venues (no pagination)
     * Safe because venue count is small (< 100 in most systems)
     */
    @Cacheable(
            value = CacheConfig.VENUE_CACHE,
            key = "'all'"
    )
    public List<VenueDto> getAllVenues() {
        log.debug("Fetching all venues");

        List<Venue> venues = venueRepository.findAllOrderedByName();
        return venueMapper.toDtoList(venues);
    }

    /**
     * Get venues by city
     */
    @Cacheable(
            value = CacheConfig.VENUE_CACHE,
            key = "'city:' + #city"
    )
    public List<VenueDto> getVenuesByCity(String city) {
        log.debug("Fetching venues in city: {}", city);

        List<Venue> venues = venueRepository.findByCity(city);
        return venueMapper.toDtoList(venues);
    }

    /**
     * Search venues by name
     */
    @Cacheable(
            value = CacheConfig.VENUE_CACHE,
            key = "'search:' + #searchTerm"
    )
    public List<VenueDto> searchVenuesByName(String searchTerm) {
        log.debug("Searching venues with term: {}", searchTerm);

        List<Venue> venues = venueRepository.searchByName(searchTerm);
        return venueMapper.toDtoList(venues);
    }
}