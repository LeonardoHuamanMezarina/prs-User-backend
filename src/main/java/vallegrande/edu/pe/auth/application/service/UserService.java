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
import vallegrande.edu.pe.auth.domain.port.in.PermissionUseCase;
import vallegrande.edu.pe.auth.domain.port.in.UserUseCase;
import vallegrande.edu.pe.auth.domain.port.out.IdentityProviderSyncPort;
import vallegrande.edu.pe.auth.domain.port.out.PasswordEncoderPort;
import vallegrande.edu.pe.auth.domain.port.out.RoleRepositoryPort;
import vallegrande.edu.pe.auth.domain.port.out.UserRepositoryPort;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final IdentityProviderSyncPort identityProviderSyncPort;
    private final PermissionUseCase permissionUseCase;

    @Override
    public Flux<UserView> getUsersByTenant(Long tenantId) {
        return userRepository.findByTenantId(tenantId)
                .flatMap(this::toUserView);
    }

    @Override
    public Mono<UserView> createSecretary(CreateAdminCommand command, Long parrocoTenantId) {
        return userRepository.existsByEmail(command.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException("El email ya está en uso"));
                    }
                    return roleRepository.findByName("SECRETARIO")
                            .switchIfEmpty(Mono.error(new RoleNotFoundException("Rol SECRETARIO no encontrado")))
                            .flatMap(role -> {
                                User user = User.builder()
                                        .name(command.getName())
                                        .lastname(command.getLastname())
                                        .email(command.getEmail())
                                        .dni(command.getDni())
                                        .phone(command.getPhone())
                                        .passwordHash(passwordEncoder.encode(command.getDni()))
                                        .roleId(role.getId())
                                        .tenantId(parrocoTenantId)
                                        .status("ACTIVE")
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();

                                return userRepository.save(user)
                                        .flatMap(savedUser -> identityProviderSyncPort
                                                .syncUserToIdentityProvider(command.getEmail(), command.getDni(),
                                                        command.getName(), command.getLastname(), "SECRETARIO",
                                                        savedUser.getTenantId())
                                                .then(syncDefaultPermissions(savedUser.getId()))
                                                .then(toUserView(savedUser)));
                            });
                });
    }

    @Override
    public Mono<UserView> getUserById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
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
    public Mono<Void> syncDefaultPermissions(Long userId) {
        return permissionUseCase.syncDefaultPermissions(userId);
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
