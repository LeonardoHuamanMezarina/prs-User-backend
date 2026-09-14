package vallegrande.edu.pe.auth.domain.port.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface PermissionRepositoryPort {

    Flux<Map<String, Object>> findAllPermissions();

    Flux<Map<String, Object>> findUserTenantPermissions(Long userId);

    Mono<Void> assignPermission(Long userId, Long permissionId);

    Mono<Void> removePermission(Long userId, Long permissionId);

    Mono<Void> syncDefaultPermissions(Long userId);

    Mono<Void> syncPlatformPermissions(Long userId);

    Flux<String> findActiveModulesByUserId(Long userId);

    Flux<String> findPlatformModulesByUserId(Long userId);
}
