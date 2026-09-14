package vallegrande.edu.pe.auth.infrastructure.adapter.out.client;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.port.out.TenantClientPort;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TenantClientAdapter implements TenantClientPort {

    private final WebClient webClient;

    @Override
    public Mono<Void> activateTenant(Long tenantId) {
        String url = "http://ms-tenant:8081/api/v1/tenants/" + tenantId;

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    String token = auth.getToken().getTokenValue();
                    return webClient.patch()
                            .uri(url)
                            .header("Authorization", "Bearer " + token)
                            .bodyValue(Map.of("statusId", 1))
                            .retrieve()
                            .bodyToMono(Void.class)
                            .doOnSuccess(res -> System.out.println("LOG: Tenant " + tenantId
                                    + " estado cambiado a ACTIVO a través de TenantClientAdapter."))
                            .doOnError(err -> System.err
                                    .println("ERROR: Falló el cambio de estado del tenant " + tenantId
                                            + ". Causa: " + err.getMessage()))
                            .onErrorResume(err -> Mono.error(new RuntimeException(
                                    "No se pudo activar el tenant " + tenantId + ": " + err.getMessage())));
                });
    }
}
