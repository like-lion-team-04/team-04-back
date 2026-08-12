package com.likelion.firstbite.firstbiteserver.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinAgeValidator.class)
public @interface MinAge {
    String message() default "만 {value}세 이상만 가입할 수 있습니다.";
    int value();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
