package com.ticketblitz.catalog.mapper;

import com.ticketblitz.catalog.dto.SeatDto;
import com.ticketblitz.catalog.entity.Seat;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface SeatMapper {

    SeatDto toDto(Seat seat);

    List<SeatDto> toDtoList(List<Seat> seats);
}