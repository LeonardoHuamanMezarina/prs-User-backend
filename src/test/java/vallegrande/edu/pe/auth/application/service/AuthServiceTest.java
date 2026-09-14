package vallegrande.edu.pe.auth.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import vallegrande.edu.pe.auth.domain.model.Role;
import vallegrande.edu.pe.auth.domain.model.User;
import vallegrande.edu.pe.auth.domain.port.out.PermissionRepositoryPort;
import vallegrande.edu.pe.auth.domain.port.out.RoleRepositoryPort;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

class AuthServiceTest {
    private RoleRepositoryPort roles;
    private PermissionRepositoryPort permissions;
    private AuthService service;
    private final User user = User.builder().id(10L).roleId(2L).build();

    @BeforeEach
    void setUp() {
        roles = mock(RoleRepositoryPort.class);
        permissions = mock(PermissionRepositoryPort.class);
        service = new AuthService(roles, permissions);
    }

    static Stream<Arguments> moduleScenarios() {
        List<String> parishModules = List.of("sacramentos", "agenda-parroquial", "solicitudes",
                "fianzas-pagos", "voluntariado", "personas", "libros");
        return Stream.of(
                Arguments.of("SUPERADMIN", List.of(), List.of("home", "usuarios", "config")),
                Arguments.of("SUPERADMIN", List.of("usuarios"), List.of("usuarios")),
                Arguments.of("PARROCO", List.of(), parishModules),
                Arguments.of("SECRETARIO", List.of(), parishModules),
                Arguments.of("SECRETARIO", List.of("personas", "libros"), List.of("personas", "libros")),
                Arguments.of("DESCONOCIDO", List.of(), List.of()));
    }

    @ParameterizedTest(name = "{index}: rol={0}, permisos personalizados={1}")
    @MethodSource("moduleScenarios")
    void resolvesModulesByRole(String roleName, List<String> custom, List<String> expected) {
        when(roles.findById(2L)).thenReturn(Mono.just(Role.builder().id(2L).name(roleName).build()));
        if ("SUPERADMIN".equals(roleName)) {
            when(permissions.findPlatformModulesByUserId(10L)).thenReturn(Flux.fromIterable(custom));
        } else if ("SECRETARIO".equals(roleName)) {
            when(permissions.findActiveModulesByUserId(10L)).thenReturn(Flux.fromIterable(custom));
        }

        StepVerifier.create(service.getUserModules(user)).expectNext(expected).verifyComplete();

        if ("SUPERADMIN".equals(roleName)) {
            verify(permissions).findPlatformModulesByUserId(10L);
        } else if ("SECRETARIO".equals(roleName)) {
            verify(permissions).findActiveModulesByUserId(10L);
        }
        verifyNoMoreInteractions(permissions);
    }

    @Test
    void missingRoleDoesNotQueryPermissions() {
        when(roles.findById(2L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getUserModules(user)).verifyComplete();
        verifyNoInteractions(permissions);
    }

    @Test
    void propagatesRepositoryFailure() {
        IllegalStateException failure = new IllegalStateException("Repository unavailable");
        when(roles.findById(2L)).thenReturn(Mono.error(failure));
        StepVerifier.create(service.getUserModules(user)).expectErrorMatches(error -> error == failure).verify();
        verifyNoInteractions(permissions);
    }
}
