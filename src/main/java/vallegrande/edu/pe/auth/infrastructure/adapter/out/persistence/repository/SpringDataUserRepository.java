package vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.entity.UserEntity;

public interface SpringDataUserRepository extends R2dbcRepository<UserEntity, Long> {

    Mono<UserEntity> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    Flux<UserEntity> findByStatus(String status);

    Flux<UserEntity> findByTenantId(Long tenantId);

    Flux<UserEntity> findByTenantIdAndStatus(Long tenantId, String status);
}
