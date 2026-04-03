package com.ticketblitz.fulfillment.mapper;

import com.ticketblitz.fulfillment.dto.TicketDto;
import com.ticketblitz.fulfillment.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TicketMapper {

    TicketDto toDto(Ticket ticket);

    List<TicketDto> toDtoList(List<Ticket> tickets);
}
