package vallegrande.edu.pe.auth.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.application.dto.command.CreateAdminCommand;
import vallegrande.edu.pe.auth.application.dto.command.UpdateUserCommand;
import vallegrande.edu.pe.auth.application.dto.query.UserView;
import vallegrande.edu.pe.auth.domain.port.in.AdminUseCase;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminUseCase adminUseCase;

    @PostMapping("/create")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Mono<ResponseEntity<UserView>> createUser(@Valid @RequestBody CreateAdminCommand command) {
        return adminUseCase.createPrivilegedUser(command)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }

    @PostMapping("/init")
    public Mono<ResponseEntity<UserView>> initSuperAdmin(@Valid @RequestBody CreateAdminCommand command) {
        return adminUseCase.initSuperAdmin(command)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Flux<UserView> listAllUsers() {
        return adminUseCase.getAllUsers();
    }

    @GetMapping("/users/status/{status}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Flux<UserView> listUsersByStatus(@PathVariable String status) {
        return adminUseCase.getUsersByStatus(status);
    }

    @PatchMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Mono<ResponseEntity<UserView>> updateUser(@PathVariable Long id, @RequestBody UpdateUserCommand command) {
        return adminUseCase.updateUser(id, command)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> deleteUser(@PathVariable Long id) {
        return adminUseCase.softDelete(id)
                .map(user -> ResponseEntity.ok(Map.of("message", "Usuario desactivado", "user", user)));
    }

    @PatchMapping("/users/{id}/restore")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> restoreUser(@PathVariable Long id) {
        return adminUseCase.restore(id)
                .map(user -> ResponseEntity.ok(Map.of("message", "Usuario reactivado", "user", user)));
    }

    @GetMapping("/tenants/used")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Flux<Long> getUsedTenantIds() {
        return adminUseCase.getUsedTenantIds();
    }

    @GetMapping("/debug/clean-users")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> cleanUsers() {
        return adminUseCase.cleanUsers(List.of(2L, 3L, 4L))
                .then(Mono.just(ResponseEntity.ok(Map.of("message", "Usuarios 2, 3 y 4 eliminados correctamente"))));
    }
}
