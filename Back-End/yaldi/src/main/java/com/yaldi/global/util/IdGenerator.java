package com.yaldi.global.util;

import com.github.f4b6a3.ulid.UlidCreator;

public class IdGenerator {

    private IdGenerator() {}

    public static String generateJobId() {
        return UlidCreator.getMonotonicUlid().toString();
    }
}
