package vallegrande.edu.pe.auth.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.application.dto.command.CreateAdminCommand;
import vallegrande.edu.pe.auth.application.dto.command.UpdateUserCommand;
import vallegrande.edu.pe.auth.application.dto.query.UserView;
import vallegrande.edu.pe.auth.domain.exception.BusinessException;
import vallegrande.edu.pe.auth.domain.port.in.PermissionUseCase;
import vallegrande.edu.pe.auth.domain.port.in.UserUseCase;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserUseCase userUseCase;
    private final PermissionUseCase permissionUseCase;

    private Long extractTenantId(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Object tenantIdObj = jwtAuth.getTokenAttributes().get("tenantId");
            if (tenantIdObj != null) {
                return Long.valueOf(tenantIdObj.toString());
            }
        }
        return null;
    }

    @GetMapping("/my-tenant")
    @PreAuthorize("hasRole('PARROCO') or hasRole('SECRETARIO')")
    public Flux<UserView> listMyTenantUsers(Authentication auth) {
        Long tenantId = extractTenantId(auth);
        if (tenantId == null) {
            return Flux.error(new BusinessException("No se encontró Tenant ID en el token"));
        }
        return userUseCase.getUsersByTenant(tenantId);
    }

    @PostMapping("/secretary")
    @PreAuthorize("hasRole('PARROCO')")
    public Mono<ResponseEntity<UserView>> createSecretary(@Valid @RequestBody CreateAdminCommand command,
            Authentication auth) {
        Long tenantId = extractTenantId(auth);
        return userUseCase.createSecretary(command, tenantId)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('PARROCO')")
    public Mono<ResponseEntity<Object>> updateMyUser(@PathVariable Long id, @RequestBody UpdateUserCommand command,
            Authentication auth) {
        Long myTenantId = extractTenantId(auth);

        return userUseCase.getUserById(id)
                .flatMap(userToUpdate -> {
                    if (!userToUpdate.getTenantId().equals(myTenantId)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body((Object) Map.of("error", "No puedes editar usuarios de otros tenants")));
                    }

                    if (command.getRole() != null && !"SECRETARIO".equals(command.getRole())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body((Object) Map.of("error", "Solo puedes asignar el rol SECRETARIO")));
                    }

                    return userUseCase.updateUser(id, command)
                            .map(updated -> ResponseEntity.ok((Object) updated));
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PARROCO')")
    public Mono<ResponseEntity<Object>> deleteMyUser(@PathVariable Long id, Authentication auth) {
        Long myTenantId = extractTenantId(auth);

        return userUseCase.getUserById(id)
                .flatMap(userToDelete -> {
                    if (!userToDelete.getTenantId().equals(myTenantId)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body((Object) Map.of("error", "No puedes eliminar usuarios de otros tenants")));
                    }

                    return userUseCase.softDelete(id)
                            .map(deleted -> ResponseEntity
                                    .ok((Object) Map.of("message", "Usuario desactivado", "user", deleted)));
                });
    }

    @GetMapping("/{userId}/permissions")
    @PreAuthorize("hasRole('PARROCO')")
    public Flux<Map<String, Object>> listUserPermissions(@PathVariable Long userId, Authentication auth) {
        Long myTenantId = extractTenantId(auth);

        return userUseCase.getUserById(userId)
                .flatMapMany(user -> {
                    if (!user.getTenantId().equals(myTenantId)) {
                        return Flux.error(new BusinessException("No puedes ver permisos de usuarios de otros tenants"));
                    }
                    return permissionUseCase.getUserTenantPermissions(userId);
                });
    }

    @PostMapping("/{userId}/permissions/{permId}")
    @PreAuthorize("hasRole('PARROCO')")
    public Mono<ResponseEntity<Map<String, Object>>> assignPermission(@PathVariable Long userId,
            @PathVariable Long permId, Authentication auth) {
        Long myTenantId = extractTenantId(auth);

        return userUseCase.getUserById(userId)
                .flatMap(user -> {
                    if (!user.getTenantId().equals(myTenantId)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error",
                                        (Object) "No puedes asignar permisos a usuarios de otros tenants")));
                    }
                    return permissionUseCase.assignPermission(userId, permId)
                            .then(Mono.just(
                                    ResponseEntity.ok(Map.of("message", (Object) "Permiso asignado correctamente"))));
                });
    }

    @DeleteMapping("/{userId}/permissions/{permId}")
    @PreAuthorize("hasRole('PARROCO')")
    public Mono<ResponseEntity<Map<String, Object>>> removePermission(@PathVariable Long userId,
            @PathVariable Long permId, Authentication auth) {
        Long myTenantId = extractTenantId(auth);

        return userUseCase.getUserById(userId)
                .flatMap(user -> {
                    if (!user.getTenantId().equals(myTenantId)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error",
                                        (Object) "No puedes revocar permisos a usuarios de otros tenants")));
                    }
                    return permissionUseCase.removePermission(userId, permId)
                            .then(Mono.just(
                                    ResponseEntity.ok(Map.of("message", (Object) "Permiso revocado correctamente"))));
                });
    }
}
