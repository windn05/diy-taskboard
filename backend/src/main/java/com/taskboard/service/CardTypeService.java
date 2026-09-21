package com.taskboard.service;

import com.taskboard.domain.CardType;
import com.taskboard.dto.CardTypeDtos.*;
import com.taskboard.exception.EntityNotFoundException;
import com.taskboard.repository.CardTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 작업 유형 관리. 작업은 유형을 이름으로 저장하므로(Card.type)
 * 유형 이름을 바꾸거나 지워도 기존 작업의 값은 그대로 유지
 */
@Service
@RequiredArgsConstructor
public class CardTypeService {

    private final CardTypeRepository cardTypeRepository;

    @Transactional(readOnly = true)
    public List<CardTypeResponse> list() {
        return cardTypeRepository.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CardTypeResponse create(CardTypeRequest request) {
        CardType type = CardType.builder().name(request.name()).build();
        cardTypeRepository.save(type);
        return toResponse(type);
    }

    @Transactional
    public CardTypeResponse update(Long typeId, CardTypeRequest request) {
        CardType type = getType(typeId);
        type.setName(request.name());
        return toResponse(type);
    }

    @Transactional
    public void delete(Long typeId) {
        getType(typeId);
        cardTypeRepository.deleteById(typeId);
    }

    private CardType getType(Long typeId) {
        return cardTypeRepository.findById(typeId).orElseThrow(() -> new EntityNotFoundException("타입을 찾을 수 없습니다."));
    }

    private CardTypeResponse toResponse(CardType type) {
        return new CardTypeResponse(type.getId(), type.getName());
    }
}
