package com.trisha.bff.controller;

import com.trisha.bff.auth.WebConfig;
import com.trisha.bff.model.dto.request.AdventureRequest;
import com.trisha.bff.model.dto.response.AdventureResponse;
import com.trisha.bff.service.AdventureBffService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AdventureController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.trisha.bff.auth.SecurityConfig.class)
)
@Import({MockBffSecurityConfig.class, WebConfig.class})
@DisplayName("BFF AdventureController")
class AdventureBffControllerTest {

    private static final String ADVENTURE_ID = "aventura-1";
    private static final String USER_ID = "usuario-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdventureBffService adventureService;

    private AdventureResponse responseStub() {
        return new AdventureResponse(ADVENTURE_ID, USER_ID, null, "Pico da Bandeira",
                "PLANEJADA", "PRIVADA", LocalDateTime.now(), 1, null);
    }

    @Test
    @DisplayName("POST /bff/aventuras cria aventura com sucesso")
    void shouldCreateAdventure() throws Exception {
        AdventureRequest request = new AdventureRequest(null, "Pico da Bandeira", "PRIVADA");
        when(adventureService.create(any(AdventureRequest.class))).thenReturn(responseStub());

        mockMvc.perform(post("/bff/aventuras")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ADVENTURE_ID))
                .andExpect(jsonPath("$.destino").value("Pico da Bandeira"));
    }

    @Test
    @DisplayName("GET /bff/aventuras/{id} retorna aventura existente")
    void shouldGetAdventureById() throws Exception {
        when(adventureService.getById(USER_ID, ADVENTURE_ID)).thenReturn(responseStub());

        mockMvc.perform(get("/bff/aventuras/{id}", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ADVENTURE_ID));
    }

    @Test
    @DisplayName("GET /bff/aventuras/{id} retorna 404 quando aventura nao encontrada no downstream")
    void shouldReturn404WhenNotFoundDownstream() throws Exception {
        when(adventureService.getById(USER_ID, "inexistente"))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/bff/aventuras/{id}", "inexistente")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /bff/aventuras/{id} retorna 503 quando servico APP indisponivel")
    void shouldReturn503WhenServiceUnavailable() throws Exception {
        when(adventureService.getById(USER_ID, ADVENTURE_ID))
                .thenThrow(new ResourceAccessException("Connection refused"));

        mockMvc.perform(get("/bff/aventuras/{id}", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("DELETE /bff/aventuras/{id} deleta com sucesso")
    void shouldDeleteAdventure() throws Exception {
        mockMvc.perform(delete("/bff/aventuras/{id}", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /bff/aventuras/{id} retorna 503 quando servico APP indisponivel")
    void shouldReturn503OnDeleteWhenServiceUnavailable() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceAccessException("Connection refused"))
                .when(adventureService).delete(eq(ADVENTURE_ID));

        mockMvc.perform(delete("/bff/aventuras/{id}", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isServiceUnavailable());
    }
}
