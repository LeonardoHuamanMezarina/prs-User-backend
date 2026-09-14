package vallegrande.edu.pe.auth.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.model.User;
import vallegrande.edu.pe.auth.domain.port.in.AuthUseCase;
import vallegrande.edu.pe.auth.domain.port.out.PermissionRepositoryPort;
import vallegrande.edu.pe.auth.domain.port.out.RoleRepositoryPort;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final RoleRepositoryPort roleRepository;
    private final PermissionRepositoryPort permissionRepository;

    private static final List<String> PARROCO_MODULES = List.of(
            "sacramentos", "agenda-parroquial", "solicitudes",
            "fianzas-pagos", "voluntariado", "personas", "libros");

    private static final List<String> SUPERADMIN_DEFAULT_MODULES = List.of("home", "usuarios", "config");

    @Override
    public Mono<List<String>> getUserModules(User user) {
        return roleRepository.findById(user.getRoleId())
                .flatMap(role -> {
                    String roleName = role.getName();

                    if ("SUPERADMIN".equals(roleName)) {
                        return permissionRepository.findPlatformModulesByUserId(user.getId())
                                .collectList()
                                .map(customModules -> customModules.isEmpty()
                                        ? SUPERADMIN_DEFAULT_MODULES
                                        : customModules);
                    }

                    if ("PARROCO".equals(roleName)) {
                        return Mono.just(PARROCO_MODULES);
                    }

                    if ("SECRETARIO".equals(roleName)) {
                        return permissionRepository.findActiveModulesByUserId(user.getId())
                                .collectList()
                                .map(customModules -> customModules.isEmpty()
                                        ? PARROCO_MODULES
                                        : customModules);
                    }

                    return Mono.just(List.of());
                });
    }
}
