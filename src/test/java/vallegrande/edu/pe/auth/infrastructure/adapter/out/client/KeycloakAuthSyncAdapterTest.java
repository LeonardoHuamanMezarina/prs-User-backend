package vallegrande.edu.pe.auth.infrastructure.adapter.out.client;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KeycloakAuthSyncAdapterTest {

    private Keycloak keycloak;
    private UsersResource users;
    private Response response;
    private KeycloakAuthSyncAdapter adapter;

    @BeforeEach
    void setUp() {
        // Solo mocks: no inicia Spring ni conecta con proveedores o bases de datos.
        keycloak = mock(Keycloak.class, RETURNS_DEEP_STUBS);
        users = keycloak.realm("prs-realm").users();
        response = mock(Response.class);
        when(users.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("https://example.com/users/user-123"));
        adapter = new KeycloakAuthSyncAdapter(keycloak);
    }

    @Test
    void createsSecretaryWithTenantPasswordAndRole() {
        RoleRepresentation role = new RoleRepresentation();
        role.setName("SECRETARIO");
        when(keycloak.realm("prs-realm").roles().get("SECRETARIO").toRepresentation()).thenReturn(role);

        StepVerifier.create(adapter.syncUserToIdentityProvider(
                "secretario@example.com", "test-password", "Ana", "Prueba", "SECRETARIO", 7L))
                .verifyComplete();

        ArgumentCaptor<UserRepresentation> user = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(user.capture());
        assertEquals("secretario@example.com", user.getValue().getUsername());
        assertEquals("secretario@example.com", user.getValue().getEmail());
        assertEquals("Ana", user.getValue().getFirstName());
        assertEquals("Prueba", user.getValue().getLastName());
        assertEquals(List.of("7"), user.getValue().getAttributes().get("tenantId"));

        ArgumentCaptor<CredentialRepresentation> password = ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(users.get("user-123")).resetPassword(password.capture());
        assertEquals(CredentialRepresentation.PASSWORD, password.getValue().getType());
        assertEquals("test-password", password.getValue().getValue());
        verify(users.get("user-123").roles().realmLevel()).add(List.of(role));
    }

    @Test
    void createsSuperAdminWithoutTenant() {
        RoleRepresentation role = new RoleRepresentation();
        role.setName("SUPERADMIN");
        when(keycloak.realm("prs-realm").roles().get("SUPERADMIN").toRepresentation()).thenReturn(role);

        StepVerifier.create(adapter.syncUserToIdentityProvider(
                "admin@example.com", "test-password", "Admin", "Prueba", "SUPERADMIN", null))
                .verifyComplete();

        ArgumentCaptor<UserRepresentation> user = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(user.capture());
        assertTrue(user.getValue().getAttributes() == null || user.getValue().getAttributes().isEmpty());
        verify(users.get("user-123").roles().realmLevel()).add(List.of(role));
    }

    @Test
    void propagatesKeycloakFailureWithoutContinuingProvisioning() {
        when(response.getStatus()).thenReturn(409);
        when(response.readEntity(String.class)).thenReturn("User already exists");

        StepVerifier.create(adapter.syncUserToIdentityProvider(
                "existing@example.com", "test-password", "Ana", "Prueba", "SECRETARIO", 7L))
                .expectErrorMatches(error -> error instanceof RuntimeException
                        && error.getMessage().contains("409"))
                .verify();

        verify(users, never()).get(anyString());
    }
}
