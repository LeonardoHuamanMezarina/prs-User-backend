package vallegrande.edu.pe.auth.domain.port.in;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface PermissionUseCase {
    Flux<Map<String, Object>> getAllPermissions();

    Flux<Map<String, Object>> getUserTenantPermissions(Long userId);

    Mono<Void> assignPermission(Long userId, Long permissionId);

    Mono<Void> removePermission(Long userId, Long permissionId);

    Mono<Void> syncDefaultPermissions(Long userId);

    Mono<Void> syncPlatformPermissions(Long userId);
}
