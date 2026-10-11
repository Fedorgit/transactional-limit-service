package com.bank.limits.domain.model;

public record AccountNumber(String number) {

    public AccountNumber{

        if (number==null || number.isBlank()){
            throw new IllegalArgumentException("Account number can't be null!");
        }

        if (number.length()!= 10){
            throw new IllegalArgumentException("Account number must have length 10");
        }

        if (!number.chars().allMatch(Character::isDigit)){
            throw new IllegalArgumentException("Account number must have only numbers!");
        }
    }

}
