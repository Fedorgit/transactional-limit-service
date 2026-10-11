package com.bank.limits.domain.model;

public record Currency(
        String code
) {

    private static final String EMPTY_CURRENCY_EXCEPTION = "Currency code must be not blank!";
    private static final String LENGTH_CURRENCY_EXCEPTION = "Currency length code must be equals 3";
    private static final String NOT_LETTER_CURRENCY_EXCEPTION = "Currency string must have only letters";

    public Currency{
            if (code == null || code.isBlank()) {
                throw new IllegalStateException(EMPTY_CURRENCY_EXCEPTION);
            }

            if (code.length()!=3){
                throw new IllegalStateException(LENGTH_CURRENCY_EXCEPTION);
            }

            if (!code.chars().allMatch(Character::isLetter)){
                throw new IllegalStateException(NOT_LETTER_CURRENCY_EXCEPTION);
            }

        }
}
