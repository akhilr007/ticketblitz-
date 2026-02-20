package com.ticketblitz.catalog.mapper;

import com.ticketblitz.catalog.dto.VenueDto;
import com.ticketblitz.catalog.entity.Venue;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface VenueMapper {

    VenueDto toDto(Venue venue);

    List<VenueDto> toDtoList(List<Venue> venues);
}