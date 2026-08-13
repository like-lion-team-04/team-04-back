package com.likelion.firstbite.firstbiteserver.auth.octomo;

public interface OctomoClient {
    boolean messageExists(String phoneNumber, String messageText, int withinMinutes);
}
