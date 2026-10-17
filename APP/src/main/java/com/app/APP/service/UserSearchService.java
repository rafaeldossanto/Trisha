package com.app.APP.service;

import com.app.APP.entity.User;
import com.app.APP.model.dto.response.PublicUserResponse;
import com.app.APP.model.dto.response.UserSummaryResponse;
import com.app.APP.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


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
