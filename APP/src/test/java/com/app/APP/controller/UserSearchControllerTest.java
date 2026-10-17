package com.app.APP.controller;

import com.app.APP.auth.WebConfig;
import com.app.APP.model.dto.response.PublicUserResponse;
import com.app.APP.model.dto.response.UserSummaryResponse;
import com.app.APP.service.UserSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserSearchController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.app.APP.auth.SecurityConfig.class)
)
@Import({MockSecurityConfig.class, WebConfig.class})
@DisplayName("UserSearchController")
class UserSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSearchService userSearchService;

    @Test
    @DisplayName("GET /usuario/codigo/{userCode} retorna usuario pelo codigo")
    void shouldFindByCode() throws Exception {
        when(userSearchService.findByCode("trilheiro42"))
                .thenReturn(PublicUserResponse.builder().userCode("trilheiro42").name("Rafael").build());

        mockMvc.perform(get("/usuario/codigo/{userCode}", "trilheiro42")
                        .with(jwt().jwt(j -> j.subject("usuario-1").claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoUsuario").value("trilheiro42"))
                .andExpect(jsonPath("$.nome").value("Rafael"));
    }

    @Test
    @DisplayName("GET /usuario/codigo/{userCode} retorna 400 quando nao encontrado")
    void shouldReturn400WhenNotFound() throws Exception {
        when(userSearchService.findByCode("inexistente"))
                .thenThrow(new IllegalArgumentException("Usuario nao encontrado"));

        mockMvc.perform(get("/usuario/codigo/{userCode}", "inexistente")
                        .with(jwt().jwt(j -> j.subject("usuario-1").claim("codigoUsuario", "code-1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /usuario/busca retorna lista de usuarios por termo")
    void shouldAutocomplete() throws Exception {
        when(userSearchService.autocomplete("rafa"))
                .thenReturn(List.of(
                        PublicUserResponse.builder().userCode("trilheiro42").name("Rafael").build(),
                        PublicUserResponse.builder().userCode("rafa-trilhas").name("Rafaela").build()));

        mockMvc.perform(get("/usuario/busca")
                        .with(jwt().jwt(j -> j.subject("usuario-1").claim("codigoUsuario", "code-1")))
                        .param("termo", "rafa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /usuario/busca retorna lista vazia para termo sem resultado")
    void shouldReturnEmptyList() throws Exception {
        when(userSearchService.autocomplete("xyzxyz")).thenReturn(List.of());

        mockMvc.perform(get("/usuario/busca")
                        .with(jwt().jwt(j -> j.subject("usuario-1").claim("codigoUsuario", "code-1")))
                        .param("termo", "xyzxyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /usuario/resumo-codigo retorna resumo com o id interno")
    void shouldFindSummaryByCode() throws Exception {
        when(userSearchService.findSummaryByCode("trilheiro42"))
                .thenReturn(UserSummaryResponse.builder()
                        .id("usuario-1").name("Rafael").userCode("trilheiro42").build());

        mockMvc.perform(get("/usuario/resumo-codigo")
                        .with(jwt().jwt(j -> j.subject("usuario-1").claim("codigoUsuario", "code-1")))
                        .param("codigo", "trilheiro42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("usuario-1"))
                .andExpect(jsonPath("$.nome").value("Rafael"))
                .andExpect(jsonPath("$.codigoUsuario").value("trilheiro42"));
    }
}
