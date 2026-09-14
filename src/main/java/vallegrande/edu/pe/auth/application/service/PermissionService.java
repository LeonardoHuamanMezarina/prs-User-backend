package vallegrande.edu.pe.auth.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.exception.BusinessException;
import vallegrande.edu.pe.auth.domain.exception.UserNotFoundException;
import vallegrande.edu.pe.auth.domain.port.in.PermissionUseCase;
import vallegrande.edu.pe.auth.domain.port.out.PermissionRepositoryPort;
import vallegrande.edu.pe.auth.domain.port.out.RoleRepositoryPort;
import vallegrande.edu.pe.auth.domain.port.out.UserRepositoryPort;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionService implements PermissionUseCase {

    private final PermissionRepositoryPort permissionRepository;
    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;

    @Override
    public Flux<Map<String, Object>> getAllPermissions() {
        return permissionRepository.findAllPermissions();
    }

    @Override
    public Flux<Map<String, Object>> getUserTenantPermissions(Long userId) {
        return permissionRepository.findUserTenantPermissions(userId);
    }

    @Override
    public Mono<Void> assignPermission(Long userId, Long permissionId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .flatMap(user -> roleRepository.findById(user.getRoleId()))
                .flatMap(role -> {
                    if (!"SECRETARIO".equals(role.getName())) {
                        return Mono.error(new BusinessException("No puedes editar los permisos de este rol."));
                    }
                    return permissionRepository.assignPermission(userId, permissionId);
                });
    }

    @Override
    public Mono<Void> removePermission(Long userId, Long permissionId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .flatMap(user -> roleRepository.findById(user.getRoleId()))
                .flatMap(role -> {
                    if (!"SECRETARIO".equals(role.getName())) {
                        return Mono.error(new BusinessException("No puedes quitarle permisos a este rol."));
                    }
                    return permissionRepository.removePermission(userId, permissionId);
                });
    }

    @Override
    public Mono<Void> syncDefaultPermissions(Long userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .flatMap(user -> roleRepository.findById(user.getRoleId()))
                .flatMap(role -> {
                    if (!"SECRETARIO".equals(role.getName())) {
                        return Mono.error(new BusinessException("Solo se sincronizan Secretarios."));
                    }
                    return permissionRepository.syncDefaultPermissions(userId);
                });
    }

    @Override
    public Mono<Void> syncPlatformPermissions(Long userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .flatMap(user -> roleRepository.findById(user.getRoleId()))
                .flatMap(role -> {
                    if (!"SUPERADMIN".equals(role.getName())) {
                        return Mono.error(new BusinessException("Solo se sincronizan Superadmins."));
                    }
                    return permissionRepository.syncPlatformPermissions(userId);
                });
    }
}
