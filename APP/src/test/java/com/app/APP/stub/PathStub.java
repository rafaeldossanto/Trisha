package com.app.APP.stub;

import com.app.APP.entity.Path;
import com.app.APP.model.dto.request.PathRequest;
import com.app.APP.model.enums.Color;

import java.time.LocalDateTime;

/**
 * Test helper for Path.
 */
public final class PathStub {

    public static final String ID = "caminho-1";
    public static final String USER_ID = "usuario-1";
    public static final Integer NUMBER = 1;

    private PathStub() {
    }

    public static Path.PathBuilder aPath() {
        return Path.builder()
                .id(ID)
                .adventure(AdventureStub.anAdventure().build())
                .userId(USER_ID)
                .color(Color.ROXO)
                .number(NUMBER)
                .startedAt(LocalDateTime.now());
    }

    public static PathRequest aRequest() {
        return new PathRequest(AdventureStub.ID, Color.ROXO);
    }
}
