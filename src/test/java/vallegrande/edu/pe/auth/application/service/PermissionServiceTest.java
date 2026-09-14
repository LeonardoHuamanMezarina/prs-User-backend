package vallegrande.edu.pe.auth.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import vallegrande.edu.pe.auth.domain.exception.BusinessException;
import vallegrande.edu.pe.auth.domain.exception.UserNotFoundException;
import vallegrande.edu.pe.auth.domain.model.Role;
import vallegrande.edu.pe.auth.domain.model.User;
import vallegrande.edu.pe.auth.domain.port.out.PermissionRepositoryPort;
import vallegrande.edu.pe.auth.domain.port.out.RoleRepositoryPort;
import vallegrande.edu.pe.auth.domain.port.out.UserRepositoryPort;

import static org.mockito.Mockito.*;

class PermissionServiceTest {
    private PermissionRepositoryPort permissions;
    private UserRepositoryPort users;
    private RoleRepositoryPort roles;
    private PermissionService service;

    @BeforeEach
    void setUp() {
        permissions = mock(PermissionRepositoryPort.class);
        users = mock(UserRepositoryPort.class);
        roles = mock(RoleRepositoryPort.class);
        service = new PermissionService(permissions, users, roles);
    }

    @ParameterizedTest(name = "{0} permite rol {1}")
    @CsvSource({"assign,SECRETARIO", "remove,SECRETARIO", "tenant,SECRETARIO", "platform,SUPERADMIN"})
    void allowsExpectedRole(String operation, String role) {
        stubUserRole(role);
        switch (operation) {
            case "assign" -> when(permissions.assignPermission(10L, 20L)).thenReturn(Mono.empty());
            case "remove" -> when(permissions.removePermission(10L, 20L)).thenReturn(Mono.empty());
            case "tenant" -> when(permissions.syncDefaultPermissions(10L)).thenReturn(Mono.empty());
            case "platform" -> when(permissions.syncPlatformPermissions(10L)).thenReturn(Mono.empty());
            default -> throw new IllegalArgumentException(operation);
        }
        StepVerifier.create(execute(operation)).verifyComplete();
        switch (operation) {
            case "assign" -> verify(permissions).assignPermission(10L, 20L);
            case "remove" -> verify(permissions).removePermission(10L, 20L);
            case "tenant" -> verify(permissions).syncDefaultPermissions(10L);
            case "platform" -> verify(permissions).syncPlatformPermissions(10L);
            default -> throw new IllegalArgumentException(operation);
        }
        verifyNoMoreInteractions(permissions);
    }

    @ParameterizedTest(name = "{0} rechaza rol {1}")
    @CsvSource({"assign,PARROCO", "assign,SUPERADMIN", "remove,PARROCO", "remove,SUPERADMIN",
            "tenant,PARROCO", "tenant,SUPERADMIN", "platform,SECRETARIO", "platform,PARROCO"})
    void rejectsOtherRolesWithoutChangingPermissions(String operation, String role) {
        stubUserRole(role);
        StepVerifier.create(execute(operation)).expectError(BusinessException.class).verify();
        verifyNoInteractions(permissions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"assign", "remove", "tenant", "platform"})
    void rejectsMissingUser(String operation) {
        when(users.findById(10L)).thenReturn(Mono.empty());
        StepVerifier.create(execute(operation)).expectError(UserNotFoundException.class).verify();
        verifyNoInteractions(roles, permissions);
    }

    private void stubUserRole(String role) {
        when(users.findById(10L)).thenReturn(Mono.just(User.builder().id(10L).roleId(2L).build()));
        when(roles.findById(2L)).thenReturn(Mono.just(Role.builder().id(2L).name(role).build()));
    }

    private Mono<Void> execute(String operation) {
        return switch (operation) {
            case "assign" -> service.assignPermission(10L, 20L);
            case "remove" -> service.removePermission(10L, 20L);
            case "tenant" -> service.syncDefaultPermissions(10L);
            case "platform" -> service.syncPlatformPermissions(10L);
            default -> throw new IllegalArgumentException(operation);
        };
    }
}
