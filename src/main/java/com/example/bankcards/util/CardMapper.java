package com.example.bankcards.util;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

    private final UserMapper userMapper;

    public CardMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CardResponse toCardResponse(Card card) {
        CardResponse response = new CardResponse();
        response.setId(card.getId());

        String decryptedCardNumber = EncryptionUtil.decrypt(card.getCardNumber());
        response.setCardNumberMasked(CardMasker.maskCardNumber(decryptedCardNumber));

        response.setExpiryDate(card.getExpiryDate().toString());
        response.setStatus(card.getStatus().name());
        response.setBalance(card.getBalance());

        if (card.getOwner() != null) {
            response.setOwner(userMapper.toUserResponse(card.getOwner()));
        }

        return response;
    }
}