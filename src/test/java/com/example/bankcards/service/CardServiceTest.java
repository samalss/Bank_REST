package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    private User currentUser;
    private Card myCard;
    private Card otherCard;
    private User otherUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setUsername("testuser");
        currentUser.setRole(Role.USER);

        myCard = new Card();
        myCard.setId(UUID.randomUUID());
        myCard.setOwner(currentUser);
        myCard.setBalance(new BigDecimal("500.00"));
        myCard.setStatus(CardStatus.ACTIVE);

        otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setUsername("otheruser");
        otherCard = new Card();
        otherCard.setId(UUID.randomUUID());
        otherCard.setOwner(otherUser);
        otherCard.setBalance(new BigDecimal("100.00"));
        otherCard.setStatus(CardStatus.ACTIVE);

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void testCreateCard_Success() {
        
        CreateCardRequest request = new CreateCardRequest();
        Card savedCard = new Card();
        savedCard.setId(UUID.randomUUID());

        when(cardRepository.save(any(Card.class))).thenReturn(savedCard);
        when(cardRepository.findById(savedCard.getId())).thenReturn(Optional.of(savedCard));
        when(cardMapper.toCardResponse(any(Card.class))).thenReturn(new CardResponse());

        CardResponse result = cardService.createCard(request);
        
        assertNotNull(result);
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    void testTransferFunds_Success() {
        
        Card destinationCard = new Card();
        destinationCard.setId(UUID.randomUUID());
        destinationCard.setOwner(currentUser);
        destinationCard.setBalance(new BigDecimal("100.00"));
        destinationCard.setStatus(CardStatus.ACTIVE);

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSourceCardId(myCard.getId());
        transferRequest.setDestinationCardId(destinationCard.getId());
        transferRequest.setAmount(new BigDecimal("100.00"));

        when(cardRepository.findById(myCard.getId())).thenReturn(Optional.of(myCard));
        when(cardRepository.findById(destinationCard.getId())).thenReturn(Optional.of(destinationCard));

        cardService.transferFunds(transferRequest);

        assertEquals(new BigDecimal("400.00"), myCard.getBalance());
        assertEquals(new BigDecimal("200.00"), destinationCard.getBalance());
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void testTransferFunds_InsufficientFunds_ThrowsException() {
        
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSourceCardId(myCard.getId());
        transferRequest.setDestinationCardId(otherCard.getId());
        transferRequest.setAmount(new BigDecimal("600.00"));

        when(cardRepository.findById(myCard.getId())).thenReturn(Optional.of(myCard));
        when(cardRepository.findById(otherCard.getId())).thenReturn(Optional.of(otherCard));

        assertThrows(ResponseStatusException.class, () -> cardService.transferFunds(transferRequest));
    }

    @Test
    void testBlockCard_Success() {
        
        when(cardRepository.findById(myCard.getId())).thenReturn(Optional.of(myCard));
        when(cardMapper.toCardResponse(any(Card.class))).thenReturn(new CardResponse());

        CardResponse result = cardService.blockCard(myCard.getId());

        assertEquals(CardStatus.BLOCKED, myCard.getStatus());
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    void testBlockCard_AlreadyBlocked_ThrowsException() {
        myCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(myCard.getId())).thenReturn(Optional.of(myCard));

        assertThrows(ResponseStatusException.class, () -> cardService.blockCard(myCard.getId()));
    }

    @Test
    void testDeleteMyCard_Success() {
        myCard.setBalance(BigDecimal.ZERO);
        when(cardRepository.findById(myCard.getId())).thenReturn(Optional.of(myCard));

        cardService.deleteMyCard(myCard.getId());

        assertEquals(CardStatus.DELETED, myCard.getStatus());
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    void testGetCardsForCurrentUser_ExcludesDeletedCards() {
        Card deletedCard = new Card();
        deletedCard.setOwner(currentUser);
        deletedCard.setStatus(CardStatus.DELETED);

        List<Card> activeCards = List.of(myCard);
        Page<Card> cardsPage = new PageImpl<>(activeCards);

        when(cardRepository.findByOwnerIdAndStatusNot(currentUser.getId(), CardStatus.DELETED, PageRequest.of(0, 10)))
                .thenReturn(cardsPage);
        when(cardMapper.toCardResponse(myCard)).thenReturn(new CardResponse());

        Pageable pageable = PageRequest.of(0, 10);
        Page<CardResponse> result = cardService.getCardsForCurrentUser(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(myCard.getId(), activeCards.get(0).getId());
        verify(cardRepository, times(1)).findByOwnerIdAndStatusNot(any(UUID.class), any(CardStatus.class), any(Pageable.class));
    }

    @Test
    void testTransferFunds_ToSameCard_ThrowsException() {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSourceCardId(myCard.getId());
        transferRequest.setDestinationCardId(myCard.getId());
        transferRequest.setAmount(new BigDecimal("10.00"));

        when(cardRepository.findById(myCard.getId())).thenReturn(Optional.of(myCard));

        assertThrows(ResponseStatusException.class, () -> cardService.transferFunds(transferRequest));
    }

    @Test
    void testTransferFunds_DifferentOwners_ThrowsException() {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSourceCardId(myCard.getId());
        transferRequest.setDestinationCardId(otherCard.getId());
        transferRequest.setAmount(new BigDecimal("10.00"));

        when(cardRepository.findById(myCard.getId())).thenReturn(Optional.of(myCard));
        when(cardRepository.findById(otherCard.getId())).thenReturn(Optional.of(otherCard));

        assertThrows(ResponseStatusException.class, () -> cardService.transferFunds(transferRequest));
    }

    @Test
    void testTransferFunds_ToInactiveCard_ThrowsException() {
        Card inactiveCard = new Card();
        inactiveCard.setId(UUID.randomUUID());
        inactiveCard.setOwner(currentUser);
        inactiveCard.setBalance(new BigDecimal("100.00"));
        inactiveCard.setStatus(CardStatus.BLOCKED);

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSourceCardId(myCard.getId());
        transferRequest.setDestinationCardId(inactiveCard.getId());
        transferRequest.setAmount(new BigDecimal("10.00"));

        when(cardRepository.findById(myCard.getId())).thenReturn(Optional.of(myCard));
        when(cardRepository.findById(inactiveCard.getId())).thenReturn(Optional.of(inactiveCard));

        assertThrows(ResponseStatusException.class, () -> cardService.transferFunds(transferRequest));
    }
}