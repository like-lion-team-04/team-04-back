package com.likelion.firstbite.firstbiteserver.coaching.domain;

public enum CompletionReason {
    COMPLETED, USER_ENDED;

    public static CompletionReason parse(String value) {
        if (value == null) return null;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
