package com.ticketblitz.booking.mapper;

import com.ticketblitz.booking.dto.PaymentDto;
import com.ticketblitz.booking.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PaymentMapper {

    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "status", target = "status")
    PaymentDto toDto(Payment payment);

    List<PaymentDto> toDtoList(List<Payment> payments);
}