package net.aidencooper.pluton.server.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.aidencooper.pluton.server.auth.request.RegisterRequest;
import net.aidencooper.pluton.server.exception.EmailTakenException;
import net.aidencooper.pluton.server.exception.InvalidPasswordException;
import net.aidencooper.pluton.server.constant.Responses;
import net.aidencooper.pluton.server.exception.UsernameTakenException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "Registers a new user",

            responses = {
                    @ApiResponse(
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "string")
                            ),
                            description = Responses.Success.REGISTERED,
                            responseCode = "201"
                    ),
                    @ApiResponse(
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "string")
                            ),
                            description = Responses.Error.EMAIL_TAKEN,
                            responseCode = "409"
                    ),
                    @ApiResponse(
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "string")
                            ),
                            description = Responses.Error.USERNAME_TAKEN,
                            responseCode = "409"
                    ),
                    @ApiResponse(
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "string")
                            ),
                            description = Responses.Error.INVALID_PASSWORD,
                            responseCode = "422"
                    )
            }
    )
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            this.authService.register(registerRequest);
        } catch (EmailTakenException exception) {
            return ResponseEntity.status(409).body(Responses.Error.EMAIL_TAKEN);
        } catch (UsernameTakenException exception) {
            return ResponseEntity.status(409).body(Responses.Error.USERNAME_TAKEN);
        } catch (InvalidPasswordException exception) {
            return ResponseEntity.status(422).body(Responses.Error.INVALID_PASSWORD);
        }

        return ResponseEntity.status(201).body(Responses.Success.REGISTERED);
    }
}
