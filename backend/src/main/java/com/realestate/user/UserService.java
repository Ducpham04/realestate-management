package com.realestate.user;

import com.realestate.common.api.PageResponse;
import com.realestate.common.api.SortSanitizer;
import com.realestate.common.exception.BusinessException;
import com.realestate.common.exception.ConflictException;
import com.realestate.common.exception.ResourceNotFoundException;
import com.realestate.security.UserPrincipal;
import com.realestate.user.dto.ChangePasswordRequest;
import com.realestate.user.dto.CreateUserRequest;
import com.realestate.user.dto.UpdateUserRequest;
import com.realestate.user.dto.UserResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final Set<String> SORTABLE = Set.of("id", "fullName", "email", "role", "createdAt");

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserResponse> search(String keyword, Role role, Pageable pageable) {
        Pageable safe = SortSanitizer.sanitize(pageable, SORTABLE, Sort.by("fullName"));
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return PageResponse.of(userRepository.search(kw, role, safe), userMapper::toResponse);
    }

    public UserResponse getById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email '%s' is already in use".formatted(request.email()));
        }
        User user = userMapper.toEntity(request);
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, UserPrincipal actor) {
        User user = findUser(id);
        if (user.getId().equals(actor.getId())
                && (request.role() != user.getRole() || !request.enabled())) {
            throw new BusinessException("You cannot change your own role or disable your own account");
        }
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        return userMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(UserPrincipal actor, ChangePasswordRequest request) {
        User user = findUser(actor.getId());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    public User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /** Resolves a user that may be assigned as the responsible agent of a record. */
    public User findAssignableAgent(Long id) {
        User user = findUser(id);
        if (!user.isEnabled()) {
            throw new BusinessException("User %d is disabled and cannot be assigned".formatted(id));
        }
        return user;
    }
}
