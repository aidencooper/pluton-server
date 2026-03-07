package net.aidencooper.pluton.server.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @Size(min = 8) String password,
        @NotBlank String username) {
}
