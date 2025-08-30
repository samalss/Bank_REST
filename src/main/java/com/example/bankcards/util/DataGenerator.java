package com.example.bankcards.util;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Random;

public class DataGenerator {
    public static String generateCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            cardNumber.append(random.nextInt(10));
        }
        return cardNumber.toString();
    }

    public static LocalDate generateExpiryDate() {
        return LocalDate.now().plusYears(3).with(TemporalAdjusters.lastDayOfMonth());
    }
}