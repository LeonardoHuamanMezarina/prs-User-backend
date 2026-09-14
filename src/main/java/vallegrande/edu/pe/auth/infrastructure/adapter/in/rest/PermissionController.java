package vallegrande.edu.pe.auth.infrastructure.adapter.in.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.port.in.PermissionUseCase;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PermissionController {

    private final PermissionUseCase permissionUseCase;

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Flux<Map<String, Object>> listPermissions() {
        return permissionUseCase.getAllPermissions();
    }

    @PostMapping("/users/{userId}/permissions/{permId}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> assignPermission(@PathVariable Long userId, @PathVariable Long permId) {
        return permissionUseCase.assignPermission(userId, permId)
                .then(Mono.just(ResponseEntity.ok(Map.of("message", (Object) "Permiso asignado correctamente"))));
    }

    @DeleteMapping("/users/{userId}/permissions/{permId}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> removePermission(@PathVariable Long userId, @PathVariable Long permId) {
        return permissionUseCase.removePermission(userId, permId)
                .then(Mono.just(ResponseEntity.ok(Map.of("message", (Object) "Permiso revocado correctamente"))));
    }

    @PostMapping("/users/{userId}/permissions/sync")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> syncPermissions(@PathVariable Long userId) {
        return permissionUseCase.syncDefaultPermissions(userId)
                .then(Mono.just(ResponseEntity.ok(Map.of("message", (Object) "Permisos sincronizados. Ahora puedes editarlos uno a uno."))));
    }
}
