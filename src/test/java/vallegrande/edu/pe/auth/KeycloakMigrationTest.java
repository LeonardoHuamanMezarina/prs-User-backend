package vallegrande.edu.pe.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vallegrande.edu.pe.auth.domain.model.Role;
import vallegrande.edu.pe.auth.domain.model.User;
import vallegrande.edu.pe.auth.domain.port.out.RoleRepositoryPort;
import vallegrande.edu.pe.auth.domain.port.out.UserRepositoryPort;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@EnabledIfSystemProperty(named = "keycloak.migration.enabled", matches = "true")
public class KeycloakMigrationTest {

    @Autowired
    private UserRepositoryPort userRepository;

    @Autowired
    private RoleRepositoryPort roleRepository;

    @Test
    public void generateKeycloakImportJson() throws Exception {
        // Cargar todos los roles para mapear roleId -> roleName
        Map<Long, String> roleMap = roleRepository.findAll()
                .collectMap(Role::getId, Role::getName)
                .block();

        List<User> users = userRepository.findAll().collectList().block();
        List<Map<String, Object>> keycloakUsers = new ArrayList<>();

        if (users != null && roleMap != null) {
            for (User user : users) {
                Map<String, Object> kcUser = new HashMap<>();
                kcUser.put("username", user.getEmail());
                kcUser.put("email", user.getEmail());
                kcUser.put("emailVerified", true);
                kcUser.put("firstName", user.getName());
                kcUser.put("lastName", user.getLastname());
                kcUser.put("enabled", "ACTIVE".equals(user.getStatus()));

                // Credenciales (bcrypt hash)
                List<Map<String, Object>> credentials = new ArrayList<>();
                Map<String, Object> cred = new HashMap<>();
                cred.put("type", "password");
                cred.put("algorithm", "bcrypt");
                cred.put("hashedSaltedValue", user.getPasswordHash());
                cred.put("hashIterations", 10);
                credentials.add(cred);
                kcUser.put("credentials", credentials);

                // Atributos personalizados (como tenantId, dni, phone, etc.)
                Map<String, List<String>> attributes = new HashMap<>();
                attributes.put("tenantId", List.of(user.getTenantId() != null ? user.getTenantId().toString() : ""));
                attributes.put("phone", List.of(user.getPhone() != null ? user.getPhone() : ""));
                attributes.put("dni", List.of(user.getDni() != null ? user.getDni() : ""));
                attributes.put("localUserId", List.of(user.getId().toString()));
                kcUser.put("attributes", attributes);

                // Roles asociados
                String roleName = roleMap.get(user.getRoleId());
                if (roleName != null) {
                    kcUser.put("realmRoles", List.of(roleName.toUpperCase()));
                }

                keycloakUsers.add(kcUser);
            }
        }

        // Guardar a un archivo JSON en la raíz del proyecto
        ObjectMapper mapper = new ObjectMapper();
        File outputFile = new File("keycloak-users.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, keycloakUsers);

        System.out.println("=========================================================");
        System.out.println("MIGRACIÓN EXITOSA: Se ha generado el archivo de importación!");
        System.out.println("Ubicación: " + outputFile.getAbsolutePath());
        System.out.println("Total de usuarios mapeados: " + keycloakUsers.size());
        System.out.println("=========================================================");
    }
}
