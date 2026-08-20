package com.restaurantqr.platform.modules.table.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {
    @NotBlank(message = "Guest name is required")
    private String guestName;

    private String guestPhone;

    @NotBlank(message = "Reservation time is required")
    private String reservationTime;

    @NotNull(message = "Number of guests is required")
    private Integer guestCount;

    private String notes;
}
