package com.hope.master_service.dto;

import com.hope.master_service.dto.enums.USState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address {

    private UUID uuid;

    @NotBlank(message = "Line1 is mandatory field")
    @Size(max = 255, message = "Line1 should not exceed {max} characters")
    private String line1;

    @Size(max = 255, message = "Line2 should not exceed {max} characters")
    private String line2;

    @NotBlank(message = "City is mandatory field")
    @Pattern(regexp = "^[a-zA-Z]+(?:[\\s-][a-zA-Z]+)*$", message = "Invalid city format")
    @Size(max = 255, message = "city should not exceed {max} characters")
    private String city;

    @NotNull(message = "State is mandatory field")
    private USState state;

    @NotBlank(message = "Country is mandatory field")
    @Pattern(regexp = "^[a-zA-Z]+(?:[\\s-][a-zA-Z]+)*$", message = "Invalid country format")
    @Size(max = 255, message = "country should not exceed {max} characters")
    private String country;

    @NotBlank(message = "Zipcode is a mandatory field")
    @Size(min = 5, max = 10, message = "Zipcode should be between 5 and 10 characters")
    private String zipcode;
}
