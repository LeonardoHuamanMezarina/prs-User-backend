package vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.entity.RoleEntity;

public interface SpringDataRoleRepository extends R2dbcRepository<RoleEntity, Long> {

    Mono<RoleEntity> findByName(String name);
}
