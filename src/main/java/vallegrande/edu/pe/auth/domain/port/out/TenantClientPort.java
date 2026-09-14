package vallegrande.edu.pe.auth.domain.port.out;

import reactor.core.publisher.Mono;

public interface TenantClientPort {
    Mono<Void> activateTenant(Long tenantId);
}
