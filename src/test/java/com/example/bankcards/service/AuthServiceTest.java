package com.example.bankcards.service;

import com.example.bankcards.dto.JwtResponse;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.UserStatus;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    void testRegisterUser_Success() {
        
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");

        User savedUser = new User();
        savedUser.setUsername("newuser");
        savedUser.setPassword("hashedpassword");
        savedUser.setRole(Role.USER);
        savedUser.setStatus(UserStatus.ACTIVE);
        
        when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.registerUser(registerRequest);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals(Role.USER, result.getRole());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameAlreadyExists_ThrowsException() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("existinguser");
        registerRequest.setPassword("password123");

        when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.of(new User()));
        
        assertThrows(IllegalArgumentException.class, () -> authService.registerUser(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAuthenticateUser_Success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("testuser");
        mockUser.setPassword("hashedpassword");
        mockUser.setRole(Role.USER);
        mockUser.setStatus(UserStatus.ACTIVE);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(mockUser));
        when(tokenProvider.generateAccessToken(mockUser)).thenReturn("test_access_token");
        when(tokenProvider.generateRefreshToken(mockUser)).thenReturn("test_refresh_token");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        JwtResponse result = authService.authenticateUser(loginRequest);

        assertNotNull(result);
        assertEquals("test_access_token", result.getAccessToken());
        assertEquals("test_refresh_token", result.getRefreshToken());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void testAuthenticateUser_UserNotFound_ThrowsException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new UsernameNotFoundException("User not found with username: " + loginRequest.getUsername()));

        assertThrows(UsernameNotFoundException.class, () -> authService.authenticateUser(loginRequest));

        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void testAuthenticateUser_UserIsBlocked_ThrowsException() {
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("blockeduser");
        loginRequest.setPassword("password123");

        User blockedUser = new User();
        blockedUser.setUsername("blockeduser");
        blockedUser.setStatus(UserStatus.BLOCKED);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(blockedUser));

        assertThrows(ResponseStatusException.class, () -> authService.authenticateUser(loginRequest));
        verify(userRepository, never()).save(any());
    }
}