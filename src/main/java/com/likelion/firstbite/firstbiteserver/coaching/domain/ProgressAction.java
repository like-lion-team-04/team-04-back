package com.likelion.firstbite.firstbiteserver.coaching.domain;

public enum ProgressAction {
    AUTO_ADVANCE, SKIP, NEXT, COMPLETE, USER_END;

    public static ProgressAction parse(String value) {
        if (value == null) return null;
        try {
            ProgressAction action = valueOf(value);
            return action == AUTO_ADVANCE || action == SKIP || action == NEXT ? action : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
