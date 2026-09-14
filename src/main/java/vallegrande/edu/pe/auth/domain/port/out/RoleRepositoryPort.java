package vallegrande.edu.pe.auth.domain.port.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.model.Role;

public interface RoleRepositoryPort {

    Mono<Role> findById(Long id);

    Mono<Role> findByName(String name);

    Flux<Role> findAll();
}
