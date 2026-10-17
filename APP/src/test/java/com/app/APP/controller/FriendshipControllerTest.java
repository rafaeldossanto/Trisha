package com.app.APP.controller;

import com.app.APP.auth.WebConfig;
import com.app.APP.model.dto.request.FriendshipRequest;
import com.app.APP.model.dto.response.FriendshipResponse;
import com.app.APP.model.enums.FriendshipStatus;
import com.app.APP.service.FriendshipService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FriendshipController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.app.APP.auth.SecurityConfig.class)
)
@Import({MockSecurityConfig.class, WebConfig.class})
@DisplayName("FriendshipController")
class FriendshipControllerTest {

    private static final String USER_ID = "usuario-1";
    private static final String FRIENDSHIP_ID = "amizade-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FriendshipService friendshipService;

    private FriendshipResponse responseStub() {
        return FriendshipResponse.builder()
                .id(FRIENDSHIP_ID).requesterId(USER_ID).receiverId("usuario-2")
                .status(FriendshipStatus.PENDENTE).requestedAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("POST /amizade envia solicitacao com sucesso")
    void shouldRequestFriendship() throws Exception {
        FriendshipRequest request = new FriendshipRequest("codigo-amigo");
        when(friendshipService.request(any(), any(FriendshipRequest.class))).thenReturn(responseStub());

        mockMvc.perform(post("/amizade")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FRIENDSHIP_ID))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST /amizade retorna 400 quando receptor nao encontrado")
    void shouldReturn400WhenReceiverNotFound() throws Exception {
        FriendshipRequest request = new FriendshipRequest("codigo-invalido");
        when(friendshipService.request(any(), any(FriendshipRequest.class)))
                .thenThrow(new IllegalArgumentException("Usuario receptor nao encontrado"));

        mockMvc.perform(post("/amizade")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /amizade/{id}/responder aceita solicitacao")
    void shouldRespondFriendship() throws Exception {
        FriendshipResponse accepted = FriendshipResponse.builder()
                .id(FRIENDSHIP_ID).requesterId("usuario-2").receiverId(USER_ID)
                .status(FriendshipStatus.ACEITA).requestedAt(LocalDateTime.now())
                .respondedAt(LocalDateTime.now()).build();
        when(friendshipService.respond(any(), eq(FRIENDSHIP_ID), eq(FriendshipStatus.ACEITA))).thenReturn(accepted);

        mockMvc.perform(patch("/amizade/{id}/responder", FRIENDSHIP_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("status", "ACEITA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITA"));
    }

    @Test
    @DisplayName("GET /amizade/pendentes lista solicitacoes pendentes")
    void shouldListPending() throws Exception {
        Page<FriendshipResponse> page = new PageImpl<>(List.of(responseStub()));
        when(friendshipService.getPending(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/amizade/pendentes")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(FRIENDSHIP_ID));
    }

    @Test
    @DisplayName("GET /amizade/sao-amigos verifica relacao entre usuarios")
    void shouldCheckAreFriends() throws Exception {
        when(friendshipService.areFriends("usuario-1", "usuario-2")).thenReturn(true);

        mockMvc.perform(get("/amizade/sao-amigos")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("a", "usuario-1")
                        .param("b", "usuario-2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /amizade/sao-amigos recusa consulta sobre relacao alheia")
    void shouldRejectAreFriendsForUnrelatedUsers() throws Exception {
        mockMvc.perform(get("/amizade/sao-amigos")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("a", "outro-1")
                        .param("b", "outro-2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /amizade/amigos-ids lista ids dos amigos do autenticado")
    void shouldListFriendIds() throws Exception {
        when(friendshipService.friendIds(USER_ID)).thenReturn(List.of("usuario-2", "usuario-3"));

        mockMvc.perform(get("/amizade/amigos-ids")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("usuario-2"));
    }
}
