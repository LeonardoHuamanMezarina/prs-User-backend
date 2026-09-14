package vallegrande.edu.pe.auth.domain.port.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.model.User;

import java.util.List;

public interface UserRepositoryPort {

    Mono<User> findById(Long id);

    Mono<User> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    Flux<User> findAll();

    Flux<User> findByStatus(String status);

    Flux<User> findByTenantId(Long tenantId);

    Flux<User> findByTenantIdAndStatus(Long tenantId, String status);

    Mono<User> save(User user);

    Mono<Void> deleteByIds(List<Long> ids);
}
