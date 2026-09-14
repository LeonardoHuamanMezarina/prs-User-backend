package vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.model.Role;
import vallegrande.edu.pe.auth.domain.port.out.RoleRepositoryPort;
import vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.entity.RoleEntity;
import vallegrande.edu.pe.auth.infrastructure.adapter.out.persistence.repository.SpringDataRoleRepository;

@Component
@RequiredArgsConstructor
public class RolePersistenceAdapter implements RoleRepositoryPort {

    private final SpringDataRoleRepository springDataRoleRepository;

    @Override
    public Mono<Role> findById(Long id) {
        return springDataRoleRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<Role> findByName(String name) {
        return springDataRoleRepository.findByName(name)
                .map(this::toDomain);
    }

    @Override
    public Flux<Role> findAll() {
        return springDataRoleRepository.findAll()
                .map(this::toDomain);
    }

    private Role toDomain(RoleEntity entity) {
        if (entity == null) return null;
        return Role.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
