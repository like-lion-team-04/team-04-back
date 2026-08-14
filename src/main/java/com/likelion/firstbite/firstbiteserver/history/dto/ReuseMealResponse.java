package com.likelion.firstbite.firstbiteserver.history.dto;

import java.util.UUID;

public record ReuseMealResponse(UUID newMealId, String status, String source,
                                int copiedItemCount, boolean recalculationRequired) {}
