package net.aidencooper.pluton.server.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(@Email @NotBlank String email, @NotBlank String password) {}
