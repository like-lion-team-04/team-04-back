package com.likelion.firstbite.firstbiteserver.feedback.dto;

import java.math.BigDecimal;

public record PersonalizationResponse(boolean enabled, long feedbackCount, BigDecimal coefficient,
                                      String direction, String message) {}
