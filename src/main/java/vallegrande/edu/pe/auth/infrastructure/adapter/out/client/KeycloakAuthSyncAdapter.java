package vallegrande.edu.pe.auth.infrastructure.adapter.out.client;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.port.out.IdentityProviderSyncPort;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KeycloakAuthSyncAdapter implements IdentityProviderSyncPort {

    private final Keycloak keycloak;
    private final String realm = "prs-realm";

    @Override
    public Mono<Void> syncUserToIdentityProvider(String email, String password, String firstName, String lastName, String roleName, Long tenantId) {
        return Mono.fromCallable(() -> {
            System.out.println("Sincronizando usuario a Keycloak: " + email);

            // Crear usuario y asignar sus credenciales y rol en Keycloak.
            try {
                UserRepresentation user = new UserRepresentation();
                user.setUsername(email);
                user.setEmail(email);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEnabled(true);
                user.setEmailVerified(true);

                if (tenantId != null) {
                    Map<String, List<String>> attributes = new HashMap<>();
                    attributes.put("tenantId", Collections.singletonList(tenantId.toString()));
                    user.setAttributes(attributes);
                }

                Response response = keycloak.realm(realm).users().create(user);

                if (response.getStatus() == 201) {
                    String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

                    // Set Password
                    try {
                        CredentialRepresentation passwordCred = new CredentialRepresentation();
                        passwordCred.setTemporary(false);
                        passwordCred.setType(CredentialRepresentation.PASSWORD);
                        passwordCred.setValue(password);
                        keycloak.realm(realm).users().get(userId).resetPassword(passwordCred);
                    } catch (Exception ex) {
                        throw new RuntimeException("Error al hacer resetPassword para el userId: " + userId + ". Error: " + ex.getMessage());
                    }

                    // Set Role
                    if (roleName != null) {
                        try {
                            RoleRepresentation roleRep = keycloak.realm(realm).roles().get(roleName).toRepresentation();
                            keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(roleRep));
                        } catch (Exception ex) {
                            throw new RuntimeException("Error al asignar rol '" + roleName + "' al userId: " + userId + ". Error: " + ex.getMessage());
                        }
                    }
                    System.out.println("Usuario creado y asignado rol en Keycloak exitosamente.");
                } else {
                    String errorMsg = "Error creando usuario en Keycloak, status: " + response.getStatus() + ", reason: " + response.readEntity(String.class);
                    System.err.println(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            } catch (Exception e) {
                System.err.println("Error interactuando con Keycloak: " + e.getMessage());
                throw new RuntimeException("Fallo en sincronización con Keycloak: " + e.getMessage(), e);
            }

            return null;
        }).then();
    }
}
