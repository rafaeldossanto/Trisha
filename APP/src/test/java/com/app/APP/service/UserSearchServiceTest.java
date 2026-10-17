package com.app.APP.service;

import com.app.APP.entity.User;
import com.app.APP.model.dto.response.PublicUserResponse;
import com.app.APP.model.dto.response.UserSummaryResponse;
import com.app.APP.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSearchService")
class UserSearchServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSearchService service;

    private User user(String code, String name) {
        User u = new User();
        ReflectionTestUtils.setField(u, "userCode", code);
        ReflectionTestUtils.setField(u, "name", name);
        ReflectionTestUtils.setField(u, "email", "secreto@trilha.com");
        return u;
    }

    @Test
    @DisplayName("findByCode should return the public view (no email)")
    void shouldFindByCode() {
        when(userRepository.findByUserCode("rafael#1"))
                .thenReturn(Optional.of(user("rafael#1", "Rafael")));

        PublicUserResponse response = service.findByCode("rafael#1");

        assertThat(response.userCode()).isEqualTo("rafael#1");
        assertThat(response.name()).isEqualTo("Rafael");
    }

    @Test
    @DisplayName("findByCode should fail when user does not exist")
    void shouldFailFindNotFound() {
        when(userRepository.findByUserCode("inexistente#9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("inexistente#9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrado");
    }

    @Test
    @DisplayName("findSummaryByCode should return the summary with the internal id")
    void shouldFindSummaryByCode() {
        User u = user("rafael#1", "Rafael");
        ReflectionTestUtils.setField(u, "id", "usuario-1");
        when(userRepository.findByUserCode("rafael#1")).thenReturn(Optional.of(u));

        UserSummaryResponse response = service.findSummaryByCode("rafael#1");

        assertThat(response.id()).isEqualTo("usuario-1");
        assertThat(response.name()).isEqualTo("Rafael");
        assertThat(response.userCode()).isEqualTo("rafael#1");
    }

    @Test
    @DisplayName("findSummaryByCode should fail when user does not exist")
    void shouldFailSummaryNotFound() {
        when(userRepository.findByUserCode("inexistente#9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findSummaryByCode("inexistente#9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrado");
    }

    @Test
    @DisplayName("autocomplete should map results of the prefix")
    void shouldAutocomplete() {
        when(userRepository.findByUserCodePrefix("raf"))
                .thenReturn(List.of(user("rafael#1", "Rafael"), user("rafaela#2", "Rafaela")));

        List<PublicUserResponse> response = service.autocomplete("raf");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).userCode()).isEqualTo("rafael#1");
    }
}
