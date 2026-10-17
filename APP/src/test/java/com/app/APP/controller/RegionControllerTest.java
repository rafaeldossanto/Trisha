package com.app.APP.controller;

import com.app.APP.auth.WebConfig;
import com.app.APP.model.dto.request.RegionRequest;
import com.app.APP.model.dto.response.RegionResponse;
import com.app.APP.model.enums.RegionVisibility;
import com.app.APP.service.RegionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = RegionController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.app.APP.auth.SecurityConfig.class)
)
@Import({MockSecurityConfig.class, WebConfig.class})
@DisplayName("RegionController")
class RegionControllerTest {

    private static final String USER_ID = "usuario-1";
    private static final String REGION_ID = "regiao-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegionService regionService;

    private RegionResponse responseStub() {
        return RegionResponse.builder()
                .id(REGION_ID).userId(USER_ID).name("Serra do Caparao")
                .visibility(RegionVisibility.PRIVADA).cities(List.of())
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("POST /regiao cria regiao com dados validos")
    void shouldCreateRegion() throws Exception {
        RegionRequest request = new RegionRequest("Serra do Caparao", null, null, RegionVisibility.PRIVADA, List.of());
        when(regionService.create(any(), any(RegionRequest.class))).thenReturn(responseStub());

        mockMvc.perform(post("/regiao")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REGION_ID))
                .andExpect(jsonPath("$.nome").value("Serra do Caparao"));
    }

    @Test
    @DisplayName("POST /regiao retorna 400 quando nome esta em branco")
    void shouldRejectBlankName() throws Exception {
        RegionRequest request = new RegionRequest("", null, null, RegionVisibility.PRIVADA, List.of());

        mockMvc.perform(post("/regiao")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /regiao lista regioes do usuario")
    void shouldListMine() throws Exception {
        Page<RegionResponse> page = new PageImpl<>(List.of(responseStub()));
        when(regionService.listMine(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/regiao")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(REGION_ID));
    }

    @Test
    @DisplayName("GET /regiao/{id} retorna 400 quando nao encontrada")
    void shouldReturn400WhenNotFound() throws Exception {
        when(regionService.getById(any(), eq("inexistente")))
                .thenThrow(new IllegalArgumentException("Regiao nao encontrada"));

        mockMvc.perform(get("/regiao/{id}", "inexistente")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /regiao/{id} atualiza regiao")
    void shouldUpdateRegion() throws Exception {
        RegionRequest request = new RegionRequest("Atualizada", null, null, RegionVisibility.PUBLICA, List.of());
        RegionResponse updated = RegionResponse.builder().id(REGION_ID).userId(USER_ID).name("Atualizada")
                .visibility(RegionVisibility.PUBLICA).cities(List.of()).createdAt(LocalDateTime.now()).build();
        when(regionService.update(any(), eq(REGION_ID), any(RegionRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/regiao/{id}", REGION_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Atualizada"));
    }

    @Test
    @DisplayName("DELETE /regiao/{id} retorna 403 quando nao e o dono")
    void shouldRejectDeleteByNonOwner() throws Exception {
        doThrow(new com.app.APP.exception.ForbiddenException("Voce nao e o dono desta regiao"))
                .when(regionService).delete(any(), eq(REGION_ID));

        mockMvc.perform(delete("/regiao/{id}", REGION_ID)
                        .with(jwt().jwt(j -> j.subject("outro-usuario").claim("codigoUsuario", "code-outro"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /regiao/{id} remove com sucesso")
    void shouldDeleteRegion() throws Exception {
        mockMvc.perform(delete("/regiao/{id}", REGION_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk());
    }
}
