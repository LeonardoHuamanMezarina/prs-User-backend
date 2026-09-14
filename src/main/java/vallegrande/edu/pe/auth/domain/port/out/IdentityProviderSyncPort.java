package vallegrande.edu.pe.auth.domain.port.out;

import reactor.core.publisher.Mono;

public interface IdentityProviderSyncPort {
    Mono<Void> syncUserToIdentityProvider(String email, String password, String firstName, String lastName, String roleName, Long tenantId);
}
