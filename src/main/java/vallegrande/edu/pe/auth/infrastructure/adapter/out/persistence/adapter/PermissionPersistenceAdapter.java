package vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.port.out.PermissionRepositoryPort;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PermissionPersistenceAdapter implements PermissionRepositoryPort {

    private final DatabaseClient databaseClient;

    @Override
    public Flux<Map<String, Object>> findAllPermissions() {
        return databaseClient.sql("SELECT id, name, module, description FROM permissions ORDER BY id ASC")
                .map((row, metadata) -> Map.of(
                        "id", row.get("id") != null ? row.get("id") : 0,
                        "name", row.get("name") != null ? row.get("name") : "",
                        "module", row.get("module") != null ? row.get("module") : "",
                        "description", row.get("description") != null ? row.get("description") : ""))
                .all();
    }

    @Override
    public Flux<Map<String, Object>> findUserTenantPermissions(Long userId) {
        String sql = "SELECT p.id, p.name, p.module, p.description, COALESCE(up.status, 'INACTIVE') as status " +
                "FROM permissions p " +
                "LEFT JOIN user_permissions up ON p.id = up.permission_id AND up.user_id = :userId " +
                "WHERE p.name LIKE 'TEN_%' " +
                "ORDER BY p.id ASC";
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .map((row, metadata) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", row.get("id") != null ? row.get("id") : 0);
                    map.put("name", row.get("name") != null ? row.get("name") : "");
                    map.put("module", row.get("module") != null ? row.get("module") : "");
                    map.put("description", row.get("description") != null ? row.get("description") : "");
                    map.put("status", row.get("status") != null ? row.get("status") : "INACTIVE");
                    return map;
                })
                .all();
    }

    @Override
    public Mono<Void> assignPermission(Long userId, Long permissionId) {
        String sql = "INSERT INTO user_permissions (user_id, permission_id, status) VALUES (:userId, :permId, 'ACTIVE') "
                + "ON CONFLICT (user_id, permission_id) DO UPDATE SET status = 'ACTIVE'";
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .bind("permId", permissionId)
                .then();
    }

    @Override
    public Mono<Void> removePermission(Long userId, Long permissionId) {
        String sql = "UPDATE user_permissions SET status = 'INACTIVE' WHERE user_id = :userId AND permission_id = :permId";
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .bind("permId", permissionId)
                .then();
    }

    @Override
    public Mono<Void> syncDefaultPermissions(Long userId) {
        String sql = "INSERT INTO user_permissions (user_id, permission_id, status) " +
                "SELECT :userId, id, 'ACTIVE' FROM permissions WHERE name LIKE 'TEN_%' " +
                "ON CONFLICT DO NOTHING";
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .then();
    }

    @Override
    public Mono<Void> syncPlatformPermissions(Long userId) {
        String sql = "INSERT INTO user_permissions (user_id, permission_id, status) " +
                "SELECT :userId, id, 'ACTIVE' FROM permissions WHERE name LIKE 'PLAT_%' " +
                "ON CONFLICT (user_id, permission_id) DO UPDATE SET status = 'ACTIVE'";
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .then();
    }

    @Override
    public Flux<String> findActiveModulesByUserId(Long userId) {
        String sql = "SELECT p.module FROM permissions p " +
                "JOIN user_permissions up ON p.id = up.permission_id " +
                "WHERE up.user_id = :userId AND up.status = 'ACTIVE'";
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .map((row, metadata) -> row.get("module", String.class))
                .all();
    }

    @Override
    public Flux<String> findPlatformModulesByUserId(Long userId) {
        String sql = "SELECT p.module FROM permissions p " +
                "JOIN user_permissions up ON p.id = up.permission_id " +
                "WHERE up.user_id = :userId AND up.status = 'ACTIVE' AND p.name LIKE 'PLAT_%'";
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .map((row, metadata) -> row.get("module", String.class))
                .all();
    }
}
