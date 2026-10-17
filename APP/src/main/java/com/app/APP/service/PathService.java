package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.Path;
import com.app.APP.mapper.PathMapper;
import com.app.APP.model.dto.request.PathRequest;
import com.app.APP.model.dto.response.PathDiscoveryResponse;
import com.app.APP.model.dto.response.PathResponse;
import com.app.APP.repository.AdventureRepository;
import com.app.APP.repository.PathRepository;
import com.app.APP.util.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.app.APP.mapper.PathMapper.toResponse;

/**
 * questionamento:
 * se um usuario entrar dentro de uma aventura de outro, e fizer o caminho dentro dessa aventura, se ele sair dessa aventura o registro do caminho ser excluido?
 * se um usuario para e querer finalizar o caminho mas ele esta dentro de uma aventura que nao é dele, ele nao consegue finalizar por conta disso,
 * sendo que em certas situações pode ocorrer de um terminar seu caminho e outros continuarem, entao nao pode se limitar apenas ao woner finalizar o caminho,
 * caminhos individuas mas registrados em uma mesma aventura, com cada um podendo finalizar quando quiser, e caso ele queria retirar essa aventura o caminho deve continuar salvo.
 * opção para esse problema: assim que entrar em uma aventura, criamos uma aventura a parte/individual em que o usuario tera poder sobre ela, e caso ele quera se disvincular da aventura que fez em grupo ele consegue mas mantendo os registros
 * criar uma aventura a parte assim que entrar em um com amigos acredito que seja desnecessario. mais facil: ao querer sair da aventura em grupo, perguntar se deseja manter os dados com uma aventura pessoal apenas com seus dados retirando o dos outros,
 * mas podendno tbm escolher de quem ele quer que mantenha
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class PathService {

    private final PathRepository pathRepository;
    private final AdventureRepository adventureRepository;
    private final AdventureAccessService accessService;

    /**
     * Inicia um caminho. Transacional e com lock de escrita na aventura: o numero
     * do caminho vem de {@code countByAdventureId()+1} e caminhos sao individuais
     * (varios participantes de uma aventura em grupo podem iniciar ao mesmo
     * tempo), entao a contagem precisa ser serializada por aventura para nao
     * gerar numero duplicado. O lock e por aventura — aventuras diferentes nao
     * se bloqueiam.
     */
    @Transactional
    public PathResponse start(String userId, PathRequest request) {
        log.info("Starting path for user: {} in adventure: {}", userId, request.adventureId());

        Adventure adventure = adventureRepository.findByIdForUpdate(request.adventureId())
                .orElseThrow(() -> new IllegalArgumentException("Aventura nao encontrada"));
        accessService.validateContribute(userId, adventure);

        int number = pathRepository.countByAdventureId(request.adventureId()) + 1;

        return toResponse(pathRepository.save(PathMapper.toEntity(request, adventure, number, userId)));
    }

    public PathResponse finish(String userId, String id, Double totalDistanceKm) {
        log.info("Finishing path: {}", id);
        Path path = findById(id);
        OwnershipValidator.requireOwner(userId, path.getUserId(), "Voce nao e o dono deste caminho");

        path.setFinishedAt(LocalDateTime.now());
        path.setTotalDistanceKm(totalDistanceKm);

        return toResponse(pathRepository.save(path));
    }

    @Transactional(readOnly = true)
    public Page<PathResponse> getByAdventure(String observerId, String adventureId, Pageable pageable) {
        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new IllegalArgumentException("Aventura nao encontrada"));
        accessService.validateView(observerId, adventure);
        return pathRepository.findByAdventureId(adventureId, pageable)
                .map(PathMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PathResponse> getByUser(String observerId, String userId, Pageable pageable) {
        Page<Path> page = observerId.equals(userId)
                ? pathRepository.findByUserId(userId, pageable)
                : pathRepository.findVisibleByUser(userId, observerId, pageable);
        return page.map(PathMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean canView(String observerId, String pathId) {
        return accessService.canViewPath(observerId, pathId);
    }

    /**
     * Filtra, entre os caminhos que o mapa encontrou na regiao visivel (ids
     * vindos do servico de Localizacao via BFF), os que o observador pode ver:
     * PUBLICA para todos, SO_GRUPO so para participantes; os proprios ficam de
     * fora — o app ja os desenha.
     */
    @Transactional(readOnly = true)
    public List<PathDiscoveryResponse> discover(String observerId, List<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return pathRepository.findDiscoverableByIds(ids, observerId)
                .stream().map(PathMapper::toDiscoveryResponse).toList();
    }

    private Path findById(String id) {
        return pathRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Caminho nao encontrado"));
    }
}
