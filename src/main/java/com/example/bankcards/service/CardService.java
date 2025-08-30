package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.DataGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireCards() {
        LocalDate today = LocalDate.now();
        List<Card> expiredCards = cardRepository.findByExpiryDateBeforeAndStatusIs(today, CardStatus.ACTIVE);

        if (!expiredCards.isEmpty()) {
            expiredCards.forEach(card -> {
                card.setStatus(CardStatus.EXPIRED);
                System.out.println("Card " + card.getId() + " has expired and its status has been updated.");
            });
            cardRepository.saveAll(expiredCards);
        }
    }

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        User owner = getCurrentUser();

        Card newCard = new Card();
        newCard.setCardNumber(DataGenerator.generateCardNumber());
        newCard.setExpiryDate(DataGenerator.generateExpiryDate());
        newCard.setOwner(owner);
        newCard.setBalance(new BigDecimal("200.00"));
        newCard.setStatus(CardStatus.ACTIVE);

        Card savedCard = cardRepository.save(newCard);
        return cardMapper.toCardResponse(savedCard);
    }

    @Transactional
    public CardResponse blockCard(UUID cardId) {
        User currentUser = getCurrentUser();
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card is already inactive or expired.");
        }

        card.setStatus(CardStatus.BLOCKED);
        Card savedCard = cardRepository.save(card);
        return cardMapper.toCardResponse(savedCard);
    }

    @Transactional
    public void deleteMyCard(UUID cardId) {
        User currentUser = getCurrentUser();
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found."));

        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You can only delete your own cards.");
        }

        if (card.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete a card with a non-zero balance.");
        }

        card.setStatus(CardStatus.DELETED);
        cardRepository.save(card);
    }

    @Transactional
    public void transferFunds(TransferRequest transferRequest) {
        if (transferRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount must be positive.");
        }
        
        Card sourceCard = cardRepository.findById(transferRequest.getSourceCardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source card not found."));
        Card destinationCard = cardRepository.findById(transferRequest.getDestinationCardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination card not found."));

        if (sourceCard.getId().equals(destinationCard.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer funds to the same card.");
        }
        
        User currentUser = getCurrentUser();
        if (!sourceCard.getOwner().getId().equals(currentUser.getId()) ||
                !destinationCard.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Cards do not belong to the user.");
        }

        if (sourceCard.getStatus() != CardStatus.ACTIVE || destinationCard.getStatus() != CardStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer funds. One of the cards is not active.");
        }

        if (sourceCard.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds on the source card.");
        }

        sourceCard.setBalance(sourceCard.getBalance().subtract(transferRequest.getAmount()));
        destinationCard.setBalance(destinationCard.getBalance().add(transferRequest.getAmount()));

        cardRepository.save(sourceCard);
        cardRepository.save(destinationCard);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public Page<CardResponse> getCardsForCurrentUser(Pageable pageable) {
        User currentUser = getCurrentUser();
        
        Page<Card> cardsPage = cardRepository.findByOwnerIdAndStatusNot(currentUser.getId(), CardStatus.DELETED, pageable);
        return cardsPage.map(cardMapper::toCardResponse);
    }

    public CardResponse getCardByIdForCurrentUser(UUID cardId) {
        User currentUser = getCurrentUser();
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        if (card.getStatus() == CardStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found");
        }

        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return cardMapper.toCardResponse(card);
    }
}