package com.pradeep.user.service;

import com.pradeep.user.entity.User;
import com.pradeep.user.repository.RoleRepository;
import com.pradeep.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserRegistrationService userRegistrationService = mock(UserRegistrationService.class);
    private final UserService userService = new UserService(userRepository, roleRepository, userRegistrationService);

    @Test
    void updatesEmailForTheSameGoogleIdentity() {
        User existing = User.builder().id(10L).googleId("google-subject").email("old@example.com").build();
        when(userRepository.findByGoogleId("google-subject")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(existing)).thenReturn(existing);

        User result = userService.createOrUpdateUser("new@example.com", "google-subject", "Example", null);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(existing);
        verify(userRegistrationService, never()).register(any(), any(), any(), any());
    }

    @Test
    void resolvesTheCanonicalUserAfterAConcurrentRegistrationConflict() {
        User canonical = User.builder().id(10L).googleId("google-subject").email("person@example.com").build();
        when(userRepository.findByGoogleId("google-subject"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(canonical));
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.empty());
        when(userRegistrationService.register("person@example.com", "google-subject", "Example", null))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        User result = userService.createOrUpdateUser("person@example.com", "google-subject", "Example", null);

        assertThat(result).isSameAs(canonical);
    }

    @Test
    void rejectsAnEmailAlreadyOwnedByAnotherGoogleIdentity() {
        User existing = User.builder().id(10L).googleId("google-subject").email("old@example.com").build();
        User conflicting = User.builder().id(11L).googleId("other-google-subject").email("new@example.com").build();
        when(userRepository.findByGoogleId("google-subject")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(conflicting));

        assertThatThrownBy(() -> userService.createOrUpdateUser("new@example.com", "google-subject", "Example", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email is already linked to another identity");
    }
}
