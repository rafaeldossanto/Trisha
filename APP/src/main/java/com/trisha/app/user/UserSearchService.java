package com.trisha.app.user;

import com.trisha.app.user.dto.PublicUserResponse;
import com.trisha.app.user.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserSearchService {

    private static final int AUTOCOMPLETE_LIMIT = 10;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PublicUserResponse findByCode(String userCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        return toPublic(user);
    }

    /** Id interno a partir do codigo publico; cada caller decide a mensagem de erro. */
    @Transactional(readOnly = true)
    public Optional<String> findIdByCode(String userCode) {
        return userRepository.findByUserCode(userCode).map(User::getId);
    }

    @Transactional(readOnly = true)
    public List<PublicUserResponse> autocomplete(String term) {
        return userRepository.findByUserCodePrefix(term).stream()
                .limit(AUTOCOMPLETE_LIMIT)
                .map(this::toPublic)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse findSummaryByCode(String userCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .userCode(user.getUserCode())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getSummaries(List<String> ids) {
        return userRepository.findAllById(ids).stream()
                .map(user -> UserSummaryResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .userCode(user.getUserCode())
                        .build())
                .toList();
    }

    private PublicUserResponse toPublic(User user) {
        return PublicUserResponse.builder()
                .userCode(user.getUserCode())
                .name(user.getName())
                .build();
    }

}
