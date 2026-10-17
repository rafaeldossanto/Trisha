package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.client.LocationClient;
import com.trisha.bff.model.dto.request.PathRequest;
import com.trisha.bff.model.dto.response.PathDiscoveryResponse;
import com.trisha.bff.model.dto.response.PathResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.model.dto.response.SessionResponse;
import com.trisha.bff.model.dto.response.TrailDiscoveryResponse;
import com.trisha.bff.model.dto.response.TrailPointsResponse;
import com.trisha.bff.model.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class PathBffService {

    private static final String STATUS_IN_PROGRESS = "EM_ANDAMENTO";

    private final AppClient appClient;
    private final LocationClient locationClient;

    public PathResponse start(PathRequest request) {
        log.info("BFF: iniciando caminho na aventura {}", request.adventureId());
        return appClient.startPath(request);
    }

    /**
     * Orquestra a finalizacao da trilha: busca a sessao de rastreamento do caminho
     * no servico de Localizacao, garante que ela esta finalizada (a distancia real
     * so e calculada na finalizacao da sessao) e usa ESSA distancia — a do GPS, nao
     * uma informada pelo cliente — para finalizar o caminho no APP. Mantem APP e
     * Localizacao desacoplados: o BFF e o maestro.
     */
    @CacheEvict(cacheNames = "caminhos-aventura", allEntries = true)
    public PathResponse finish(String id) {
        log.info("BFF: finalizando caminho {}", id);

        SessionResponse session = locationClient.getSessionByPath(id);
        if (STATUS_IN_PROGRESS.equals(session.status())) {
            session = locationClient.finishSession(session.id());
        }

        return appClient.finishPath(id, session.totalDistanceKm());
    }

    // Chave inclui o observador: o APP filtra por visibilidade, entao a mesma
    // consulta pode devolver resultados diferentes para usuarios diferentes.
    @Cacheable(cacheNames = "caminhos-aventura",
            key = "#observerId + '-' + #adventureId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<PathResponse> getByAdventure(String observerId, String adventureId, Pageable pageable) {
        return appClient.getPathsByAdventure(adventureId, pageable);
    }

    @Cacheable(cacheNames = "caminhos-usuario",
            key = "#observerId + '-' + #userId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<PathResponse> getByUser(String observerId, String userId, Pageable pageable) {
        return appClient.getPathsByUser(userId, pageable);
    }

    /**
     * Mapa colaborativo: trilhas na area visivel do mapa que o usuario pode ver.
     * Orquestra as duas pontas — a Localizacao devolve a geometria por bbox (ja
     * decimada) e o APP diz quais desses caminhos sao visiveis (visibilidade da
     * aventura; os proprios ficam de fora). Sem cache: a bbox varia a cada gesto
     * de pan/zoom e o resultado depende do usuario autenticado.
     */
    public List<TrailDiscoveryResponse> discover(double minLat, double minLng, double maxLat, double maxLng,
                                                 int maxPointsPerPath) {
        List<TrailPointsResponse> trails =
                locationClient.getPointsInBbox(minLat, minLng, maxLat, maxLng, maxPointsPerPath);
        if (trails.isEmpty()) {
            return List.of();
        }

        Map<String, TrailPointsResponse> byPathId = trails.stream()
                .collect(Collectors.toMap(TrailPointsResponse::pathId, Function.identity()));

        List<PathDiscoveryResponse> visible = appClient.discoverPaths(List.copyOf(byPathId.keySet()));
        if (visible.isEmpty()) {
            return List.of();
        }

        // Nome/codigo dos donos, para o app rotular as trilhas no mapa.
        Map<String, UserSummaryResponse> users = appClient
                .getUserSummaries(visible.stream().map(PathDiscoveryResponse::userId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(UserSummaryResponse::id, Function.identity()));

        return visible.stream()
                .map(path -> {
                    UserSummaryResponse user = users.get(path.userId());
                    return new TrailDiscoveryResponse(
                            path.id(),
                            path.adventureId(),
                            path.userId(),
                            user == null ? null : user.name(),
                            user == null ? null : user.userCode(),
                            path.destination(),
                            path.color(),
                            byPathId.get(path.id()).points());
                })
                .toList();
    }
}
