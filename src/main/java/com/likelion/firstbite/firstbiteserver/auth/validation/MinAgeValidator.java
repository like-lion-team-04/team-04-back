package com.likelion.firstbite.firstbiteserver.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Clock;
import java.time.LocalDate;

public class MinAgeValidator implements ConstraintValidator<MinAge, LocalDate> {
    private int minimumAge;

    @Override
    public void initialize(MinAge annotation) {
        minimumAge = annotation.value();
    }

    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        return birthDate == null || !birthDate.plusYears(minimumAge).isAfter(LocalDate.now(Clock.systemDefaultZone()));
    }
}
