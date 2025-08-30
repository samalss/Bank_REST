package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.UserStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.UserMapper;
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

public class AdminServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminService adminService;

    private User adminUser;
    private User regularUser;
    private Card activeCard;
    private Card blockedCard;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ADMIN);

        regularUser = new User();
        regularUser.setId(UUID.randomUUID());
        regularUser.setUsername("testuser");
        regularUser.setRole(Role.USER);
        regularUser.setStatus(UserStatus.ACTIVE);

        activeCard = new Card();
        activeCard.setId(UUID.randomUUID());
        activeCard.setOwner(regularUser);
        activeCard.setBalance(new BigDecimal("100.00"));
        activeCard.setStatus(CardStatus.ACTIVE);

        blockedCard = new Card();
        blockedCard.setId(UUID.randomUUID());
        blockedCard.setOwner(regularUser);
        blockedCard.setBalance(new BigDecimal("50.00"));
        blockedCard.setStatus(CardStatus.BLOCKED);

        
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
    }

    @Test
    void testGetAllCards_Success() {
        
        Pageable pageable = PageRequest.of(0, 10);
        List<Card> cardList = List.of(activeCard, blockedCard);
        Page<Card> cardPage = new PageImpl<>(cardList, pageable, cardList.size());

        when(cardRepository.findAll(pageable)).thenReturn(cardPage);
        when(cardMapper.toCardResponse(any(Card.class))).thenReturn(new CardResponse());

        
        Page<CardResponse> result = adminService.getAllCards(pageable);

        
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(cardRepository, times(1)).findAll(pageable);
    }

    @Test
    void testGetAllUsers_Success() {
        
        Pageable pageable = PageRequest.of(0, 10);
        List<User> userList = List.of(adminUser, regularUser);
        Page<User> userPage = new PageImpl<>(userList, pageable, userList.size());

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse());

        
        Page<UserResponse> result = adminService.getAllUsers(pageable);

        
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    void testBlockUser_Success() {
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse());

        UserResponse result = adminService.blockUser(regularUser.getId());

        assertNotNull(result);
        assertEquals(UserStatus.BLOCKED.name(), regularUser.getStatus().name());
        verify(userRepository, times(1)).save(regularUser);
    }

    @Test
    void testBlockUser_UserNotFound_ThrowsException() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> adminService.blockUser(UUID.randomUUID()));
    }

    @Test
    void testBlockUser_UserAlreadyBlocked_ThrowsException() {
        regularUser.setStatus(UserStatus.BLOCKED);
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        assertThrows(ResponseStatusException.class, () -> adminService.blockUser(regularUser.getId()));
    }

    @Test
    void testBlockUser_AdminBlocksOwnAccount_ThrowsException() {
        assertThrows(ResponseStatusException.class, () -> adminService.blockUser(adminUser.getId()));
    }

    @Test
    void testActivateUser_Success() {
        
        regularUser.setStatus(UserStatus.BLOCKED);
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(new UserResponse());

        UserResponse result = adminService.activateUser(regularUser.getId());

        assertNotNull(result);
        assertEquals(UserStatus.ACTIVE.name(), regularUser.getStatus().name());
        verify(userRepository, times(1)).save(regularUser);
    }

    @Test
    void testActivateUser_AlreadyActive_ThrowsException() {
        
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        
        assertThrows(ResponseStatusException.class, () -> adminService.activateUser(regularUser.getId()));
    }

    @Test
    void testDeleteUser_Success() {
        
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        
        adminService.deleteUser(regularUser.getId());

        
        assertEquals(UserStatus.DELETED.name(), regularUser.getStatus().name());
        verify(userRepository, times(1)).save(regularUser);
    }

    @Test
    void testDeleteUser_AdminDeletesOwnAccount_ThrowsException() {
        
        assertThrows(ResponseStatusException.class, () -> adminService.deleteUser(adminUser.getId()));
    }

    @Test
    void testBlockCard_Success() {
        
        when(cardRepository.findById(activeCard.getId())).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(any(Card.class))).thenReturn(activeCard);
        when(cardMapper.toCardResponse(any(Card.class))).thenReturn(new CardResponse());

        
        CardResponse result = adminService.blockCard(activeCard.getId());

        
        assertNotNull(result);
        assertEquals(CardStatus.BLOCKED.name(), activeCard.getStatus().name());
        verify(cardRepository, times(1)).save(activeCard);
    }

    @Test
    void testBlockCard_AlreadyBlocked_ThrowsException() {
        
        when(cardRepository.findById(blockedCard.getId())).thenReturn(Optional.of(blockedCard));

        
        assertThrows(ResponseStatusException.class, () -> adminService.blockCard(blockedCard.getId()));
    }

    @Test
    void testActivateCard_Success() {
        
        when(cardRepository.findById(blockedCard.getId())).thenReturn(Optional.of(blockedCard));
        when(cardRepository.save(any(Card.class))).thenReturn(blockedCard);
        when(cardMapper.toCardResponse(any(Card.class))).thenReturn(new CardResponse());

        
        CardResponse result = adminService.activateCard(blockedCard.getId());

        
        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE.name(), blockedCard.getStatus().name());
        verify(cardRepository, times(1)).save(blockedCard);
    }

    @Test
    void testActivateCard_AlreadyActive_ThrowsException() {
        
        when(cardRepository.findById(activeCard.getId())).thenReturn(Optional.of(activeCard));

        
        assertThrows(ResponseStatusException.class, () -> adminService.activateCard(activeCard.getId()));
    }

    @Test
    void testDeleteCard_Success() {
        
        activeCard.setBalance(BigDecimal.ZERO);
        when(cardRepository.findById(activeCard.getId())).thenReturn(Optional.of(activeCard));

        
        adminService.deleteCard(activeCard.getId());

        
        verify(cardRepository, times(1)).deleteById(activeCard.getId());
    }

    @Test
    void testDeleteCard_NonZeroBalance_ThrowsException() {
        
        when(cardRepository.findById(activeCard.getId())).thenReturn(Optional.of(activeCard));

        
        assertThrows(ResponseStatusException.class, () -> adminService.deleteCard(activeCard.getId()));
    }
}