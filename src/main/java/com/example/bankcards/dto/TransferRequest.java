package com.example.bankcards.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequest {
    private UUID sourceCardId;
    private UUID destinationCardId;
    private BigDecimal amount;
}