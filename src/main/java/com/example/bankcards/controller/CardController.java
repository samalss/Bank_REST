package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.service.CardService;
import com.example.bankcards.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponse> createCard(@RequestBody(required = false) CreateCardRequest request) {
        CardResponse createdCard = cardService.createCard(request != null ? request : new CreateCardRequest());
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardResponse>> getMyCards(Pageable pageable) {
        Page<CardResponse> cards = cardService.getCardsForCurrentUser(pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponse> getCardById(@PathVariable UUID id) {
        CardResponse card = cardService.getCardByIdForCurrentUser(id);
        return ResponseEntity.ok(card);
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponse> blockCard(@PathVariable UUID id) {
        CardResponse updatedCard = cardService.blockCard(id);
        return ResponseEntity.ok(updatedCard);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteMyCard(@PathVariable UUID id) {
        cardService.deleteMyCard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> transferFunds(@RequestBody TransferRequest transferRequest) {
        cardService.transferFunds(transferRequest);
        return ResponseEntity.ok().build();
    }
}