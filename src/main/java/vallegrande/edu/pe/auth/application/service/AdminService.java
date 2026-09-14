package vallegrande.edu.pe.auth.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.application.dto.command.CreateAdminCommand;
import vallegrande.edu.pe.auth.application.dto.command.UpdateUserCommand;
import vallegrande.edu.pe.auth.application.dto.query.UserView;
import vallegrande.edu.pe.auth.domain.exception.BusinessException;
import vallegrande.edu.pe.auth.domain.exception.RoleNotFoundException;
import vallegrande.edu.pe.auth.domain.exception.UserNotFoundException;
import vallegrande.edu.pe.auth.domain.model.User;
import vallegrande.edu.pe.auth.domain.port.in.AdminUseCase;
import vallegrande.edu.pe.auth.domain.port.in.PermissionUseCase;
import vallegrande.edu.pe.auth.domain.port.out.IdentityProviderSyncPort;
import vallegrande.edu.pe.auth.domain.port.out.PasswordEncoderPort;
import vallegrande.edu.pe.auth.domain.port.out.RoleRepositoryPort;
import vallegrande.edu.pe.auth.domain.port.out.TenantClientPort;
import vallegrande.edu.pe.auth.domain.port.out.UserRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService implements AdminUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TenantClientPort tenantClientPort;
    private final PermissionUseCase permissionUseCase;
    private final IdentityProviderSyncPort identityProviderSyncPort;

    @Override
    public Mono<UserView> initSuperAdmin(CreateAdminCommand command) {
        return userRepository.existsByEmail(command.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException("El email ya está en uso"));
                    }
                    return userRepository.findAll()
                            .flatMap(u -> roleRepository.findById(u.getRoleId()))
                            .any(role -> "SUPERADMIN".equals(role.getName()))
                            .flatMap(hasSuperAdmin -> {
                                if (hasSuperAdmin) {
                                    return Mono.error(new BusinessException("Ya existe un SUPERADMIN"));
                                }
                                return roleRepository.findByName("SUPERADMIN")
                                        .switchIfEmpty(
                                                Mono.error(new RoleNotFoundException("Rol SUPERADMIN no encontrado")))
                                        .flatMap(role -> {
                                            User user = User.builder()
                                                    .name(command.getName())
                                                    .lastname(command.getLastname())
                                                    .email(command.getEmail())
                                                    .dni(command.getDni())
                                                    .passwordHash(passwordEncoder.encode(command.getDni()))
                                                    .roleId(role.getId())
                                                    .status("ACTIVE")
                                                    .createdAt(LocalDateTime.now())
                                                    .updatedAt(LocalDateTime.now())
                                                    .build();

                                            return userRepository.save(user)
                                                    .flatMap(savedUser -> identityProviderSyncPort
                                                            .syncUserToIdentityProvider(command.getEmail(),
                                                                    command.getDni(), command.getName(),
                                                                    command.getLastname(), "SUPERADMIN",
                                                                    savedUser.getTenantId())
                                                            .then(permissionUseCase
                                                                    .syncPlatformPermissions(savedUser.getId()))
                                                            .then(toUserView(savedUser)));
                                        });
                            });
                });
    }

    @Override
    public Mono<UserView> createPrivilegedUser(CreateAdminCommand command) {
        return userRepository.existsByEmail(command.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException("El email ya está en uso"));
                    }
                    return roleRepository.findByName(command.getRole())
                            .switchIfEmpty(Mono.error(new RoleNotFoundException("Rol no encontrado")))
                            .flatMap(role -> {
                                if ("PARROCO".equals(role.getName()) && command.getTenantId() == null) {
                                    return Mono.error(
                                            new BusinessException("El Tenant ID es obligatorio para el rol PÁRROCO"));
                                }

                                User user = User.builder()
                                        .name(command.getName())
                                        .lastname(command.getLastname())
                                        .email(command.getEmail())
                                        .dni(command.getDni())
                                        .passwordHash(passwordEncoder.encode(command.getDni()))
                                        .roleId(role.getId())
                                        .tenantId(command.getTenantId())
                                        .status("ACTIVE")
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();

                                return userRepository.save(user)
                                        .flatMap(savedUser -> {
                                            Mono<Void> postAction = Mono.empty();
                                            if ("PARROCO".equals(role.getName())) {
                                                postAction = tenantClientPort.activateTenant(savedUser.getTenantId());
                                            } else if ("SECRETARIO".equals(role.getName())) {
                                                postAction = permissionUseCase
                                                        .syncDefaultPermissions(savedUser.getId());
                                            }
                                            return identityProviderSyncPort
                                                    .syncUserToIdentityProvider(command.getEmail(), command.getDni(),
                                                            command.getName(), command.getLastname(), role.getName(),
                                                            savedUser.getTenantId())
                                                    .then(postAction)
                                                    .then(toUserView(savedUser));
                                        });
                            });
                });
    }

    @Override
    public Flux<UserView> getAllUsers() {
        return userRepository.findAll()
                .flatMap(this::toUserView);
    }

    @Override
    public Flux<UserView> getUsersByStatus(String status) {
        return userRepository.findByStatus(status)
                .flatMap(this::toUserView);
    }

    @Override
    public Mono<UserView> updateUser(Long id, UpdateUserCommand command) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .flatMap(user -> {
                    if (command.getName() != null)
                        user.setName(command.getName());
                    if (command.getLastname() != null)
                        user.setLastname(command.getLastname());
                    if (command.getPhone() != null)
                        user.setPhone(command.getPhone());
                    if (command.getDni() != null) {
                        user.setDni(command.getDni());
                        user.setPasswordHash(passwordEncoder.encode(command.getDni()));
                    }
                    if (command.getTenantId() != null)
                        user.setTenantId(command.getTenantId());
                    user.setUpdatedAt(LocalDateTime.now());

                    if (command.getRole() != null) {
                        return roleRepository.findByName(command.getRole())
                                .switchIfEmpty(Mono.error(new RoleNotFoundException("Rol no encontrado")))
                                .flatMap(role -> {
                                    user.setRoleId(role.getId());
                                    return userRepository.save(user);
                                });
                    }
                    return userRepository.save(user);
                })
                .flatMap(this::toUserView);
    }

    @Override
    public Mono<UserView> softDelete(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .flatMap(user -> {
                    user.setStatus("INACTIVE");
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .flatMap(this::toUserView);
    }

    @Override
    public Mono<UserView> restore(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .flatMap(user -> {
                    user.setStatus("ACTIVE");
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .flatMap(this::toUserView);
    }

    @Override
    public Flux<Long> getUsedTenantIds() {
        return userRepository.findAll()
                .filter(user -> user.getTenantId() != null)
                .map(User::getTenantId)
                .distinct();
    }

    @Override
    public Mono<Void> cleanUsers(List<Long> ids) {
        return userRepository.deleteByIds(ids);
    }

    private Mono<UserView> toUserView(User user) {
        if (user.getRoleId() == null) {
            return Mono.just(UserView.builder()
                    .id(user.getId())
                    .tenantId(user.getTenantId())
                    .name(user.getName())
                    .lastname(user.getLastname())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .dni(user.getDni())
                    .status(user.getStatus())
                    .role(null)
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build());
        }
        return roleRepository.findById(user.getRoleId())
                .map(role -> UserView.builder()
                        .id(user.getId())
                        .tenantId(user.getTenantId())
                        .name(user.getName())
                        .lastname(user.getLastname())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .dni(user.getDni())
                        .status(user.getStatus())
                        .role(role.getName())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .defaultIfEmpty(UserView.builder()
                        .id(user.getId())
                        .tenantId(user.getTenantId())
                        .name(user.getName())
                        .lastname(user.getLastname())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .dni(user.getDni())
                        .status(user.getStatus())
                        .role(null)
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build());
    }
}
