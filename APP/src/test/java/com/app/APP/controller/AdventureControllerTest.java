package com.app.APP.controller;

import com.app.APP.auth.WebConfig;
import com.app.APP.model.dto.request.AdventureRequest;
import com.app.APP.model.dto.response.AdventureResponse;
import com.app.APP.model.dto.response.LeaveAdventureResponse;
import com.app.APP.model.enums.AdventureStatus;
import com.app.APP.model.enums.AdventureVisibility;
import com.app.APP.service.AdventureExitService;
import com.app.APP.service.AdventureService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AdventureController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.app.APP.auth.SecurityConfig.class)
)
@Import({MockSecurityConfig.class, WebConfig.class})
@DisplayName("AdventureController")
class AdventureControllerTest {

    private static final String USER_ID = "usuario-1";
    private static final String ADVENTURE_ID = "aventura-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdventureService adventureService;

    @MockitoBean
    private AdventureExitService adventureExitService;

    private AdventureResponse responseStub() {
        return AdventureResponse.builder()
                .id(ADVENTURE_ID)
                .userId(USER_ID)
                .destination("Pico da Bandeira")
                .status(AdventureStatus.PLANEJADA)
                .visibility(AdventureVisibility.PRIVADA)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /aventura retorna 200 ao criar com dados validos")
    void shouldCreateAdventure() throws Exception {
        AdventureRequest request = new AdventureRequest(null, "Pico da Bandeira", AdventureVisibility.PRIVADA);
        when(adventureService.create(any(), any(AdventureRequest.class))).thenReturn(responseStub());

        mockMvc.perform(post("/aventura")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ADVENTURE_ID))
                .andExpect(jsonPath("$.destino").value("Pico da Bandeira"));
    }

    @Test
    @DisplayName("POST /aventura retorna 400 quando destino esta em branco")
    void shouldRejectMissingDestination() throws Exception {
        AdventureRequest request = new AdventureRequest(null, "", AdventureVisibility.PRIVADA);

        mockMvc.perform(post("/aventura")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /aventura/{id} retorna a aventura existente")
    void shouldGetAdventureById() throws Exception {
        when(adventureService.getById(USER_ID, ADVENTURE_ID)).thenReturn(responseStub());

        mockMvc.perform(get("/aventura/{id}", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ADVENTURE_ID));
    }

    @Test
    @DisplayName("GET /aventura/{id} retorna 400 quando nao encontrada")
    void shouldReturn400WhenNotFound() throws Exception {
        when(adventureService.getById(USER_ID, "inexistente"))
                .thenThrow(new IllegalArgumentException("Aventura nao encontrada"));

        mockMvc.perform(get("/aventura/{id}", "inexistente")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /aventura/usuario/{userId} retorna pagina de aventuras")
    void shouldListByUser() throws Exception {
        Page<AdventureResponse> page = new PageImpl<>(List.of(responseStub()));
        when(adventureService.getByUser(eq(USER_ID), eq(USER_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/aventura/usuario/{userId}", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ADVENTURE_ID));
    }

    @Test
    @DisplayName("PATCH /aventura/{id}/status atualiza status")
    void shouldUpdateStatus() throws Exception {
        AdventureResponse updated = AdventureResponse.builder()
                .id(ADVENTURE_ID).userId(USER_ID).destination("Pico da Bandeira")
                .status(AdventureStatus.CONCLUIDA).visibility(AdventureVisibility.PRIVADA)
                .createdAt(LocalDateTime.now()).build();

        when(adventureService.updateStatus(any(), eq(ADVENTURE_ID), eq(AdventureStatus.CONCLUIDA))).thenReturn(updated);

        mockMvc.perform(patch("/aventura/{id}/status", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("status", "CONCLUIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    @DisplayName("DELETE /aventura/{id} retorna 403 quando nao e o dono")
    void shouldRejectDeleteByNonOwner() throws Exception {
        doThrow(new com.app.APP.exception.ForbiddenException("Voce nao e o dono desta aventura"))
                .when(adventureService).delete(any(), eq(ADVENTURE_ID));

        mockMvc.perform(delete("/aventura/{id}", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject("outro-usuario").claim("codigoUsuario", "code-outro"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /aventura/{id} exclui com sucesso")
    void shouldDeleteAdventure() throws Exception {
        mockMvc.perform(delete("/aventura/{id}", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /aventura/{id}/participante sai da aventura e retorna a aventura pessoal criada")
    void shouldLeaveAdventure() throws Exception {
        when(adventureExitService.leave(eq(USER_ID), eq(ADVENTURE_ID), eq(true)))
                .thenReturn(new LeaveAdventureResponse("aventura-pessoal", 1, 0));

        mockMvc.perform(delete("/aventura/{id}/participante", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aventuraPessoalId").value("aventura-pessoal"))
                .andExpect(jsonPath("$.caminhosMovidos").value(1));
    }

    @Test
    @DisplayName("DELETE /aventura/{id}/participante/{userId} remove participante (kick) pelo dono")
    void shouldKickParticipant() throws Exception {
        when(adventureExitService.kick(eq(USER_ID), eq(ADVENTURE_ID), eq("membro-2")))
                .thenReturn(new LeaveAdventureResponse("aventura-pessoal", 2, 0));

        mockMvc.perform(delete("/aventura/{id}/participante/{userId}", ADVENTURE_ID, "membro-2")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aventuraPessoalId").value("aventura-pessoal"));
    }
}
