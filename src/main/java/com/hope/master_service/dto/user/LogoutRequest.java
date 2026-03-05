package com.hope.master_service.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogoutRequest {

    @NotBlank(message = "Refresh token is mandatory")
    @JsonProperty("refresh_token")
    private String refreshToken;
}
