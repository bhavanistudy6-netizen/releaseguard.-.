package com.releaseguard.security;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserPageResponse list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size between 1 and 100");
        }
        var pageResult = userRepository.findAll(PageRequest.of(page, size, Sort.by("id")));
        var content = pageResult.getContent().stream().map(UserResponse::from).toList();
        return new UserPageResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String normalizedUsername = request.username().trim().toLowerCase();
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists: " + normalizedUsername);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(normalizedUsername, passwordHash, request.displayName(), request.roles());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, Long currentAdminId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isCurrentlyAdmin = user.getRoles().contains(Role.ADMIN) && user.isEnabled();
        boolean willRemainAdmin = request.roles().contains(Role.ADMIN) && request.enabled();

        if (isCurrentlyAdmin && !willRemainAdmin) {
            long activeAdmins = userRepository.countActiveAdmins();
            if (activeAdmins <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot disable or remove the ADMIN role from the last active administrator");
            }
        }

        user.setDisplayName(request.displayName());
        user.setRoles(request.roles());
        user.setEnabled(request.enabled());

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete your own active administrator account");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRoles().contains(Role.ADMIN) && user.isEnabled()) {
            long activeAdmins = userRepository.countActiveAdmins();
            if (activeAdmins <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete the last active administrator");
            }
        }

        userRepository.delete(user);
    }
}

