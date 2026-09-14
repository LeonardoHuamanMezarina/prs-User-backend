package vallegrande.edu.pe.auth.domain.port.in;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.application.dto.command.CreateAdminCommand;
import vallegrande.edu.pe.auth.application.dto.command.UpdateUserCommand;
import vallegrande.edu.pe.auth.application.dto.query.UserView;

public interface UserUseCase {
    Flux<UserView> getUsersByTenant(Long tenantId);

    Mono<UserView> createSecretary(CreateAdminCommand command, Long parrocoTenantId);

    Mono<UserView> getUserById(Long id);

    Mono<UserView> updateUser(Long id, UpdateUserCommand command);

    Mono<UserView> softDelete(Long id);

    Mono<Void> syncDefaultPermissions(Long userId);
}
