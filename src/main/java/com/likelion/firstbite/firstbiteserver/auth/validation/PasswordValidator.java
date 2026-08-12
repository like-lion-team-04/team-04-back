package com.likelion.firstbite.firstbiteserver.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank() || value.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        int categories = 0;
        if (value.matches(".*[A-Za-z].*")) categories++;
        if (value.matches(".*[0-9].*")) categories++;
        if (value.matches(".*[^A-Za-z0-9\\s].*")) categories++;
        return categories >= 2;
    }
}
