package com.trisha.bff.controller;

import com.trisha.bff.auth.AuthenticatedUser;
import com.trisha.bff.auth.WebConfig;
import com.trisha.bff.model.dto.request.FriendshipRequest;
import com.trisha.bff.model.dto.response.FriendshipResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.service.FriendshipBffService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FriendshipController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.trisha.bff.auth.SecurityConfig.class)
)
@Import({MockBffSecurityConfig.class, WebConfig.class})
@DisplayName("BFF FriendshipController")
class FriendshipBffControllerTest {

    private static final String USER_ID = "usuario-1";
    private static final String FRIENDSHIP_ID = "amizade-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FriendshipBffService friendshipService;

    private FriendshipResponse responseStub() {
        return new FriendshipResponse(FRIENDSHIP_ID, USER_ID, "usuario-2",
                "PENDENTE", LocalDateTime.now(), null);
    }

    @Test
    @DisplayName("POST /bff/amizades envia solicitacao com sucesso")
    void shouldRequestFriendship() throws Exception {
        FriendshipRequest request = new FriendshipRequest("amigo-codigo");
        when(friendshipService.request(any(FriendshipRequest.class))).thenReturn(responseStub());

        mockMvc.perform(post("/bff/amizades")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FRIENDSHIP_ID))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST /bff/amizades retorna 503 quando servico APP indisponivel")
    void shouldReturn503WhenServiceUnavailable() throws Exception {
        FriendshipRequest request = new FriendshipRequest("amigo-codigo");
        when(friendshipService.request(any(FriendshipRequest.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        mockMvc.perform(post("/bff/amizades")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("PATCH /bff/amizades/{id}/responder aceita solicitacao")
    void shouldRespondFriendship() throws Exception {
        FriendshipResponse accepted = new FriendshipResponse(FRIENDSHIP_ID, "usuario-2", USER_ID,
                "ACEITA", LocalDateTime.now(), LocalDateTime.now());
        when(friendshipService.respond(FRIENDSHIP_ID, "ACEITA")).thenReturn(accepted);

        mockMvc.perform(patch("/bff/amizades/{id}/responder", FRIENDSHIP_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("status", "ACEITA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITA"));
    }

    @Test
    @DisplayName("GET /bff/amizades/pendentes lista solicitacoes pendentes")
    void shouldListPending() throws Exception {
        PageResponse<FriendshipResponse> page = new PageResponse<>(List.of(responseStub()), 0, 20, 1, 1);
        when(friendshipService.getPending(any(AuthenticatedUser.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/bff/amizades/pendentes")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(FRIENDSHIP_ID));
    }

    @Test
    @DisplayName("GET /bff/amizades/pendentes retorna 503 quando servico APP indisponivel")
    void shouldReturn503OnPendingWhenServiceUnavailable() throws Exception {
        when(friendshipService.getPending(any(AuthenticatedUser.class), any(Pageable.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        mockMvc.perform(get("/bff/amizades/pendentes")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("GET /bff/amizades/amigos lista amigos do usuario autenticado")
    void shouldListFriends() throws Exception {
        PageResponse<FriendshipResponse> page = new PageResponse<>(List.of(responseStub()), 0, 20, 1, 1);
        when(friendshipService.getFriends(any(AuthenticatedUser.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/bff/amizades/amigos")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
