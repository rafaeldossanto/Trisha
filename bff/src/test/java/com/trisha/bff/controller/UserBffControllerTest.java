package com.trisha.bff.controller;

import com.trisha.bff.auth.WebConfig;
import com.trisha.bff.model.dto.request.UserCreateRequest;
import com.trisha.bff.model.dto.response.UserResponse;
import com.trisha.bff.model.dto.response.UserSummaryResponse;
import com.trisha.bff.service.UserBffService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.trisha.bff.auth.SecurityConfig.class)
)
@Import({MockBffSecurityConfig.class, WebConfig.class})
@DisplayName("BFF UserController")
class UserBffControllerTest {

    private static final String USER_ID = "usuario-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserBffService userService;

    private UserResponse responseStub() {
        return new UserResponse(USER_ID, "Rafael Santos", "rafael@email.com",
                "trilheiro42", "ATIVO", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /bff/usuarios cria usuario com sucesso")
    void shouldCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest("Rafael Santos", "rafael@email.com", "senha123");
        when(userService.create(any(UserCreateRequest.class))).thenReturn(responseStub());

        mockMvc.perform(post("/bff/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.nome").value("Rafael Santos"))
                .andExpect(jsonPath("$.codigoUsuario").value("trilheiro42"));
    }

    @Test
    @DisplayName("POST /bff/usuarios retorna 503 quando servico Cadastro indisponivel")
    void shouldReturn503WhenServiceUnavailable() throws Exception {
        UserCreateRequest request = new UserCreateRequest("Rafael Santos", "rafael@email.com", "senha123");
        when(userService.create(any(UserCreateRequest.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        mockMvc.perform(post("/bff/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("GET /bff/usuarios/{id} retorna usuario existente")
    void shouldGetUserById() throws Exception {
        when(userService.getById(USER_ID)).thenReturn(responseStub());

        mockMvc.perform(get("/bff/usuarios/{id}", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "trilheiro42"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value("rafael@email.com"));
    }

    @Test
    @DisplayName("GET /bff/usuarios/{id} retorna 404 quando a propria conta nao existe no downstream")
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getById(USER_ID))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/bff/usuarios/{id}", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "trilheiro42"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /bff/usuarios/{id} recusa acesso a conta de outro usuario")
    void shouldRejectAccessToAnotherUser() throws Exception {
        mockMvc.perform(get("/bff/usuarios/{id}", "outro-usuario")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "trilheiro42"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bff/usuarios/resumo retorna resumo pelo codigo publico")
    void shouldFindSummaryByCode() throws Exception {
        when(userService.findSummaryByCode("trilheiro42"))
                .thenReturn(new UserSummaryResponse(USER_ID, "Rafael Santos", "trilheiro42"));

        mockMvc.perform(get("/bff/usuarios/resumo")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "trilheiro42")))
                        .param("codigo", "trilheiro42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.nome").value("Rafael Santos"))
                .andExpect(jsonPath("$.codigoUsuario").value("trilheiro42"));
    }
}
