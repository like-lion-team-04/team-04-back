package com.likelion.firstbite.firstbiteserver.coaching.domain;

public enum TimerAction {
    PAUSE, RESUME;

    public static TimerAction parse(String value) {
        if (value == null) return null;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
