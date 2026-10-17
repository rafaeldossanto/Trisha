package com.trisha.app.stub;

import com.trisha.app.path.Color;
import com.trisha.app.path.Path;
import com.trisha.app.path.dto.PathRequest;

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
