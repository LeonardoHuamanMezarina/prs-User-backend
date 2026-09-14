package vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.model.User;
import vallegrande.edu.pe.auth.domain.port.out.UserRepositoryPort;
import vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.entity.UserEntity;
import vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<User> findById(Long id) {
        return springDataUserRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    @Override
    public Flux<User> findAll() {
        return springDataUserRepository.findAll()
                .map(this::toDomain);
    }

    @Override
    public Flux<User> findByStatus(String status) {
        return springDataUserRepository.findByStatus(status)
                .map(this::toDomain);
    }

    @Override
    public Flux<User> findByTenantId(Long tenantId) {
        return springDataUserRepository.findByTenantId(tenantId)
                .map(this::toDomain);
    }

    @Override
    public Flux<User> findByTenantIdAndStatus(Long tenantId, String status) {
        return springDataUserRepository.findByTenantIdAndStatus(tenantId, status)
                .map(this::toDomain);
    }

    @Override
    public Mono<User> save(User user) {
        UserEntity entity = toEntity(user);
        return springDataUserRepository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Mono.empty();
        }
        String sql = "DELETE FROM users WHERE id IN (:ids)";
        return databaseClient.sql(sql)
                .bind("ids", ids)
                .then();
    }

    private User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .name(entity.getName())
                .lastname(entity.getLastname())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .dni(entity.getDni())
                .passwordHash(entity.getPasswordHash())
                .status(entity.getStatus())
                .roleId(entity.getRoleId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserEntity toEntity(User domain) {
        if (domain == null) return null;
        return UserEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .name(domain.getName())
                .lastname(domain.getLastname())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .dni(domain.getDni())
                .passwordHash(domain.getPasswordHash())
                .status(domain.getStatus())
                .roleId(domain.getRoleId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
