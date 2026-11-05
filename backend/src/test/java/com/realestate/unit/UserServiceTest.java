package com.realestate.unit;

import static com.realestate.support.TestData.ADMIN_ID;
import static com.realestate.support.TestData.AGENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realestate.common.exception.BusinessException;
import com.realestate.common.exception.ConflictException;
import com.realestate.support.Mappers;
import com.realestate.support.TestData;
import com.realestate.user.Role;
import com.realestate.user.User;
import com.realestate.user.UserRepository;
import com.realestate.user.UserService;
import com.realestate.user.dto.ChangePasswordRequest;
import com.realestate.user.dto.CreateUserRequest;
import com.realestate.user.dto.UpdateUserRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, Mappers.USER, passwordEncoder);
    }

    @Test
    void createHashesPasswordAndNormalizesEmail() {
        when(userRepository.existsByEmailIgnoreCase(" New@Test.com ")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(new CreateUserRequest("New", " New@Test.com ", "Password1", null, Role.AGENT));

        assertThat(response.email()).isEqualTo("new@test.com");
        assertThat(response.enabled()).isTrue();
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(u -> u.getPassword().equals("hashed")));
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateUserRequest("Dup", "dup@test.com", "Password1", null, Role.AGENT)))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminCannotDemoteOrDisableThemselves() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(TestData.user(ADMIN_ID, Role.ADMIN)));

        assertThatThrownBy(() -> service.update(ADMIN_ID,
                new UpdateUserRequest("Admin", null, Role.AGENT, true), TestData.admin()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.update(ADMIN_ID,
                new UpdateUserRequest("Admin", null, Role.ADMIN, false), TestData.admin()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void adminCanDisableAgent() {
        User agent = TestData.user(AGENT_ID, Role.AGENT);
        when(userRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));

        service.update(AGENT_ID, new UpdateUserRequest("Renamed", "0901234567", Role.AGENT, false), TestData.admin());

        assertThat(agent.isEnabled()).isFalse();
        assertThat(agent.getFullName()).isEqualTo("Renamed");
    }

    @Test
    void disabledUserCannotBeAssigned() {
        User agent = TestData.user(AGENT_ID, Role.AGENT);
        agent.setEnabled(false);
        when(userRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.findAssignableAgent(AGENT_ID)).isInstanceOf(BusinessException.class);
    }

    @Test
    void changePasswordRequiresCurrentPassword() {
        User agent = TestData.user(AGENT_ID, Role.AGENT);
        when(userRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(TestData.agent(),
                new ChangePasswordRequest("wrong", "NewPass123")))
                .isInstanceOf(BusinessException.class);
        assertThat(agent.getPassword()).isEqualTo("hash");
    }
}
