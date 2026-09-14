package vallegrande.edu.pe.auth.domain.port.in;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.application.dto.command.CreateAdminCommand;
import vallegrande.edu.pe.auth.application.dto.command.UpdateUserCommand;
import vallegrande.edu.pe.auth.application.dto.query.UserView;

import java.util.List;

public interface AdminUseCase {
    Mono<UserView> initSuperAdmin(CreateAdminCommand command);

    Mono<UserView> createPrivilegedUser(CreateAdminCommand command);

    Flux<UserView> getAllUsers();

    Flux<UserView> getUsersByStatus(String status);

    Mono<UserView> updateUser(Long id, UpdateUserCommand command);

    Mono<UserView> softDelete(Long id);

    Mono<UserView> restore(Long id);

    Flux<Long> getUsedTenantIds();

    Mono<Void> cleanUsers(List<Long> ids);
}
