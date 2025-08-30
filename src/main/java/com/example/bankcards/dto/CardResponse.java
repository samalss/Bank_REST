package com.example.bankcards.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CardResponse {
    private UUID id;
    private String cardNumberMasked;
    private String expiryDate;
    private String status;
    private BigDecimal balance;
    private UserResponse owner;
}