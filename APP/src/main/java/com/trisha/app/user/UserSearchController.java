package com.trisha.app.user;

import com.trisha.app.user.dto.PublicUserResponse;
import com.trisha.app.user.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User search by public userCode, for adding friends.
 */
@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
public class UserSearchController {

    private final UserSearchService userSearchService;

    @GetMapping("/codigo/{userCode}")
    public PublicUserResponse findByCode(@PathVariable String userCode) {
        return userSearchService.findByCode(userCode);
    }

    @GetMapping("/busca")
    public List<PublicUserResponse> autocomplete(@RequestParam String termo) {
        return userSearchService.autocomplete(termo);
    }

    /** Resolucao em lote de ids -> nome/codigo, para o BFF enriquecer respostas. */
    @GetMapping("/resumo")
    public List<UserSummaryResponse> getSummaries(@RequestParam("ids") List<String> ids) {
        return userSearchService.getSummaries(ids);
    }

    @GetMapping("/resumo-codigo")
    public UserSummaryResponse findSummaryByCode(@RequestParam("codigo") String userCode) {
        return userSearchService.findSummaryByCode(userCode);
    }
}
