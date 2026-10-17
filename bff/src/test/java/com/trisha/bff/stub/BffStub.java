package com.trisha.bff.stub;

import com.trisha.bff.model.dto.request.FriendshipRequest;
import com.trisha.bff.model.dto.request.AdventureRequest;
import com.trisha.bff.model.dto.request.PathRequest;
import com.trisha.bff.model.dto.request.EvidenceRequest;
import com.trisha.bff.model.dto.request.MediaRequest;
import com.trisha.bff.model.dto.request.GpsPointRequest;
import com.trisha.bff.model.dto.request.PointOfInterestRequest;
import com.trisha.bff.model.dto.request.SessionRequest;
import com.trisha.bff.model.dto.request.UserCreateRequest;
import com.trisha.bff.model.dto.request.UserUpdateRequest;
import com.trisha.bff.model.dto.response.FriendshipResponse;
import com.trisha.bff.model.dto.response.AuthenticationResponse;
import com.trisha.bff.model.dto.response.AdventureResponse;
import com.trisha.bff.model.dto.response.PathResponse;
import com.trisha.bff.model.dto.response.EvidenceResponse;
import com.trisha.bff.model.dto.response.MediaResponse;
import com.trisha.bff.model.dto.response.GpsPointResponse;
import com.trisha.bff.model.dto.response.PointOfInterestResponse;
import com.trisha.bff.model.dto.response.SessionResponse;
import com.trisha.bff.model.dto.response.UserResponse;

import java.time.LocalDateTime;

/**
 * Facilitador de testes para os DTOs de request e response do BFF.
 */
public final class BffStub {

    public static final String USER_ID = "usuario-1";
    public static final String ADVENTURE_ID = "aventura-1";
    public static final String PATH_ID = "caminho-1";
    public static final String POINT_ID = "ponto-1";
    public static final String SESSION_ID = "sessao-1";

    private BffStub() {
    }

    // ----------------------------- Usuario ------------------------------

    public static UserResponse aUser() {
        return new UserResponse(USER_ID, "Rafael", "rafael@trilha.com",
                "USR-1", "ATIVO", LocalDateTime.now(), LocalDateTime.now());
    }

    public static AuthenticationResponse anAuthentication() {
        return new AuthenticationResponse(aUser(), "jwt-token", 7200);
    }

    public static UserCreateRequest aUserCreateRequest() {
        return new UserCreateRequest("Rafael", "rafael@trilha.com", "senha123");
    }

    public static UserUpdateRequest aUserUpdateRequest() {
        return new UserUpdateRequest("Rafael Santos", null);
    }

    // ----------------------------- Aventura -----------------------------

    public static AdventureResponse anAdventure() {
        return new AdventureResponse(ADVENTURE_ID, USER_ID, "regiao-1", "Pico da Bandeira",
                "PLANEJADA", "PRIVADA", LocalDateTime.now(), 1, null);
    }

    public static AdventureRequest anAdventureRequest() {
        return new AdventureRequest("regiao-1", "Pico da Bandeira", "PRIVADA");
    }

    // ----------------------------- Caminho ------------------------------

    public static PathResponse aPath() {
        return new PathResponse(PATH_ID, ADVENTURE_ID, USER_ID, "ROXO", 1,
                LocalDateTime.now(), null, null);
    }

    public static PathRequest aPathRequest() {
        return new PathRequest(ADVENTURE_ID, "ROXO");
    }

    // ------------------------- Ponto de interesse -----------------------

    public static PointOfInterestResponse aPoint() {
        return new PointOfInterestResponse(POINT_ID, PATH_ID, USER_ID, "MIRANTE",
                "Mirante", "Vista", -20.43, -41.79, 3, LocalDateTime.now());
    }

    public static PointOfInterestRequest aPointRequest() {
        return new PointOfInterestRequest(PATH_ID, "MIRANTE",
                "Mirante", "Vista", -20.43, -41.79);
    }

    public static EvidenceResponse anEvidence() {
        return new EvidenceResponse("evidencia-1", POINT_ID, USER_ID,
                "https://cdn/foto.jpg", "VISTA", true, LocalDateTime.now());
    }

    public static EvidenceRequest anEvidenceRequest() {
        return new EvidenceRequest(POINT_ID, "https://cdn/foto.jpg",
                "VISTA", -20.43, -41.79);
    }

    // ------------------------------ Midia -------------------------------

    public static MediaResponse aMedia() {
        return new MediaResponse("midia-1", ADVENTURE_ID, PATH_ID, "FOTO",
                "https://cdn/midia.jpg", 0.30, 1.5, LocalDateTime.now());
    }

    public static MediaRequest aMediaRequest() {
        return new MediaRequest(ADVENTURE_ID, PATH_ID, "FOTO",
                "https://cdn/midia.jpg", -20.43, -41.79, 1.5, 0.30);
    }

    // ----------------------------- Amizade ------------------------------

    public static FriendshipResponse aFriendship() {
        return new FriendshipResponse("amizade-1", USER_ID, "usuario-2", "ACEITA",
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static FriendshipRequest aFriendshipRequest() {
        return new FriendshipRequest("rafael#2");
    }

    // ---------------------------- Localizacao ---------------------------

    public static SessionResponse aSession() {
        return new SessionResponse(SESSION_ID, PATH_ID, USER_ID, "EM_ANDAMENTO", "PRIVADO",
                false, 5.0, null, LocalDateTime.now(), null);
    }

    public static SessionRequest aSessionRequest() {
        return new SessionRequest(PATH_ID, USER_ID, true, 10.0, "AMIGOS");
    }

    public static GpsPointResponse aGpsPoint() {
        return new GpsPointResponse("gps-1", SESSION_ID, -20.43, -41.79, 800.0, 5.0, 1.2,
                1, LocalDateTime.now(), null, null);
    }

    public static GpsPointRequest aGpsPointRequest() {
        return new GpsPointRequest(SESSION_ID, -20.43, -41.79, 800.0, 5.0, 1.2);
    }
}
