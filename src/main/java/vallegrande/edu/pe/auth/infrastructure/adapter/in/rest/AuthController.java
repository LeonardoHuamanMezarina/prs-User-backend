package vallegrande.edu.pe.auth.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.application.dto.command.CreateAdminCommand;
import vallegrande.edu.pe.auth.application.dto.query.UserView;
import vallegrande.edu.pe.auth.domain.port.in.AdminUseCase;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AdminUseCase adminUseCase;

    @PostMapping("/init")
    public Mono<ResponseEntity<UserView>> initSuperAdmin(@Valid @RequestBody CreateAdminCommand command) {
        return adminUseCase.initSuperAdmin(command)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }
}
